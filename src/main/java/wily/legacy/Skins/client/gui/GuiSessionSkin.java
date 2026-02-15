package wily.legacy.Skins.client.gui;

import com.mojang.authlib.GameProfile;
import wily.legacy.Skins.skin.ClientSkinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.concurrent.ConcurrentHashMap;

public final class GuiSessionSkin {
    private static final ConcurrentHashMap<String, Boolean> FETCHING = new ConcurrentHashMap<>();

    private GuiSessionSkin() {
    }

    public static PlayerSkin getSessionPlayerSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            ClientSkinCache.pushBypassSkinOverride();
            try {
                return mc.player.getSkin();
            } finally {
                ClientSkinCache.popBypassSkinOverride();
            }
        }
        return null;
    }

    public static void ensureSessionTexturesFetch(GameProfile profile) {
        try {
            if (profile == null) return;
            String key = getProfileKey(profile);
            if (key == null) return;
            if (FETCHING.putIfAbsent(key, Boolean.TRUE) != null) return;
            if (ProfilePropertyUtil.hasTexturesProperty(profile)) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            Object skinManager;
            try {
                skinManager = mc.getClass().getMethod("getSkinManager").invoke(mc);
            } catch (Throwable t) {
                return;
            }
            Thread t = new Thread(() -> {
                try {


                    try {
                        skinManager.getClass().getMethod("getInsecureSkinInformation", GameProfile.class).invoke(skinManager, profile);
                    } catch (Throwable ignored) {
                    }
                } finally {

                    FETCHING.remove(key);
                }
            }, "ConsoleSkins-SessionTextures");
            t.setDaemon(true);
            t.start();
        } catch (Throwable ignored) {
        }
    }

    private static String getProfileKey(GameProfile profile) {
        if (profile == null) return null;
        try {
            var u = ProfilePropertyUtil.getProfileUUID(profile);
            if (u != null) return String.valueOf(u);
        } catch (Throwable ignored) {
        }
        try {
            String n = ProfilePropertyUtil.getProfileName(profile);
            if (n != null && !n.isBlank()) return n;
        } catch (Throwable ignored) {
        }
        return null;
    }
}
