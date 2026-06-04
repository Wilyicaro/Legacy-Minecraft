package wily.legacy.client;

import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;

public final class LegacyInteractionAnimations {
    private static final long SERVER_SWING_WINDOW = 750L;
    private static long mainHandSwingUntil;
    private static long offHandSwingUntil;

    private LegacyInteractionAnimations() {
    }

    public static void suppressServerSwing(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) mainHandSwingUntil = Util.getMillis() + SERVER_SWING_WINDOW;
        else offHandSwingUntil = Util.getMillis() + SERVER_SWING_WINDOW;
    }

    public static boolean consumeServerSwing(InteractionHand hand) {
        if (hand == null) return false;
        long now = Util.getMillis();
        if (hand == InteractionHand.MAIN_HAND) {
            if (mainHandSwingUntil < now) return false;
            mainHandSwingUntil = 0L;
            return true;
        }
        if (offHandSwingUntil < now) return false;
        offHandSwingUntil = 0L;
        return true;
    }
}
