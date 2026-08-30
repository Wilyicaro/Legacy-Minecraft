package wily.legacy.client;

import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.compat.FlashbackCompat;

public final class ReplayCompat {
    private static volatile boolean rendering;

    public static boolean isRendering() {
        return rendering || FlashbackCompat.isExporting();
    }

    public static void setRendering(boolean rendering) {
        ReplayCompat.rendering = rendering;
        Legacy4JClient.controllerManager.updateCursorInputMode();
    }
}
