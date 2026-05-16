package wily.legacy.client.screen.globalleaderboards;

import wily.legacy.Legacy4J;
import wily.legacy.client.screen.globalleaderboards.api.GlobalLeaderboardApiClient;
import wily.legacy.client.screen.globalleaderboards.board.GlobalLeaderboardBoardRegistry;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardCache;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardSnapshot;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardEntry;
import wily.legacy.client.screen.globalleaderboards.screen.GlobalLeaderboardsScreen;
import wily.legacy.client.screen.globalleaderboards.storage.GlobalLeaderboardCacheStore;
import wily.legacy.client.screen.globalleaderboards.storage.GlobalStatsAggregator;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.stats.Stat;
import wily.factoryapi.FactoryAPIClient;

public final class GlobalLeaderboardsFeature {
   private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "legacy-global-leaderboards");
      thread.setDaemon(true);
      return thread;
   });
   private static final Set<String> IN_FLIGHT_REQUESTS = ConcurrentHashMap.newKeySet();
   private static final Map<String, Long> REQUEST_TIMES = new ConcurrentHashMap<>();
   private static final AtomicInteger CACHE_VERSION = new AtomicInteger();
   private static final GlobalLeaderboardsConfig.Values CONFIG = GlobalLeaderboardsConfig.get();
   private static final GlobalLeaderboardApiClient API_CLIENT = new GlobalLeaderboardApiClient(CONFIG);
   private static volatile boolean started;
   private static volatile boolean launchTaskScheduled;
   private static volatile String playerUuid = "";
   private static volatile String playerName = "";
   private static volatile List<String> boardIds = GlobalLeaderboardBoardRegistry.trackedBoardIds();
   private static volatile Object2IntOpenHashMap<Stat<?>> aggregateStats = new Object2IntOpenHashMap<>();
   private static volatile Map<String, GlobalLeaderboardBoardSnapshot> boardSnapshots = Map.of();
   private static volatile Map<String, GlobalLeaderboardBoardCache> boardCaches = Map.of();
   private static volatile String lastSyncHash = "";
   private static volatile long lastSyncAt;

   private GlobalLeaderboardsFeature() {
   }

   public static synchronized void init() {
      if (started) {
         return;
      }

      started = true;
      loadCache();
      FactoryAPIClient.setup(GlobalLeaderboardsFeature::onClientStarted);
   }

   public static void onClientStarted(Minecraft minecraft) {
      ensureStarted(minecraft);
   }

   public static Screen createScreen(Screen parent, Supplier<Screen> fallback) {
      try {
         return new GlobalLeaderboardsScreen(parent);
      } catch (RuntimeException err) {
         Legacy4J.LOGGER.warn("Falling back to legacy leaderboards", err);
         return fallback.get();
      }
   }

   public static synchronized void ensureStarted(Minecraft minecraft) {
      if (launchTaskScheduled) {
         return;
      }

      launchTaskScheduled = true;
      String cachedPlayerUuid = playerUuid;
      playerUuid = minecraft.getUser().getProfileId() == null ? "" : minecraft.getUser().getProfileId().toString();
      playerName = minecraft.getUser().getName();
      if (!playerUuid.equals(cachedPlayerUuid)) {
         lastSyncHash = "";
         lastSyncAt = 0L;
      }
      CompletableFuture.runAsync(() -> startupSync(minecraft), EXECUTOR);
   }

   public static int cacheVersion() {
      return CACHE_VERSION.get();
   }

   public static List<String> boardIds() {
      return boardIds;
   }

   public static String boardId(int index) {
      return index >= 0 && index < boardIds.size() ? boardIds.get(index) : GlobalLeaderboardBoardRegistry.boardId(index);
   }

   public static String playerUuid() {
      return playerUuid;
   }

   public static String playerName() {
      return playerName;
   }

   public static Object2IntOpenHashMap<Stat<?>> aggregateStats() {
      return new Object2IntOpenHashMap<>(aggregateStats);
   }

   public static Map<String, GlobalLeaderboardBoardSnapshot> boardSnapshots() {
      return boardSnapshots;
   }

   public static GlobalLeaderboardBoardSnapshot boardSnapshot(String boardId) {
      return boardSnapshots.get(boardId);
   }

   public static GlobalLeaderboardBoardCache boardCache(String boardId) {
      return boardCaches.get(boardId);
   }

   public static List<GlobalLeaderboardEntry> entries(String boardId, GlobalLeaderboardsScreen.ViewMode viewMode) {
      GlobalLeaderboardBoardCache cache = boardCaches.get(boardId);
      if (cache == null) {
         return List.of();
      }

      return viewMode == GlobalLeaderboardsScreen.ViewMode.TOP ? cache.topEntries() : cache.aroundEntries();
   }

   public static boolean hasEndpoint() {
      return CONFIG.hasEndpoint();
   }

   public static void requestBoard(String boardId, GlobalLeaderboardsScreen.ViewMode viewMode) {
      if (!shouldRequestBoard(boardId, viewMode)) {
         return;
      }

      String requestKey = boardId + "|" + viewMode.name();
      if (!IN_FLIGHT_REQUESTS.add(requestKey)) {
         return;
      }
      REQUEST_TIMES.put(requestKey, System.currentTimeMillis());

      CompletableFuture.runAsync(() -> {
         try {
            List<GlobalLeaderboardEntry> entries = API_CLIENT.fetchBoard(boardId, playerUuid, viewMode, CONFIG.aroundWindow(), CONFIG.topLimit());
            if (!entries.isEmpty()) {
               mergeBoardCache(boardId, viewMode, entries);
            }
         } finally {
            IN_FLIGHT_REQUESTS.remove(requestKey);
         }
      }, EXECUTOR);
   }

   private static void loadCache() {
      GlobalLeaderboardCacheStore.State state = GlobalLeaderboardCacheStore.load();
      boardCaches = state.boardCaches();
      playerUuid = state.playerUuid();
      playerName = state.playerName();
      lastSyncHash = state.lastSyncHash();
      lastSyncAt = state.lastSyncAt();
      CACHE_VERSION.incrementAndGet();
   }

   private static void startupSync(Minecraft minecraft) {
      try {
         aggregateStats = GlobalStatsAggregator.aggregateSurvivalStats(minecraft);
         GlobalLeaderboardBoardRegistry.ensureStatsBoards(minecraft);
         boardIds = List.copyOf(GlobalLeaderboardBoardRegistry.loadBoardIds(minecraft));
         boardSnapshots = Map.copyOf(GlobalLeaderboardBoardRegistry.buildSnapshots(aggregateStats, boardIds));
         Legacy4J.LOGGER.debug("Global leaderboards startup aggregated {} stats into {} board snapshots", aggregateStats.size(), boardSnapshots.size());
         if (CONFIG.syncOnLaunch()) {
            syncChangedBoards();
         }

         if (CONFIG.prefetchAroundOnLaunch()) {
            String initialBoardId = initialBoardId();
            if (!initialBoardId.isBlank()) {
               requestBoard(initialBoardId, GlobalLeaderboardsScreen.ViewMode.AROUND_ME);
            }
         }

         if (CONFIG.prefetchTopOnLaunch()) {
            for (String boardId : boardSnapshots.keySet()) {
               requestBoard(boardId, GlobalLeaderboardsScreen.ViewMode.TOP);
            }
         }
      } catch (Exception err) {
         Legacy4J.LOGGER.warn("Global leaderboard startup failed", err);
      } finally {
         persistCache();
         CACHE_VERSION.incrementAndGet();
      }
   }

   private static String initialBoardId() {
      int defaultBoard = GlobalLeaderboardBoardRegistry.defaultBoardIndex();
      if (defaultBoard >= 0 && defaultBoard < boardIds.size()) {
         return boardIds.get(defaultBoard);
      }

      if (!boardSnapshots.isEmpty()) {
         return new ArrayList<>(boardSnapshots.keySet()).getFirst();
      }

      return boardIds.isEmpty() ? "" : boardIds.getFirst();
   }

   private static void syncChangedBoards() {
      String syncHash = String.valueOf(Objects.hash(playerUuid, playerName, boardSnapshots));
      if (syncHash.equals(lastSyncHash)) {
         return;
      }

      long now = System.currentTimeMillis();
      long cooldown = Math.max(0, CONFIG.syncCooldownSeconds()) * 1000L;
      if (lastSyncAt > 0 && now - lastSyncAt < cooldown) {
         return;
      }

      if (API_CLIENT.syncBoards(playerUuid, playerName, boardSnapshots)) {
         lastSyncHash = syncHash;
         lastSyncAt = now;
      }
   }

   private static boolean shouldRequestBoard(String boardId, GlobalLeaderboardsScreen.ViewMode viewMode) {
      if (!CONFIG.hasEndpoint() || boardId.isBlank()) {
         return false;
      }

      String requestKey = boardId + "|" + viewMode.name();
      long now = System.currentTimeMillis();
      long cooldown = Math.max(0, CONFIG.fetchCooldownSeconds()) * 1000L;
      Long requestedAt = REQUEST_TIMES.get(requestKey);
      if (requestedAt != null && now - requestedAt < cooldown) {
         return false;
      }

      GlobalLeaderboardBoardCache cache = boardCaches.get(boardId);
      if (cache == null) {
         return true;
      }

      List<GlobalLeaderboardEntry> entries = viewMode == GlobalLeaderboardsScreen.ViewMode.TOP ? cache.topEntries() : cache.aroundEntries();
      long fetchedAt = viewMode == GlobalLeaderboardsScreen.ViewMode.TOP ? cache.topFetchedAt() : cache.aroundFetchedAt();
      return entries.isEmpty() || fetchedAt <= 0 || now - fetchedAt >= cooldown;
   }

   private static synchronized void mergeBoardCache(String boardId, GlobalLeaderboardsScreen.ViewMode viewMode, List<GlobalLeaderboardEntry> entries) {
      LinkedHashMap<String, GlobalLeaderboardBoardCache> merged = new LinkedHashMap<>(boardCaches);
      GlobalLeaderboardBoardCache existing = merged.get(boardId);
      String displayNameKey = boardSnapshots.containsKey(boardId) ? boardSnapshots.get(boardId).displayNameKey() : boardId;
      if (existing == null) {
         existing = new GlobalLeaderboardBoardCache(boardId, displayNameKey, System.currentTimeMillis(), List.of(), List.of());
      }

      GlobalLeaderboardBoardCache updated = viewMode == GlobalLeaderboardsScreen.ViewMode.TOP
         ? existing.withTopEntries(entries, System.currentTimeMillis())
         : existing.withAroundEntries(entries, System.currentTimeMillis());
      merged.put(boardId, updated);
      boardCaches = Map.copyOf(merged);
      persistCache();
      CACHE_VERSION.incrementAndGet();
   }

   private static void persistCache() {
      GlobalLeaderboardCacheStore.save(new GlobalLeaderboardCacheStore.State(playerUuid, playerName, System.currentTimeMillis(), lastSyncHash, lastSyncAt, boardCaches));
   }
}
