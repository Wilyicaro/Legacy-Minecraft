package wily.legacy.skins.client.gui;

import com.mojang.authlib.GameProfile;
//? if <1.20.2 {
/*import com.mojang.authlib.minecraft.MinecraftProfileTexture;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
//? if >1.20.1 {
import net.minecraft.client.resources.PlayerSkin;
//?}
import wily.legacy.skins.skin.ClientSkinCache;

import java.util.UUID;
//? if >1.20.1 {
import java.util.concurrent.CompletableFuture;
//?}

public final class GuiSessionSkin {
    private static volatile String cachedSessionProfileKey;
    private static volatile GameProfile cachedSessionProfile;
    private static volatile String cachedSessionSkinKey;
    private static volatile SessionSkin cachedSessionSkin;
    private static volatile String cachedLookupKey;
    //? if >1.20.1 {
    private static volatile CompletableFuture<SessionSkin> cachedLookup;
    //?}

    private GuiSessionSkin() {
    }

    public static void prewarm() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        requestSessionSkin(mc, getSessionProfile(mc));
    }

    public static SessionSkin getSessionSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        GameProfile profile = getSessionProfile(mc);
        requestSessionSkin(mc, profile);

        SessionSkin skin = getCachedSessionSkin(profile);
        if (skin != null) return skin;

        skin = getCompletedSessionSkin(profile);
        if (skin != null) return skin;

        skin = getLivePlayerSkin(mc, profile);
        if (skin != null) cacheSessionSkin(profile, skin);
        return skin;
    }

    public static void ensureSessionTexturesFetch(GameProfile profile) {
        requestSessionSkin(Minecraft.getInstance(), profile);
    }

    private static SessionSkin getCachedSessionSkin(GameProfile profile) {
        String key = getProfileKey(profile);
        SessionSkin skin = cachedSessionSkin;
        if (skin == null || key == null) return null;
        return key.equals(cachedSessionSkinKey) ? skin : null;
    }

    private static SessionSkin getCompletedSessionSkin(GameProfile profile) {
        //? if >1.20.1 {
        String key = getProfileKey(profile);
        CompletableFuture<SessionSkin> future = cachedLookup;
        if (key == null || future == null || !key.equals(cachedLookupKey)) return null;
        SessionSkin skin;
        try {
            skin = future.getNow(null);
        } catch (RuntimeException ignored) {
            return null;
        }
        if (skin == null) return null;
        cacheSessionSkin(profile, skin);
        return skin;
        //?} else {
        /*return null;
        *///?}
    }

    private static void cacheSessionSkin(GameProfile profile, SessionSkin skin) {
        String key = getProfileKey(profile);
        if (key == null || skin == null || skin.texture() == null) return;
        cachedSessionSkinKey = key;
        cachedSessionSkin = skin;
    }

    private static void requestSessionSkin(Minecraft mc, GameProfile profile) {
        if (mc == null || profile == null) return;
        String key = getProfileKey(profile);
        if (key == null) return;
        if (key.equals(cachedLookupKey)/*? if >1.20.1 {*/ && cachedLookup != null/*?} else {*//* && getCachedSessionSkin(profile) != null*//*?}*/) return;
        try {
            cachedLookupKey = key;
            //? if >1.20.1 {
            CompletableFuture<SessionSkin> requested = mc.getSkinManager().getOrLoad(profile).thenApply(GuiSessionSkin::toSessionSkinResult);
            cachedLookup = requested;
            requested.thenAccept(skin -> {
                if (key.equals(cachedLookupKey)) cacheSessionSkin(profile, skin);
            });
            //?} else {
            /*mc.getSkinManager().registerSkins(profile, (type, texture, profileTexture) -> {
                if (type != MinecraftProfileTexture.Type.SKIN || !key.equals(cachedLookupKey)) return;
                cacheSessionSkin(profile, toSessionSkin(texture, profileTexture));
            }, true);
            *///?}
        } catch (RuntimeException ignored) {
        }
    }

    private static SessionSkin getLivePlayerSkin(Minecraft mc, GameProfile profile) {
        if (mc == null || mc.player == null) return null;
        ClientSkinCache.pushBypassSkinOverride();
        try {
            ResourceLocation texture = mc.player./*? if >1.20.1 {*/getSkin().texture()/*?} else {*//*getSkinTextureLocation()*//*?}*/;
            if (texture == null) return null;
            boolean slim = /*? if >1.20.1 {*/"slim".equals(mc.player.getSkin().model().id())/*?} else {*//*"slim".equals(mc.player.getModelName())*//*?}*/;
            return new SessionSkin(texture, slim);
        } catch (RuntimeException ignored) {
            return null;
        } finally {
            ClientSkinCache.popBypassSkinOverride();
        }
    }

    //? if >1.20.1 {
    private static SessionSkin toSessionSkinResult(Object result) {
        if (result instanceof java.util.Optional<?> optional) {
            Object skin = optional.orElse(null);
            return skin instanceof PlayerSkin playerSkin ? toSessionSkin(playerSkin) : null;
        }
        return result instanceof PlayerSkin playerSkin ? toSessionSkin(playerSkin) : null;
    }

    private static SessionSkin toSessionSkin(PlayerSkin skin) {
        if (skin == null || skin.texture() == null) return null;
        return new SessionSkin(skin.texture(), "slim".equals(skin.model().id()));
    }
    //?} else {
    /*private static SessionSkin toSessionSkin(ResourceLocation texture, MinecraftProfileTexture profileTexture) {
        if (texture == null) return null;
        boolean slim = profileTexture != null && "slim".equals(profileTexture.getMetadata("model"));
        return new SessionSkin(texture, slim);
    }
    *///?}

    private static GameProfile getSessionProfile(Minecraft mc) {
        if (mc == null) return null;
        GameProfile profile = getGameProfile(mc);
        if (profile != null) {
            String key = getProfileKey(profile);
            if (key == null) return profile;
            GameProfile cached = cachedSessionProfile;
            if (key.equals(cachedSessionProfileKey) && cached != null) {
                if (!hasTexturesProperty(cached) && hasTexturesProperty(profile)) {
                    cachedSessionProfile = profile;
                    return profile;
                }
                return cached;
            }
            cacheProfile(key, profile);
            return profile;
        }
        if (mc.getUser() == null) return null;
        UUID id = mc.getUser().getProfileId();
        String name = mc.getUser().getName();
        if (id == null && (name == null || name.isBlank())) return null;
        String key = id != null ? String.valueOf(id) : name;
        GameProfile cached = cachedSessionProfile;
        if (key != null && key.equals(cachedSessionProfileKey) && cached != null) return cached;
        profile = new GameProfile(id, name);
        cacheProfile(key, profile);
        return profile;
    }

    private static GameProfile getGameProfile(Minecraft mc) {
        try {
            return /*? if >1.20.2 {*/mc.getGameProfile()/*?} else {*//*mc.getUser().getGameProfile()*//*?}*/;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void cacheProfile(String key, GameProfile profile) {
        cachedSessionProfileKey = key;
        cachedSessionProfile = profile;
        if (key == null || !key.equals(cachedSessionSkinKey)) cachedSessionSkin = null;
        if (key == null || !key.equals(cachedLookupKey)) {
            cachedLookupKey = null;
            //? if >1.20.1 {
            cachedLookup = null;
            //?}
        }
    }

    private static String getProfileKey(GameProfile profile) {
        if (profile == null) return null;
        UUID id = profile.getId();
        if (id != null) return String.valueOf(id);
        String name = profile.getName();
        return name == null || name.isBlank() ? null : name;
    }

    private static boolean hasTexturesProperty(GameProfile profile) {
        return profile != null && profile.getProperties() != null && !profile.getProperties().get("textures").isEmpty();
    }

    public record SessionSkin(ResourceLocation texture, boolean slim) {
    }
}
