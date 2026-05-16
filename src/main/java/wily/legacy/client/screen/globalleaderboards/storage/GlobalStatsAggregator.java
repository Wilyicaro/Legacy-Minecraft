package wily.legacy.client.screen.globalleaderboards.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import wily.factoryapi.FactoryAPIPlatform;

public final class GlobalStatsAggregator {
   private GlobalStatsAggregator() {
   }

   public static Object2IntOpenHashMap<Stat<?>> aggregateSurvivalStats(Minecraft minecraft) {
      Object2IntOpenHashMap<Stat<?>> aggregateStats = new Object2IntOpenHashMap<>();
      UUID profileId = minecraft.getUser().getProfileId();
      if (profileId == null) {
         return aggregateStats;
      }

      Set<Path> loadedStats = new HashSet<>();
      String statsFile = profileId + ".json";
      for (LevelSummary summary : loadSummaries(minecraft)) {
         if (isSurvival(summary)) {
            Path worldPath = minecraft.getLevelSource().getBaseDir().resolve(summary.getLevelId());
            loadWorldStats(worldPath.resolve("stats").resolve(statsFile), aggregateStats, loadedStats);
            loadWorldStats(worldPath.resolve("players").resolve("stats").resolve(statsFile), aggregateStats, loadedStats);
         }
      }

      return aggregateStats;
   }

   private static List<LevelSummary> loadSummaries(Minecraft minecraft) {
      try {
         LevelStorageSource.LevelCandidates candidates = minecraft.getLevelSource().findLevelCandidates();
         return candidates.isEmpty() ? List.of() : minecraft.getLevelSource().loadLevelSummaries(candidates).join();
      } catch (LevelStorageException err) {
         return List.of();
      }
   }

   private static boolean isSurvival(LevelSummary summary) {
      return summary != null && (summary.isHardcore() || summary.getSettings().gameType() == GameType.SURVIVAL);
   }

   private static void loadWorldStats(Path statsPath, Object2IntOpenHashMap<Stat<?>> aggregateStats, Set<Path> loadedStats) {
      if (!loadedStats.add(statsPath.toAbsolutePath().normalize())) {
         return;
      }

      if (!Files.isRegularFile(statsPath)) {
         return;
      }

      try (BufferedReader reader = Files.newBufferedReader(statsPath)) {
         JsonObject root = GsonHelper.parse(reader);
         JsonObject statsRoot = GsonHelper.getAsJsonObject(root, "stats", new JsonObject());
         for (var statTypeEntry : statsRoot.entrySet()) {
            if (!(statTypeEntry.getValue() instanceof JsonObject statsByValue)) {
               continue;
            }

            Identifier statTypeId = Identifier.tryParse(statTypeEntry.getKey());
            if (statTypeId != null) {
               addStatsByType(aggregateStats, statTypeId, statsByValue);
            }
         }
      } catch (Exception ignored) {
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void addStatsByType(Object2IntOpenHashMap<Stat<?>> aggregateStats, Identifier statTypeId, JsonObject statsByValue) {
      StatType statType = FactoryAPIPlatform.getRegistryValue(statTypeId, BuiltInRegistries.STAT_TYPE);
      if (statType == null) {
         return;
      }

      for (var statEntry : statsByValue.entrySet()) {
         if (!(statEntry.getValue() instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            continue;
         }

         Identifier statValueId = Identifier.tryParse(statEntry.getKey());
         if (statValueId == null) {
            continue;
         }

         Object statValue = FactoryAPIPlatform.getRegistryValue(statValueId, statType.getRegistry());
         if (statValue == null) {
            continue;
         }

         Stat<?> stat = statType.get(statValue);
         aggregateStats.put(stat, aggregateStats.getInt(stat) + primitive.getAsInt());
      }
   }
}
