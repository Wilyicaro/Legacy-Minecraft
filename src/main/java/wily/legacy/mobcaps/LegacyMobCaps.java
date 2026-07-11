package wily.legacy.mobcaps;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import wily.factoryapi.FactoryEvent;
import wily.legacy.init.LegacyGameRules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class LegacyMobCaps {
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final Map<ResourceKey<Level>, WorldMobCapTracker> TRACKERS = new HashMap<>();
    private static int refreshTicks;

    private LegacyMobCaps() {
    }

    public static void init() {
        FactoryEvent.afterServerTick(LegacyMobCaps::refreshTrackers);
        FactoryEvent.serverStopping(server -> {
            TRACKERS.clear();
            refreshTicks = 0;
        });
    }

    public static WorldMobCapTracker tracker(ServerLevel level) {
        return TRACKERS.computeIfAbsent(level.dimension(), ignored -> WorldMobCapTracker.scan(level));
    }

    public static boolean isEnabled(ServerLevel level) {
        return level.getGameRules().get(LegacyGameRules.LEGACY_MOBCAP_LIMITS.get());
    }

    public static void handleEntityAdded(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        WorldMobCapTracker tracker = TRACKERS.get(level.dimension());
        if (tracker == null) {
            return;
        }
        tracker.track(entity, 1);
    }

    public static void handleEntityRemoved(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        WorldMobCapTracker tracker = TRACKERS.get(level.dimension());
        if (tracker == null) {
            return;
        }

        tracker.track(entity, -1);
    }

    private static void refreshTrackers(MinecraftServer server) {
        if (++refreshTicks < REFRESH_INTERVAL_TICKS) {
            return;
        }
        refreshTicks = 0;

        Iterator<Map.Entry<ResourceKey<Level>, WorldMobCapTracker>> iterator = TRACKERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, WorldMobCapTracker> entry = iterator.next();
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null || !isEnabled(level)) {
                iterator.remove();
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (!isEnabled(level)) {
                continue;
            }
            TRACKERS.computeIfAbsent(level.dimension(), ignored -> WorldMobCapTracker.scan(level));
        }
    }
}
