package wily.legacy.mobcaps;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class WorldMobCapTracker {
    private final EnumMap<TrackedMobCap, Integer> bucketCounts = new EnumMap<>(TrackedMobCap.class);
    private final Map<EntityType<?>, Integer> typeCounts = new IdentityHashMap<>();

    private WorldMobCapTracker() {
        for (TrackedMobCap cap : TrackedMobCap.values()) {
            bucketCounts.put(cap, 0);
        }
    }

    static WorldMobCapTracker scan(ServerLevel level) {
        WorldMobCapTracker tracker = new WorldMobCapTracker();
        for (Entity entity : level.getAllEntities()) {
            tracker.track(entity, 1);
        }
        return tracker;
    }

    void track(Entity entity, int delta) {
        updateTypeCount(entity.getType(), delta);

        TrackedMobCap bucket = ConsoleMobCaps.bucketForEntity(entity);
        if (bucket != null) {
            updateBucketCount(bucket, delta);
        }
    }

    int count(TrackedMobCap cap) {
        return bucketCounts.getOrDefault(cap, 0);
    }

    int count(EntityType<?> type) {
        return typeCounts.getOrDefault(type, 0);
    }

    private void updateTypeCount(EntityType<?> type, int delta) {
        int updated = typeCounts.getOrDefault(type, 0) + delta;
        if (updated <= 0) {
            typeCounts.remove(type);
            return;
        }

        typeCounts.put(type, updated);
    }

    private void updateBucketCount(TrackedMobCap cap, int delta) {
        int updated = bucketCounts.getOrDefault(cap, 0) + delta;
        bucketCounts.put(cap, Math.max(updated, 0));
    }
}
