package wily.legacy.client;

import wily.legacy.Legacy4JClient;

public final class ReplayCompat {
    private static volatile boolean rendering;

    public static boolean isRendering() {
        return rendering/*? if fabric && >=26.1 {*/ || wily.legacy.client.screen.compat.FlashbackCompat.isExporting()/*?}*/;
    }

    public static void setRendering(boolean rendering) {
        ReplayCompat.rendering = rendering;
        Legacy4JClient.controllerManager.updateCursorInputMode();
    }
}
