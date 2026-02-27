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
        if (mc == null) return null;
        if (mc.player != null) {
            ClientSkinCache.pushBypassSkinOverride();
            try {
                return mc.player.getSkin();
            } finally {
                ClientSkinCache.popBypassSkinOverride();
            }
        }
        GameProfile profile = null;
        try {
            profile = mc.getGameProfile();
        } catch (Throwable ignored) {
        }
        if (profile == null) {
            try {
                Object user = mc.getUser();
                if (user != null) {
                    try {
                        java.util.UUID id = null;
                        try {
                            Object o = user.getClass().getMethod("getProfileId").invoke(user);
                            if (o instanceof java.util.UUID) id = (java.util.UUID) o;
                        } catch (Throwable ignored2) {
                        }
                        String name = null;
                        try {
                            Object o = user.getClass().getMethod("getName").invoke(user);
                            if (o != null) name = String.valueOf(o);
                        } catch (Throwable ignored2) {
                        }
                        if (id != null || (name != null && !name.isBlank())) profile = new GameProfile(id, name);
                    } catch (Throwable ignored2) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (profile == null) return null;
        ensureSessionTexturesFetch(profile);
        try {
            return mc.getSkinManager().createLookup(profile, true).get();
        } catch (Throwable ignored) {
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
