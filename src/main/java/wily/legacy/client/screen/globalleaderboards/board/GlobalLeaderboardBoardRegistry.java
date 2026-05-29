package wily.legacy.client.screen.globalleaderboards.board;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardBoard;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardColumn;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardDifficulty;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardIcon;
import wily.legacy.api.client.leaderboards.LegacyLeaderboards;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardSnapshot;
import wily.legacy.client.screen.globalleaderboards.storage.GlobalLeaderboardStatCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LeaderboardsScreen;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.IOUtil;

public final class GlobalLeaderboardBoardRegistry {
   private static final Identifier LISTING_ID = Identifier.fromNamespaceAndPath("legacy", "leaderboard_listing.json");
   private static final String FARMING_BOARD = "legacy.menu.leaderboard.farming";
   private static final String MINING_BOARD = "legacy.menu.leaderboard.mining_blocks";
   private static final String KILLS_BOARD = "legacy.menu.leaderboard.kills";
   private static final String TRAVELLING_BOARD = "legacy.menu.leaderboard.travelling";
   private static final String GENERAL_BOARD = "stat.generalButton";
   private static final List<String> TRACKED_BOARD_IDS = List.of(FARMING_BOARD, MINING_BOARD, KILLS_BOARD, TRAVELLING_BOARD, GENERAL_BOARD);
   private static final Set<String> TRACKED_BOARDS = Set.copyOf(TRACKED_BOARD_IDS);
   private static final List<String> FARMING_ITEMS = List.of("egg", "wheat", "brown_mushroom", "red_mushroom", "sugar_cane", "milk_bucket", "pumpkin");
   private static final List<String> MINING_BLOCKS = List.of("dirt", "cobblestone", "sand", "stone", "gravel", "clay", "obsidian");
   private static final List<String> KILL_ENTITIES = List.of("zombie", "skeleton", "creeper", "spider", "zombified_piglin", "slime");
   private static final List<String> TRAVEL_STATS = List.of("walk_one_cm", "fall_one_cm", "minecart_one_cm", "boat_one_cm");
   private static final String DAYS_PLAYED = "days_played";
   private static final List<String> GENERAL_STATS = List.of("play_time", DAYS_PLAYED, "time_since_death", "time_since_rest");
   private static final Stat<Identifier> SKELETON_JOCKEY_STAT = Stats.CUSTOM.get(LegacyRegistries.SKELETON_JOCKEY_STAT);
   private static final Identifier SKELETON_JOCKEY_SPRITE = Legacy4J.createModLocation("icon/leaderboards/entity/skeleton_jockey");
   private static volatile List<LeaderboardsScreen.StatsBoard> statsBoards = List.of();
   private static volatile Map<String, LeaderboardsScreen.StatsBoard> statsBoardsById = Map.of();

   private GlobalLeaderboardBoardRegistry() {
   }

   public static List<String> loadBoardIds(Minecraft minecraft) {
      ensureStatsBoards(minecraft);
      return new ArrayList<>(TRACKED_BOARD_IDS);
   }

   public static List<String> trackedBoardIds() {
      return TRACKED_BOARD_IDS;
   }

   public static String boardId(int index) {
      return index >= 0 && index < TRACKED_BOARD_IDS.size() ? TRACKED_BOARD_IDS.get(index) : "board_" + index;
   }

   public static int defaultBoardIndex() {
      return TRACKED_BOARD_IDS.indexOf(TRAVELLING_BOARD);
   }

   public static String defaultBoardId() {
      return TRAVELLING_BOARD;
   }

   public static List<LeaderboardsScreen.StatsBoard> statsBoards() {
      return statsBoards;
   }

   public static LeaderboardsScreen.StatsBoard statsBoard(int index) {
      return index >= 0 && index < statsBoards.size() ? statsBoards.get(index) : null;
   }

   public static LeaderboardsScreen.StatsBoard statsBoard(String boardId) {
      return statsBoardsById.get(boardId);
   }

   public static void registerBuiltInBoards(Minecraft minecraft) {
      ensureStatsBoards(Minecraft.getInstance());
      ensureTrackedBoardStats(Minecraft.getInstance());
      for (int i = 0; i < TRACKED_BOARD_IDS.size(); i++) {
         String boardId = TRACKED_BOARD_IDS.get(i);
         LeaderboardsScreen.StatsBoard statsBoard = statsBoardsById.get(boardId);
         if (statsBoard != null) {
            LegacyLeaderboards.registerBoard(createBuiltInBoard(boardId, i, statsBoard));
         }
      }
   }

   public static Map<String, GlobalLeaderboardBoardSnapshot> buildSnapshots(Object2IntMap<Stat<?>> aggregateStats, List<GlobalLeaderboardBoard> boards) {
      return buildSnapshots(aggregateStats, boards, GlobalLeaderboardDifficulty.NORMAL);
   }

   public static Map<String, GlobalLeaderboardBoardSnapshot> buildSnapshots(Map<GlobalLeaderboardDifficulty, ? extends Object2IntMap<Stat<?>>> aggregateStats, List<GlobalLeaderboardBoard> boards) {
      LinkedHashMap<String, GlobalLeaderboardBoardSnapshot> snapshots = new LinkedHashMap<>();
      aggregateStats.forEach((difficulty, stats) -> snapshots.putAll(buildSnapshots(stats, boards, difficulty)));
      return snapshots;
   }

   public static Map<String, GlobalLeaderboardBoardSnapshot> buildSnapshots(Object2IntMap<Stat<?>> aggregateStats, List<GlobalLeaderboardBoard> boards, GlobalLeaderboardDifficulty difficulty) {
      ensureStatsBoards(Minecraft.getInstance());
      LinkedHashMap<String, GlobalLeaderboardBoardSnapshot> snapshots = new LinkedHashMap<>();
      ensureTrackedBoardStats(Minecraft.getInstance());

      for (GlobalLeaderboardBoard board : boards) {
         if (!LegacyLeaderboards.LEGACY_PROVIDER.equals(board.providerId())) {
            continue;
         }
         if (!supportsDifficulty(board.id(), difficulty)) {
            continue;
         }
         LeaderboardsScreen.StatsBoard statsBoard = statsBoardsById.get(board.id());
         if (statsBoard == null) {
            continue;
         }
         LinkedHashMap<String, Integer> encodedStats = new LinkedHashMap<>();
         int total = 0;
         int skeletonJockeyKills = KILLS_BOARD.equals(board.id()) ? aggregateStats.getInt(SKELETON_JOCKEY_STAT) : 0;
         boolean skeletonJockeyAdded = false;
         for (Stat<?> stat : statsBoard.statsList) {
            int value = aggregateStats.getInt(stat);
            if (skeletonJockeyKills > 0 && registryValueMatches(stat.getType().getRegistry(), stat.getValue(), "skeleton")) {
               value = Math.max(0, value - skeletonJockeyKills);
            }
            if (value > 0) {
               String encoded = GlobalLeaderboardStatCodec.encode(stat);
               if (!encoded.isBlank()) {
                  encodedStats.put(encoded, value);
                  total += value;
               }
            }
            if (skeletonJockeyKills > 0 && !skeletonJockeyAdded && registryValueMatches(stat.getType().getRegistry(), stat.getValue(), "spider")) {
               String id = GlobalLeaderboardStatCodec.encode(SKELETON_JOCKEY_STAT);
               if (!id.isBlank()) {
                  encodedStats.put(id, skeletonJockeyKills);
               }
               total += skeletonJockeyKills;
               skeletonJockeyAdded = true;
            }
         }
         String boardId = difficulty.boardId(board.id());
         snapshots.put(GlobalLeaderboardBoard.key(board.providerId(), boardId), new GlobalLeaderboardBoardSnapshot(boardId, board.id(), total, encodedStats));
      }

      return snapshots;
   }

   public static boolean supportsDifficulty(String boardId, GlobalLeaderboardDifficulty difficulty) {
      return !KILLS_BOARD.equals(boardId) || difficulty != GlobalLeaderboardDifficulty.PEACEFUL;
   }

   public static synchronized void ensureStatsBoards(Minecraft minecraft) {
      if (minecraft == null) {
         return;
      }

      @Nullable Resource resource = minecraft.getResourceManager().getResource(LISTING_ID).orElse(null);
      if (resource == null) {
         return;
      }

      try (BufferedReader reader = resource.openAsReader()) {
         List<LeaderboardsScreen.StatsBoard> boards = new ArrayList<>();
         JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
         JsonArray listing = root.has("listing") && root.get("listing").isJsonArray() ? root.getAsJsonArray("listing") : new JsonArray();
         LinkedHashMap<String, JsonObject> definitions = new LinkedHashMap<>();
         int index = 0;
         for (JsonElement element : listing) {
            if (element.isJsonObject()) {
               JsonObject object = element.getAsJsonObject();
               String boardId = boardId(object, index);
               if (TRACKED_BOARDS.contains(boardId)) {
                  definitions.put(boardId, object);
               }
            }
            index++;
         }

         for (String boardId : TRACKED_BOARD_IDS) {
            JsonObject object = definitions.get(boardId);
            if (object != null) {
               boards.add(statsBoardFromJson(boardId, object));
            }
         }
         if (!boards.isEmpty()) {
            statsBoards = List.copyOf(boards);
            LinkedHashMap<String, LeaderboardsScreen.StatsBoard> byId = new LinkedHashMap<>();
            for (int i = 0; i < statsBoards.size() && i < TRACKED_BOARD_IDS.size(); i++) {
               byId.put(TRACKED_BOARD_IDS.get(i), statsBoards.get(i));
            }
            statsBoardsById = Map.copyOf(byId);
         }
      } catch (IOException | RuntimeException err) {
         Legacy4J.LOGGER.warn("Failed to load global leaderboard boards", err);
      }
   }

   public static void ensureTrackedBoardStats(Minecraft minecraft) {
      if (minecraft == null || statsBoards.isEmpty()) {
         return;
      }

      for (int index = 0; index < statsBoards.size() && index < TRACKED_BOARD_IDS.size(); index++) {
         addTrackedStats(TRACKED_BOARD_IDS.get(index), statsBoards.get(index));
      }
   }

   public static boolean addStat(LeaderboardsScreen.StatsBoard board, Stat<?> stat) {
      if (!board.canAdd(stat) || board.statsList.contains(stat)) {
         return false;
      }

      board.statsList.add(stat);
      try {
         board.renderables.add(board.getRenderable(stat));
      } catch (RuntimeException ex) {
         board.renderables.add(SimpleLayoutRenderable.createDrawString(Component.literal("?"), 0, 0, 24, 24, 0xFFFFFFFF, false));
      }

      return true;
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static LeaderboardsScreen.StatsBoard statsBoardFromJson(String boardId, JsonObject object) {
      StatType<?> statType = FactoryAPIPlatform.getRegistryValue(Identifier.tryParse(GsonHelper.getAsString(object, "type")), BuiltInRegistries.STAT_TYPE);
      if (statType == null) {
         throw new IllegalStateException("Missing stat type for leaderboard board");
      }

      Component displayName = object.has("displayName") ? Component.translatable(GsonHelper.getAsString(object, "displayName")) : statType.getDisplayName();
      Predicate<Stat<?>> trackedPredicate = trackedBoardPredicate(boardId);
      LeaderboardsScreen.StatsBoard board;
      if (object.get("predicate") instanceof JsonObject predicateObject) {
         java.util.function.Predicate predicate = IOUtil.registryMatches(statType.getRegistry(), predicateObject);
         board = LeaderboardsScreen.StatsBoard.create(statType, displayName, stat -> predicate.test(stat.getValue()) && trackedPredicate.test(stat));
      } else {
         board = LeaderboardsScreen.StatsBoard.create(statType, displayName, trackedPredicate);
      }
      applyTrackedBoardIcons(boardId, board);
      return board;
   }

   private static String boardId(JsonObject object, int index) {
      if (object.has("displayName") && object.get("displayName").isJsonPrimitive()) {
         return object.get("displayName").getAsString();
      }

      if (object.has("type") && object.get("type").isJsonPrimitive()) {
         return object.get("type").getAsString() + "_" + index;
      }

      return "board_" + index;
   }

   private static Predicate<Stat<?>> trackedBoardPredicate(String boardId) {
      return switch (boardId) {
         case FARMING_BOARD -> stat -> hasValuePath(stat, FARMING_ITEMS);
         case MINING_BOARD -> stat -> hasValuePath(stat, MINING_BLOCKS);
         case KILLS_BOARD -> stat -> hasValuePath(stat, KILL_ENTITIES);
         case TRAVELLING_BOARD -> stat -> hasValuePath(stat, TRAVEL_STATS);
         case GENERAL_BOARD -> stat -> hasValuePath(stat, GENERAL_STATS);
         default -> stat -> false;
      };
   }

   private static GlobalLeaderboardBoard createBuiltInBoard(String boardId, int order, LeaderboardsScreen.StatsBoard statsBoard) {
      ArrayList<GlobalLeaderboardColumn> columns = new ArrayList<>();
      for (Stat<?> stat : statsBoard.statsList) {
         String id = GlobalLeaderboardStatCodec.encode(stat);
         if (!id.isBlank()) {
            columns.add(new GlobalLeaderboardColumn(id, statName(stat), GlobalLeaderboardIcon.custom((width, height) -> statsBoard.getRenderable(stat)), value -> Component.literal(stat.format(statValue(value)))));
         }
         if (KILLS_BOARD.equals(boardId) && registryValueMatches(stat.getType().getRegistry(), stat.getValue(), "spider")) {
            String skeletonJockeyId = GlobalLeaderboardStatCodec.encode(SKELETON_JOCKEY_STAT);
            if (!skeletonJockeyId.isBlank()) {
               columns.add(new GlobalLeaderboardColumn(skeletonJockeyId, Component.translatable("legacy.menu.leaderboard.skeleton_jockey"), GlobalLeaderboardIcon.sprite(SKELETON_JOCKEY_SPRITE)));
            }
         }
      }
      return new GlobalLeaderboardBoard(LegacyLeaderboards.LEGACY_PROVIDER, boardId, statsBoard.displayName, order, columns);
   }

   private static Component statName(Stat<?> stat) {
      Object value = stat.getValue();
      if (value instanceof EntityType<?> entityType) {
         return entityType.getDescription();
      }
      if (value instanceof ItemLike item && item.asItem() != Items.AIR) {
         return Component.translatable(item.asItem().getDescriptionId());
      }
      return Component.translatable("stat." + value.toString().replace(':', '.'));
   }

   private static int statValue(long value) {
      if (value > Integer.MAX_VALUE) {
         return Integer.MAX_VALUE;
      }
      if (value < Integer.MIN_VALUE) {
         return Integer.MIN_VALUE;
      }
      return (int) value;
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void addTrackedStats(String boardId, LeaderboardsScreen.StatsBoard board) {
      StatType statType = board.type;
      for (String path : trackedPaths(boardId)) {
         Object value = DAYS_PLAYED.equals(path) ? LegacyRegistries.DAYS_PLAYED_STAT : FactoryAPIPlatform.getRegistryValue(Identifier.withDefaultNamespace(path), statType.getRegistry());
         if (value != null) {
            addStat(board, statType.get(value));
         }
      }
   }

   private static List<String> trackedPaths(String boardId) {
      return switch (boardId) {
         case FARMING_BOARD -> FARMING_ITEMS;
         case MINING_BOARD -> MINING_BLOCKS;
         case KILLS_BOARD -> KILL_ENTITIES;
         case TRAVELLING_BOARD -> TRAVEL_STATS;
         case GENERAL_BOARD -> GENERAL_STATS;
         default -> List.of();
      };
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void applyTrackedBoardIcons(String boardId, LeaderboardsScreen.StatsBoard board) {
      if (FARMING_BOARD.equals(boardId)) {
         addItemOverride(board, "egg", Items.EGG);
         addItemOverride(board, "wheat", Items.WHEAT);
         addItemOverride(board, "brown_mushroom", Items.BROWN_MUSHROOM);
         addItemOverride(board, "red_mushroom", Items.RED_MUSHROOM);
         addItemOverride(board, "sugar_cane", Items.SUGAR_CANE);
         addItemOverride(board, "milk_bucket", Items.MILK_BUCKET);
         addItemOverride(board, "pumpkin", Items.PUMPKIN);
         return;
      }

      if (MINING_BOARD.equals(boardId)) {
         addItemOverride(board, "dirt", Blocks.DIRT);
         addItemOverride(board, "cobblestone", Blocks.COBBLESTONE);
         addItemOverride(board, "sand", Blocks.SAND);
         addItemOverride(board, "stone", Blocks.STONE);
         addItemOverride(board, "gravel", Blocks.GRAVEL);
         addItemOverride(board, "clay", Blocks.CLAY);
         addItemOverride(board, "obsidian", Blocks.OBSIDIAN);
         return;
      }

      if (TRAVELLING_BOARD.equals(boardId)) {
         addSpriteOverride(board, "walk_one_cm", lceSprite("travel/walk_one_cm"));
         addSpriteOverride(board, "fall_one_cm", lceSprite("travel/fall_one_cm"));
         addItemOverride(board, "minecart_one_cm", Items.MINECART);
         addItemOverride(board, "boat_one_cm", Items.OAK_BOAT);
         return;
      }

   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void addSpriteOverride(LeaderboardsScreen.StatsBoard board, String path, Identifier sprite) {
      LegacyIconHolder iconHolder = new LegacyIconHolder(24, 24);
      iconHolder.iconSprite = sprite;
      board.statIconOverrides.add(new LeaderboardsScreen.StatIconOverride(board.type, value -> registryValueMatches(board.type.getRegistry(), value, path), iconHolder));
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void addItemOverride(LeaderboardsScreen.StatsBoard board, String path, ItemLike itemLike) {
      LegacyIconHolder iconHolder = new LazyItemIconHolder(24, 24, itemLike);
      board.statIconOverrides.add(new LeaderboardsScreen.StatIconOverride(board.type, value -> registryValueMatches(board.type.getRegistry(), value, path), iconHolder));
   }

   private static Identifier lceSprite(String path) {
      return Identifier.fromNamespaceAndPath("legacy", "icon/leaderboards/lce/" + path);
   }

   @SuppressWarnings("unchecked")
   private static boolean registryValueMatches(Registry registry, Object value, String expectedPath) {
      if (registry == null || value == null) {
         return false;
      }

      Identifier valueId = registry.getKey(value);
      return valueId != null && expectedPath.equals(valueId.getPath());
   }

   @SuppressWarnings("unchecked")
   private static boolean hasValuePath(Stat<?> stat, List<String> allowedPaths) {
      Registry<Object> registry = (Registry<Object>) stat.getType().getRegistry();
      if (registry == null || stat.getValue() == null) {
         return false;
      }

      Identifier valueId = registry.getKey(stat.getValue());
      return valueId != null && allowedPaths.contains(valueId.getPath());
   }
}
