package wily.legacy.Skins.api;

import wily.legacy.Skins.skin.SkinSync;
import wily.legacy.Skins.util.DebugLog;

public final class ConsoleSkinsApi {

    private ConsoleSkinsApi() {
    }

    public static String modId() {
        return SkinSync.MODID;
    }

    public static boolean requestClientSkinChange(String skinId) {
        try {
            Class<?> mcCls = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcCls.getMethod("getInstance").invoke(null);
            if (mc == null) return false;

            Object player = mcCls.getField("player").get(mc);
            if (player == null) return false;

            Class<?> playerCls = player.getClass();
            Object uuid = playerCls.getMethod("getUUID").invoke(player);

            Class<?> syncCls = Class.forName("wily.legacy.Skins.skin.SkinSyncClient");
            syncCls.getMethod("requestSetSkin", uuid.getClass(), String.class).invoke(null, uuid, skinId);
            return true;
        } catch (Throwable t) {
            DebugLog.warn("Skin change request failed: {}", t.toString());
            return false;
        }
    }
}
