package wily.legacy.client.screen.globalleaderboards.storage;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.stats.Stat;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardDifficulty;
import wily.legacy.globalleaderboards.GlobalDifficultyStatsStore;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GlobalStatsAggregator {
    private GlobalStatsAggregator() {
    }

    public static Map<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> aggregateSurvivalStats(Minecraft minecraft) {
        EnumMap<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> aggregateStats = GlobalDifficultyStatsStore.emptyStats();
        UUID profileId = /*? if >1.20.2 {*/minecraft.getUser().getProfileId()/*?} else {*//*minecraft.getUser().getGameProfile().getId()*//*?}*/;
        if (profileId == null) {
            return aggregateStats;
        }

        for (LevelSummary summary : loadSummaries(minecraft)) {
            if (isSurvival(summary)) {
                Path worldPath = minecraft.getLevelSource().getBaseDir().resolve(summary.getLevelId());
                mergeStats(aggregateStats, GlobalDifficultyStatsStore.readPlayer(GlobalDifficultyStatsStore.path(worldPath), profileId));
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

    private static void mergeStats(Map<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> target, Map<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> source) {
        source.forEach((difficulty, stats) -> stats.object2IntEntrySet().forEach(entry -> target.get(difficulty).addTo(entry.getKey(), entry.getIntValue())));
    }
}
