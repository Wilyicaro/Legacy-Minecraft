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
    private static final Map<ResourceKey<Level>, WorldMobCapTracker> TRACKERS = new HashMap<>();

    private LegacyMobCaps() {
    }

    public static void init() {
        FactoryEvent.afterServerTick(LegacyMobCaps::refreshTrackers);
        FactoryEvent.serverStopping(server -> TRACKERS.clear());
    }

    public static WorldMobCapTracker tracker(ServerLevel level) {
        return TRACKERS.computeIfAbsent(level.dimension(), ignored -> WorldMobCapTracker.scan(level));
    }

    public static boolean isEnabled(ServerLevel level) {
        return level.getGameRules().getBoolean(LegacyGameRules.LCE_MOBCAP_LIMITS);
    }

    public static void handleEntityAdded(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !isEnabled(level)) {
            return;
        }

        WorldMobCapTracker tracker = tracker(level);
        entity.getSelfAndPassengers().forEach(tracked -> tracker.track(tracked, 1));
    }

    public static void handleEntityRemoved(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        WorldMobCapTracker tracker = TRACKERS.get(level.dimension());
        if (tracker == null) {
            return;
        }

        entity.getSelfAndPassengers().forEach(tracked -> tracker.track(tracked, -1));
    }

    private static void refreshTrackers(MinecraftServer server) {
        Iterator<Map.Entry<ResourceKey<Level>, WorldMobCapTracker>> iterator = TRACKERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, WorldMobCapTracker> entry = iterator.next();
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null || !isEnabled(level)) {
                iterator.remove();
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (isEnabled(level)) {
                TRACKERS.put(level.dimension(), WorldMobCapTracker.scan(level));
            }
        }
    }
}
