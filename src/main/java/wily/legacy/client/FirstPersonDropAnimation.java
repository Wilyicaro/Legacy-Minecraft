package wily.legacy.client;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public class FirstPersonDropAnimation {
    private static final long DURATION = 450L;
    private static long startTime;

    public static void start() {
        startTime = Util.getMillis();
    }

    public static boolean isActive() {
        return Util.getMillis() - startTime < DURATION;
    }

    public static float progress() {
        return Mth.clamp((Util.getMillis() - startTime) / (float) DURATION, 0.0f, 1.0f);
    }
}
