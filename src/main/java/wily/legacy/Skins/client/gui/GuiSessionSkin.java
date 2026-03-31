package wily.legacy.Skins.client.gui;

import com.mojang.authlib.GameProfile;
import wily.legacy.Skins.skin.ClientSkinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GuiSessionSkin {
    private static volatile String cachedSessionProfileKey;
    private static volatile GameProfile cachedSessionProfile;
    private static volatile String cachedSessionSkinKey;
    private static volatile PlayerSkin cachedSessionSkin;
    private static volatile String cachedLookupKey;
    private static volatile CompletableFuture<Optional<PlayerSkin>> cachedLookup;

    private GuiSessionSkin() { }

    public static void prewarm() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        GameProfile profile = getSessionProfile(mc);
        requestSessionSkin(mc, profile);
    }

    public static PlayerSkin getSessionPlayerSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        GameProfile profile = getSessionProfile(mc);
        requestSessionSkin(mc, profile);

        PlayerSkin skin = getCachedSessionSkin(profile);
        if (skin != null) return skin;

        skin = getCompletedSessionSkin(profile);
        if (skin != null) return skin;

        if (mc.player != null) {
            ClientSkinCache.pushBypassSkinOverride();
            try {
                PlayerSkin liveSkin = mc.player.getSkin();
                if (!isDefaultSkin(liveSkin)) {
                    cacheSessionSkin(profile, liveSkin);
                    return liveSkin;
                }
            } finally { ClientSkinCache.popBypassSkinOverride(); }
        }

        return null;
    }

    public static void ensureSessionTexturesFetch(GameProfile profile) {
        requestSessionSkin(Minecraft.getInstance(), profile);
    }

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
            cachedSessionProfileKey = key;
            cachedSessionProfile = profile;
            if (!key.equals(cachedSessionSkinKey)) cachedSessionSkin = null;
            if (!key.equals(cachedLookupKey)) cachedLookup = null;
            return profile;
        }
        var user = mc.getUser();
        if (user == null) return null;
        UUID id = user.getProfileId();
        String name = user.getName();
        if (id == null && (name == null || name.isBlank())) return null;
        String key = id != null ? String.valueOf(id) : name;
        GameProfile cached = cachedSessionProfile;
        if (key != null && key.equals(cachedSessionProfileKey) && cached != null) return cached;
        profile = new GameProfile(id, name);
        cachedSessionProfileKey = key;
        cachedSessionProfile = profile;
        if (!key.equals(cachedSessionSkinKey)) cachedSessionSkin = null;
        if (!key.equals(cachedLookupKey)) cachedLookup = null;
        return profile;
    }

    private static PlayerSkin getCachedSessionSkin(GameProfile profile) {
        String key = getProfileKey(profile);
        PlayerSkin skin = cachedSessionSkin;
        if (skin == null || key == null) return null;
        return key.equals(cachedSessionSkinKey) ? skin : null;
    }

    private static PlayerSkin getCompletedSessionSkin(GameProfile profile) {
        String key = getProfileKey(profile);
        CompletableFuture<Optional<PlayerSkin>> future = cachedLookup;
        if (key == null || future == null || !key.equals(cachedLookupKey)) return null;
        Optional<PlayerSkin> result;
        try {
            result = future.getNow(Optional.empty());
        } catch (RuntimeException ignored) {
            return null;
        }
        if (result.isEmpty()) return null;
        PlayerSkin skin = result.get();
        cacheSessionSkin(profile, skin);
        return skin;
    }

    private static void cacheSessionSkin(GameProfile profile, PlayerSkin skin) {
        String key = getProfileKey(profile);
        if (key == null || skin == null) return;
        cachedSessionSkinKey = key;
        cachedSessionSkin = skin;
    }

    private static void requestSessionSkin(Minecraft mc, GameProfile profile) {
        if (mc == null || profile == null) return;
        String key = getProfileKey(profile);
        if (key == null) return;
        CompletableFuture<Optional<PlayerSkin>> future = cachedLookup;
        if (key.equals(cachedLookupKey) && future != null) return;
        try {
            CompletableFuture<Optional<PlayerSkin>> requested = mc.getSkinManager().get(profile);
            cachedLookupKey = key;
            cachedLookup = requested;
            requested.thenAccept(result -> {
                if (!key.equals(cachedLookupKey) || result == null || result.isEmpty()) return;
                cacheSessionSkin(profile, result.get());
            });
        } catch (RuntimeException ignored) { }
    }

    private static boolean isDefaultSkin(PlayerSkin skin) {
        if (skin == null || skin.body() == null) return true;
        ResourceLocation texture = skin.body().texturePath();
        if (texture == null) return true;
        return "minecraft".equals(texture.getNamespace()) && texture.getPath().startsWith("textures/entity/player/");
    }

    private static GameProfile getGameProfile(Minecraft mc) {
        try {
            return mc.getGameProfile();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String getProfileKey(GameProfile profile) {
        if (profile == null) return null;
        UUID id = profile.id();
        if (id != null) return String.valueOf(id);
        String name = profile.name();
        if (name != null && !name.isBlank()) return name;
        return null;
    }

    private static boolean hasTexturesProperty(GameProfile profile) {
        return profile != null && !profile.properties().get("textures").isEmpty();
    }
}
