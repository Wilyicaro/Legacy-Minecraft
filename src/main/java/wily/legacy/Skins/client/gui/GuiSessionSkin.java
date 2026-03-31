package wily.legacy.Skins.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import wily.legacy.Skins.skin.ClientSkinCache;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiSessionSkin {
    private static final Set<String> FETCHING = ConcurrentHashMap.newKeySet();
    private static final Method PROPERTIES_METHOD = findMethod("getProperties", "properties");
    private static final Method NAME_METHOD = findMethod("getName", "name");
    private static final Method UUID_METHOD = findMethod("getId", "id");
    private static volatile String cachedSessionProfileKey;
    private static volatile GameProfile cachedSessionProfile;

    private GuiSessionSkin() { }

    public static PlayerSkin getSessionPlayerSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        if (mc.player != null) {
            ClientSkinCache.pushBypassSkinOverride();
            try {
                return mc.player.getSkin();
            } finally { ClientSkinCache.popBypassSkinOverride(); }
        }
        GameProfile profile = getSessionProfile(mc);
        if (profile == null) return null;
        ensureSessionTexturesFetch(profile);
        try {
            return mc.getSkinManager().createLookup(profile, true).get();
        } catch (RuntimeException ignored) { return null; }
    }

    public static void ensureSessionTexturesFetch(GameProfile profile) {
        if (profile == null) return;
        String key = getProfileKey(profile);
        if (key == null || !FETCHING.add(key)) return;
        if (hasTexturesProperty(profile)) {
            FETCHING.remove(key);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            FETCHING.remove(key);
            return;
        }
        try {
            mc.getSkinManager().get(profile).whenComplete((ignored, err) -> FETCHING.remove(key));
        } catch (RuntimeException ignored) { FETCHING.remove(key); }
    }

    private static GameProfile getSessionProfile(Minecraft mc) {
        if (mc == null) return null;
        GameProfile profile = getGameProfile(mc);
        if (profile != null) return profile;
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
        return profile;
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
        UUID id = getProfileUUID(profile);
        if (id != null) return String.valueOf(id);
        String name = getProfileName(profile);
        if (name != null && !name.isBlank()) return name;
        return null;
    }

    private static Method findMethod(String... names) {
        for (String name : names) {
            try {
                return GameProfile.class.getMethod(name);
            } catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }

    private static Object invoke(Method method, GameProfile profile) {
        if (method == null || profile == null) return null;
        try {
            return method.invoke(profile);
        } catch (ReflectiveOperationException ignored) { return null; }
    }

    private static boolean hasTexturesProperty(GameProfile profile) {
        Object value = invoke(PROPERTIES_METHOD, profile);
        return value instanceof PropertyMap map && !map.get("textures").isEmpty();
    }

    private static String getProfileName(GameProfile profile) {
        Object value = invoke(NAME_METHOD, profile);
        if (value instanceof String name && !name.isBlank()) return name;
        return "Player";
    }

    private static UUID getProfileUUID(GameProfile profile) {
        Object value = invoke(UUID_METHOD, profile);
        return value instanceof UUID id ? id : null;
    }
}
