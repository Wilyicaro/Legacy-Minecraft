package wily.legacy.skins.pose;

import net.minecraft.util.Mth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class IdleSitTracker {
    private static final long IDLE_MS = 10_000L;
    private static final long FORGET_MS = 60_000L;

    private static final float LOOK_EPS = 0.25F;

    private static final class Entry {
        volatile long lastMoveMs;
        volatile long lastSeenMs;
        volatile boolean sitting;

        volatile boolean hasRot;
        volatile float lastYaw;
        volatile float lastPitch;
    }

    private static final Map<UUID, Entry> MAP = new ConcurrentHashMap<>();

    private IdleSitTracker() { }

    public static void reset(UUID uuid) {
        if (uuid == null) return;
        MAP.remove(uuid);
    }

    public static boolean isSitting(UUID uuid) {
        Entry e = uuid == null ? null : MAP.get(uuid);
        return e != null && e.sitting;
    }

    public static boolean updateAndShouldSit(UUID uuid, boolean movingNow) { return updateAndShouldSit(uuid, movingNow, Float.NaN, Float.NaN); }

    public static boolean updateAndShouldSit(UUID uuid, boolean movingNow, float yawDeg, float pitchDeg) {
        if (uuid == null) return false;
        long now = System.currentTimeMillis();
        Entry e = MAP.computeIfAbsent(uuid, u -> {
            Entry n = new Entry();
            n.lastMoveMs = now;
            n.lastSeenMs = now;
            n.sitting = false;
            n.hasRot = false;
            return n;
        });

        e.lastSeenMs = now;

        boolean lookMoved = false;
        if (!Float.isNaN(yawDeg) && !Float.isNaN(pitchDeg)) {
            if (e.hasRot) {
                float dyaw = Math.abs(Mth.wrapDegrees(yawDeg - e.lastYaw));
                float dpitch = Math.abs(pitchDeg - e.lastPitch);
                lookMoved = dyaw > LOOK_EPS || dpitch > LOOK_EPS;
            } else { e.hasRot = true; }
            e.lastYaw = yawDeg;
            e.lastPitch = pitchDeg;
        }

        if (movingNow || lookMoved) {
            e.lastMoveMs = now;
            e.sitting = false;
            return false;
        }

        if (!e.sitting && (now - e.lastMoveMs) >= IDLE_MS) { e.sitting = true; }

        if ((now & 255L) == 0L) prune(now);
        return e.sitting;
    }

    private static void prune(long now) {
        for (var it = MAP.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entry e = en.getValue();
            if (e == null) {
                it.remove();
                continue;
            }
            if ((now - e.lastSeenMs) > FORGET_MS) it.remove();
        }
    }
}
