package wily.legacy.client.screen.globalleaderboards;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.stats.Stat;
import wily.legacy.Legacy4J;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardBoard;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardDifficulty;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardPage;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardProvider;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardRequest;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardRow;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardViewMode;
import wily.legacy.api.client.leaderboards.LegacyLeaderboards;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.globalleaderboards.api.GlobalLeaderboardApiClient;
import wily.legacy.client.screen.globalleaderboards.board.GlobalLeaderboardBoardRegistry;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardCache;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardSnapshot;
import wily.legacy.client.screen.globalleaderboards.screen.GlobalLeaderboardsScreen;
import wily.legacy.client.screen.globalleaderboards.storage.GlobalLeaderboardCacheStore;
import wily.legacy.client.screen.globalleaderboards.storage.GlobalStatsAggregator;
import wily.legacy.globalleaderboards.GlobalDifficultyStatsStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class GlobalLeaderboardsFeature {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "legacy-global-leaderboards");
        thread.setDaemon(true);
        return thread;
    });
    private static final GlobalLeaderboardProvider LEGACY_PROVIDER = new GlobalLeaderboardProvider() {
        @Override
        public String id() {
            return LegacyLeaderboards.LEGACY_PROVIDER;
        }

        @Override
        public CompletableFuture<GlobalLeaderboardPage> fetch(GlobalLeaderboardRequest request) {
            if (isOptedOut()) {
                return CompletableFuture.completedFuture(GlobalLeaderboardPage.failed());
            }
            return CompletableFuture.completedFuture(apiClient(GlobalLeaderboardsConfig.get()).fetchBoard(request));
        }
    };
    private static final Set<String> IN_FLIGHT_REQUESTS = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> REQUEST_TIMES = new ConcurrentHashMap<>();
    private static final AtomicInteger CACHE_VERSION = new AtomicInteger();
    private static volatile boolean started;
    private static volatile boolean launchTaskScheduled;
    private static volatile int leaderboardRegistryVersion = -1;
    private static volatile String playerUuid = "";
    private static volatile String playerName = "";
    private static volatile List<GlobalLeaderboardBoard> boards = List.of();
    private static volatile Map<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> aggregateStats = GlobalDifficultyStatsStore.emptyStats();
    private static volatile Map<String, GlobalLeaderboardBoardSnapshot> boardSnapshots = Map.of();
    private static volatile Map<String, GlobalLeaderboardBoardCache> boardCaches = Map.of();
    private static volatile String lastSyncHash = "";
    private static volatile long lastSyncAt;
    private static volatile GlobalLeaderboardsConfig.Values apiConfig;
    private static volatile GlobalLeaderboardApiClient apiClient;

    private GlobalLeaderboardsFeature() {
    }

    public static synchronized void init() {
        if (started) {
            return;
        }

        started = true;
        LegacyLeaderboards.registerProvider(LEGACY_PROVIDER);
        loadCache();
    }

    public static void onClientStarted(Minecraft minecraft) {
        ensureStarted(minecraft);
    }

    public static Screen createScreen(Screen parent, Supplier<Screen> fallback) {
        if (isOptedOut()) {
            return fallback.get();
        }
        try {
            return new GlobalLeaderboardsScreen(parent);
        } catch (RuntimeException err) {
            Legacy4J.LOGGER.warn("Falling back to legacy leaderboards", err);
            return fallback.get();
        }
    }

    public static synchronized void ensureStarted(Minecraft minecraft) {
        if (isOptedOut()) {
            return;
        }
        UUID profileId = /*? if >1.20.2 {*/minecraft.getUser().getProfileId()/*?} else {*//*minecraft.getUser().getGameProfile().getId()*//*?}*/;
        String nextPlayerUuid = profileId == null ? "" : profileId.toString();
        String nextPlayerName = minecraft.getUser().getName();
        boolean playerChanged = !playerKey(nextPlayerUuid, nextPlayerName).equals(playerKey());
        boolean nameChanged = !nextPlayerName.equals(playerName);
        if (launchTaskScheduled && !playerChanged && !nameChanged) {
            return;
        }

        playerUuid = nextPlayerUuid;
        playerName = nextPlayerName;
        if (playerChanged) {
            lastSyncHash = "";
            lastSyncAt = 0L;
            boardCaches = Map.of();
            REQUEST_TIMES.clear();
            CACHE_VERSION.incrementAndGet();
        }
        launchTaskScheduled = true;
        CompletableFuture.runAsync(() -> startupSync(minecraft), EXECUTOR);
    }

    public static int cacheVersion() {
        return CACHE_VERSION.get();
    }

    public static List<GlobalLeaderboardBoard> boards() {
        return boards;
    }

    public static List<String> boardIds() {
        return boards.stream().map(GlobalLeaderboardBoard::id).toList();
    }

    public static GlobalLeaderboardBoard board(int index) {
        return index >= 0 && index < boards.size() ? boards.get(index) : null;
    }

    public static String boardId(int index) {
        GlobalLeaderboardBoard board = board(index);
        return board == null ? "board_" + index : board.id();
    }

    public static String playerUuid() {
        return playerUuid;
    }

    public static String playerName() {
        return playerName;
    }

    public static Object2IntOpenHashMap<Stat<?>> aggregateStats() {
        Object2IntOpenHashMap<Stat<?>> stats = new Object2IntOpenHashMap<>();
        aggregateStats.values().forEach(values -> values.object2IntEntrySet().forEach(entry -> stats.put(entry.getKey(), stats.getInt(entry.getKey()) + entry.getIntValue())));
        return stats;
    }

    public static Map<String, GlobalLeaderboardBoardSnapshot> boardSnapshots() {
        return boardSnapshots;
    }

    public static GlobalLeaderboardBoardSnapshot boardSnapshot(GlobalLeaderboardBoard board) {
        return boardSnapshot(board, GlobalLeaderboardDifficulty.NORMAL);
    }

    public static GlobalLeaderboardBoardSnapshot boardSnapshot(GlobalLeaderboardBoard board, GlobalLeaderboardDifficulty difficulty) {
        if (board == null) {
            return null;
        }
        GlobalLeaderboardBoard requestBoard = requestBoard(board, difficulty);
        return boardSnapshots.get(requestBoard.key());
    }

    public static GlobalLeaderboardBoardSnapshot boardSnapshot(String boardId) {
        return boardSnapshots.get(GlobalLeaderboardBoard.key(LegacyLeaderboards.LEGACY_PROVIDER, GlobalLeaderboardDifficulty.NORMAL.boardId(boardId)));
    }

    public static GlobalLeaderboardBoardCache boardCache(GlobalLeaderboardBoard board) {
        return board == null ? null : boardCaches.get(board.key());
    }

    public static List<GlobalLeaderboardRow> entries(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode) {
        return entries(board, viewMode, GlobalLeaderboardDifficulty.NORMAL);
    }

    public static List<GlobalLeaderboardRow> entries(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode, GlobalLeaderboardDifficulty difficulty) {
        board = requestBoard(board, difficulty);
        GlobalLeaderboardBoardCache cache = boardCache(board);
        if (cache == null) {
            return List.of();
        }

        return viewMode == GlobalLeaderboardViewMode.TOP ? cache.topEntries() : cache.aroundEntries();
    }

    public static boolean hasEndpoint() {
        return GlobalLeaderboardsConfig.get().hasEndpoint();
    }

    public static boolean isOptedOut() {
        return LegacyOptions.globalLeaderboardsOptOut.get();
    }

    public static void optOut() {
        LegacyOptions.globalLeaderboardsOptOut.set(true);
        LegacyOptions.globalLeaderboardsOptOut.save();
        launchTaskScheduled = false;
        IN_FLIGHT_REQUESTS.clear();
        REQUEST_TIMES.clear();
        apiConfig = null;
        apiClient = null;
        CACHE_VERSION.incrementAndGet();
    }

    public static void optIn() {
        LegacyOptions.globalLeaderboardsOptOut.set(false);
        LegacyOptions.globalLeaderboardsOptOut.save();
        launchTaskScheduled = false;
        CACHE_VERSION.incrementAndGet();
        ensureStarted(Minecraft.getInstance());
    }

    public static void setEnabled(boolean enabled) {
        if (enabled) {
            optIn();
        } else {
            optOut();
        }
    }

    public static void refreshBoards(Minecraft minecraft) {
        GlobalLeaderboardBoardRegistry.registerBuiltInBoards(minecraft);
        int version = LegacyLeaderboards.version();
        if (version == leaderboardRegistryVersion) {
            return;
        }

        boards = LegacyLeaderboards.boards();
        leaderboardRegistryVersion = version;
        boardSnapshots = Map.copyOf(GlobalLeaderboardBoardRegistry.buildSnapshots(aggregateStats, boards));
        CACHE_VERSION.incrementAndGet();
    }

    public static void requestBoard(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode) {
        requestBoard(board, viewMode, GlobalLeaderboardDifficulty.NORMAL);
    }

    public static void requestBoard(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode, GlobalLeaderboardDifficulty difficulty) {
        GlobalLeaderboardBoard queryBoard = requestBoard(board, difficulty);
        if (isOptedOut() || !shouldRequestBoard(queryBoard, viewMode)) {
            return;
        }

        String requestKey = requestKey(queryBoard, viewMode);
        String requestPlayerUuid = playerUuid;
        String requestPlayerName = playerName;
        String requestPlayerKey = playerKey();
        if (!IN_FLIGHT_REQUESTS.add(requestKey)) {
            return;
        }
        REQUEST_TIMES.put(requestKey, System.currentTimeMillis());

        CompletableFuture.runAsync(() -> {
            if (isOptedOut()) {
                IN_FLIGHT_REQUESTS.remove(requestKey);
                return;
            }
            GlobalLeaderboardProvider provider = LegacyLeaderboards.provider(queryBoard.providerId());
            if (provider == null) {
                IN_FLIGHT_REQUESTS.remove(requestKey);
                return;
            }

            GlobalLeaderboardsConfig.Values config = GlobalLeaderboardsConfig.get();
            GlobalLeaderboardRequest request = new GlobalLeaderboardRequest(queryBoard, viewMode, requestPlayerUuid, requestPlayerName, config.aroundWindow(), config.topLimit());
            CompletableFuture<GlobalLeaderboardPage> future;
            try {
                future = provider.fetch(request);
            } catch (RuntimeException err) {
                Legacy4J.LOGGER.warn("Failed to request global leaderboard board {}", queryBoard.id(), err);
                IN_FLIGHT_REQUESTS.remove(requestKey);
                return;
            }
            if (future == null) {
                Legacy4J.LOGGER.warn("Global leaderboard provider {} returned no request for board {}", queryBoard.providerId(), queryBoard.id());
                IN_FLIGHT_REQUESTS.remove(requestKey);
                return;
            }

            future.whenComplete((page, err) -> {
                try {
                    if (err != null) {
                        Legacy4J.LOGGER.warn("Failed to fetch global leaderboard board {}", queryBoard.id(), err);
                    } else if (!isOptedOut() && page != null && page.successful() && requestPlayerKey.equals(playerKey())) {
                        mergeBoardCache(queryBoard, viewMode, page.rows());
                    }
                } finally {
                    IN_FLIGHT_REQUESTS.remove(requestKey);
                }
            });
        }, EXECUTOR);
    }

    private static void loadCache() {
        GlobalLeaderboardCacheStore.State state = GlobalLeaderboardCacheStore.load();
        boardCaches = state.boardCaches();
        playerUuid = state.playerUuid();
        playerName = state.playerName();
        lastSyncHash = state.lastSyncHash();
        lastSyncAt = lastSyncHash.isBlank() ? 0L : state.lastSyncAt();
        CACHE_VERSION.incrementAndGet();
    }

    private static void startupSync(Minecraft minecraft) {
        try {
            if (isOptedOut()) {
                return;
            }
            aggregateStats = GlobalStatsAggregator.aggregateSurvivalStats(minecraft);
            refreshBoards(minecraft);
            boardSnapshots = Map.copyOf(GlobalLeaderboardBoardRegistry.buildSnapshots(aggregateStats, boards));
            Legacy4J.LOGGER.debug("Global leaderboards startup aggregated difficulty stats into {} board snapshots", boardSnapshots.size());
            GlobalLeaderboardsConfig.Values config = GlobalLeaderboardsConfig.get();
            if (config.syncOnLaunch()) {
                syncChangedBoards(config);
            }

            if (config.prefetchAroundOnLaunch()) {
                GlobalLeaderboardBoard initialBoard = initialBoard();
                if (initialBoard != null) {
                    requestBoard(initialBoard, GlobalLeaderboardViewMode.AROUND_ME);
                }
            }

            if (config.prefetchTopOnLaunch()) {
                GlobalLeaderboardBoard initialBoard = initialBoard();
                if (initialBoard != null) {
                    requestBoard(initialBoard, GlobalLeaderboardViewMode.TOP);
                }
            }
        } catch (Exception err) {
            Legacy4J.LOGGER.warn("Global leaderboard startup failed", err);
        } finally {
            persistCache();
            CACHE_VERSION.incrementAndGet();
        }
    }

    private static GlobalLeaderboardBoard initialBoard() {
        for (GlobalLeaderboardBoard board : boards) {
            if (GlobalLeaderboardBoardRegistry.defaultBoardId().equals(board.id()) && !board.columns().isEmpty()) {
                return board;
            }
        }

        for (GlobalLeaderboardBoard board : boards) {
            if (!board.columns().isEmpty()) {
                return board;
            }
        }

        return boards.isEmpty() ? null : boards.get(0);
    }

    private static void syncChangedBoards(GlobalLeaderboardsConfig.Values config) {
        if (isOptedOut()) {
            return;
        }
        LinkedHashMap<String, GlobalLeaderboardBoardSnapshot> legacySnapshots = new LinkedHashMap<>();
        boardSnapshots.forEach((key, snapshot) -> {
            if (key.startsWith(LegacyLeaderboards.LEGACY_PROVIDER + "|")) {
                legacySnapshots.put(key, snapshot);
            }
        });
        if (legacySnapshots.values().stream().noneMatch(snapshot -> snapshot.totalScore() > 0 && !snapshot.statValues().isEmpty())) {
            return;
        }

        String syncHash = String.valueOf(Objects.hash(playerUuid, playerName, legacySnapshots));
        if (syncHash.equals(lastSyncHash)) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldown = Math.max(0, config.syncCooldownSeconds()) * 1000L;
        if (lastSyncAt > 0 && now - lastSyncAt < cooldown) {
            return;
        }

        if (!isOptedOut() && apiClient(config).syncBoards(playerUuid, playerName, legacySnapshots)) {
            lastSyncAt = now;
            lastSyncHash = syncHash;
        }
    }

    private static GlobalLeaderboardApiClient apiClient(GlobalLeaderboardsConfig.Values config) {
        GlobalLeaderboardApiClient client = apiClient;
        if (client != null && config.equals(apiConfig)) {
            return client;
        }

        synchronized (GlobalLeaderboardsFeature.class) {
            if (apiClient == null || !config.equals(apiConfig)) {
                apiConfig = config;
                apiClient = new GlobalLeaderboardApiClient(config);
            }
            return apiClient;
        }
    }

    private static GlobalLeaderboardBoard requestBoard(GlobalLeaderboardBoard board, GlobalLeaderboardDifficulty difficulty) {
        if (board == null || !LegacyLeaderboards.LEGACY_PROVIDER.equals(board.providerId())) {
            return board;
        }
        GlobalLeaderboardDifficulty value = difficulty == null ? GlobalLeaderboardDifficulty.NORMAL : difficulty;
        return new GlobalLeaderboardBoard(board.providerId(), value.boardId(board.id()), board.displayName(), board.order(), board.columns());
    }

    private static boolean shouldRequestBoard(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode) {
        if (board == null || board.id().isBlank() || LegacyLeaderboards.provider(board.providerId()) == null) {
            return false;
        }

        String requestKey = requestKey(board, viewMode);
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0, GlobalLeaderboardsConfig.get().fetchCooldownSeconds()) * 1000L;
        Long requestedAt = REQUEST_TIMES.get(requestKey);
        if (requestedAt != null && now - requestedAt < cooldown) {
            return false;
        }

        GlobalLeaderboardBoardCache cache = boardCaches.get(board.key());
        if (cache == null) {
            return true;
        }
        if (viewMode == GlobalLeaderboardViewMode.TOP && cache.topEntries().isEmpty()) {
            return true;
        }
        if (viewMode == GlobalLeaderboardViewMode.AROUND_ME && cache.aroundEntries().isEmpty()) {
            return true;
        }

        long fetchedAt = viewMode == GlobalLeaderboardViewMode.TOP ? cache.topFetchedAt() : cache.aroundFetchedAt();
        return fetchedAt <= 0 || now - fetchedAt >= cooldown;
    }

    private static synchronized void mergeBoardCache(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode, List<GlobalLeaderboardRow> entries) {
        LinkedHashMap<String, GlobalLeaderboardBoardCache> merged = new LinkedHashMap<>(boardCaches);
        GlobalLeaderboardBoardCache existing = merged.get(board.key());
        if (existing == null) {
            existing = new GlobalLeaderboardBoardCache(board.providerId(), board.id(), board.id(), 0L, List.of(), List.of(), 0L, 0L);
        }

        GlobalLeaderboardBoardCache updated = viewMode == GlobalLeaderboardViewMode.TOP
                ? existing.withTopEntries(entries, System.currentTimeMillis())
                : existing.withAroundEntries(entries, System.currentTimeMillis());
        merged.put(board.key(), updated);
        boardCaches = Map.copyOf(merged);
        persistCache();
        CACHE_VERSION.incrementAndGet();
    }

    private static void persistCache() {
        GlobalLeaderboardCacheStore.save(new GlobalLeaderboardCacheStore.State(playerUuid, playerName, System.currentTimeMillis(), lastSyncHash, lastSyncAt, boardCaches));
    }

    private static String requestKey(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode) {
        return viewMode == GlobalLeaderboardViewMode.AROUND_ME ? board.key() + "|" + viewMode.name() + "|" + playerKey() : board.key() + "|" + viewMode.name();
    }

    private static String playerKey() {
        return playerKey(playerUuid, playerName);
    }

    private static String playerKey(String uuid, String name) {
        return uuid.isBlank() ? "name:" + name : "uuid:" + uuid;
    }
}
