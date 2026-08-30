package wily.legacy.client;

import wily.legacy.Legacy4JClient;

public final class ReplayCompat {
    private static volatile boolean rendering;

    public static boolean isRendering() {
        return rendering/*? if fabric && (1.21.1 || >=1.21.4) {*/ || wily.legacy.client.screen.compat.FlashbackCompat.isExporting()/*?}*/;
    }

    public static void setRendering(boolean rendering) {
        ReplayCompat.rendering = rendering;
        Legacy4JClient.controllerManager.updateCursorInputMode();
    }
}
