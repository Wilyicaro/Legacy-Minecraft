package wily.legacy.Skins.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import wily.legacy.Skins.client.render.SkinPoseRegistry;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientSkinAssets {
    private static final Map<String, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> MODELS = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> TEXTURE_BYTES = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> MODEL_BYTES = new ConcurrentHashMap<>();
    private static volatile java.lang.reflect.Method cachedTextureRelease;
    private static volatile boolean triedResolveTextureRelease;

    /**
     * Cached PlayerSkin objects keyed by skinId, so we don't allocate new
     * ClientAsset.ResourceTexture + PlayerSkin objects every single frame per player.
     * Invalidated when textures/models change for a given skinId.
     */
    private static final Map<String, net.minecraft.world.entity.player.PlayerSkin> SKIN_CACHE = new ConcurrentHashMap<>();
    /** Cached PlayerSkin variants for skins with capes. Key = skinId + "|cape" */
    private static final Map<String, net.minecraft.world.entity.player.PlayerSkin> SKIN_CACHE_CAPE = new ConcurrentHashMap<>();

    private ClientSkinAssets() {
    }

    public static ResourceLocation getTexture(String skinId) {
        return skinId == null ? null : TEXTURES.get(skinId);
    }

    public static JsonObject getModelJson(String skinId) {
        return skinId == null ? null : MODELS.get(skinId);
    }

    public static byte[] getTextureBytes(String skinId) {
        return skinId == null ? null : TEXTURE_BYTES.get(skinId);
    }

    public static byte[] getModelBytes(String skinId) {
        return skinId == null ? null : MODEL_BYTES.get(skinId);
    }

    public static Boolean getSlimFlag(String skinId) {
        if (skinId == null || skinId.isBlank()) return null;
        JsonObject obj = MODELS.get(skinId);
        if (obj == null) return null;

        Boolean b = getSlimFlagFrom(obj);
        if (b != null) return b;

        if (obj.has("meta") && obj.get("meta").isJsonObject()) {
            JsonObject meta = obj.getAsJsonObject("meta");
            b = getSlimFlagFrom(meta);
            if (b != null) return b;
        }

        return null;
    }

    private static Boolean getSlimFlagFrom(JsonObject obj) {
        if (obj == null) return null;

        if (obj.has("slim") && obj.get("slim").isJsonPrimitive()) {
            try { return obj.get("slim").getAsBoolean(); } catch (Throwable ignored) {}
        }

        String modelStr = null;
        if (obj.has("model") && obj.get("model").isJsonPrimitive()) {
            try { modelStr = obj.get("model").getAsString(); } catch (Throwable ignored) {}
        } else if (obj.has("arms") && obj.get("arms").isJsonPrimitive()) {
            try { modelStr = obj.get("arms").getAsString(); } catch (Throwable ignored) {}
        }

        if (modelStr != null) {
            String m = modelStr.trim().toLowerCase(java.util.Locale.ROOT);
            if (m.equals("slim") || m.equals("alex")) return true;
            if (m.equals("wide") || m.equals("default") || m.equals("steve")) return false;
        }

        return null;
    }

    /**
     * Returns a cached PlayerSkin for the given skinId + cape combination,
     * avoiding per-frame allocation of ClientAsset.ResourceTexture and PlayerSkin objects.
     * Returns null if no texture is available for this skinId.
     *
     * @param skinId   the skin identifier
     * @param entry    the SkinEntry (may be null for runtime skins)
     * @param wantCape whether to include the cape texture
     */
    public static net.minecraft.world.entity.player.PlayerSkin getCachedPlayerSkin(
            String skinId, SkinEntry entry, boolean wantCape) {
        if (skinId == null || skinId.isBlank()) return null;

        // Choose cache based on whether cape is requested
        Map<String, net.minecraft.world.entity.player.PlayerSkin> cache = wantCape ? SKIN_CACHE_CAPE : SKIN_CACHE;
        net.minecraft.world.entity.player.PlayerSkin cached = cache.get(skinId);
        if (cached != null) return cached;

        // Build the skin
        ResourceLocation tex = TEXTURES.get(skinId);
        if (tex == null && entry != null) tex = entry.texture();
        if (tex == null) return null;

        net.minecraft.core.ClientAsset.ResourceTexture body =
                new net.minecraft.core.ClientAsset.ResourceTexture(tex, tex);

        net.minecraft.core.ClientAsset.Texture capeFinal = body;
        if (wantCape && entry != null && entry.cape() != null) {
            ResourceLocation capePath = entry.cape();
            capeFinal = new net.minecraft.core.ClientAsset.ResourceTexture(capePath, capePath);
        }

        Boolean slim = getSlimFlag(skinId);
        net.minecraft.world.entity.player.PlayerModelType model = slim != null
                ? (slim ? net.minecraft.world.entity.player.PlayerModelType.SLIM
                        : net.minecraft.world.entity.player.PlayerModelType.WIDE)
                : ((entry != null && entry.slimArms())
                        ? net.minecraft.world.entity.player.PlayerModelType.SLIM
                        : net.minecraft.world.entity.player.PlayerModelType.WIDE);

        net.minecraft.world.entity.player.PlayerSkin skin =
                net.minecraft.world.entity.player.PlayerSkin.insecure(body, capeFinal, body, model);
        cache.put(skinId, skin);
        return skin;
    }

    /**
     * Invalidates the PlayerSkin cache for the given skinId.
     * Called when textures or models change.
     */
    private static void invalidateSkinCache(String skinId) {
        if (skinId == null) return;
        SKIN_CACHE.remove(skinId);
        SKIN_CACHE_CAPE.remove(skinId);
    }

    /**
     * Resolves a model ResourceLocation from a texture ResourceLocation.
     * Cached via a static map to avoid per-frame string operations.
     */
    private static final Map<ResourceLocation, ResourceLocation> MODEL_ID_CACHE = new ConcurrentHashMap<>();

    public static ResourceLocation getModelIdFromTexture(ResourceLocation texture) {
        if (texture == null) return null;
        return MODEL_ID_CACHE.computeIfAbsent(texture, tex -> {
            String p = tex.getPath();
            int slash = p.lastIndexOf('/');
            if (slash != -1) p = p.substring(slash + 1);
            if (p.endsWith(".png")) p = p.substring(0, p.length() - 4);
            return ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), p);
        });
    }

    private static void registerRuntimePoses(String skinId, JsonObject obj) {
        if (skinId == null || skinId.isBlank() || obj == null) return;

        JsonElement posesEl = null;
        if (obj.has("poses")) posesEl = obj.get("poses");
        else if (obj.has("pose_tags")) posesEl = obj.get("pose_tags");
        else if (obj.has("animations")) posesEl = obj.get("animations");
        else if (obj.has("meta") && obj.get("meta").isJsonObject()) {
            JsonObject meta = obj.getAsJsonObject("meta");
            if (meta.has("poses")) posesEl = meta.get("poses");
            else if (meta.has("pose_tags")) posesEl = meta.get("pose_tags");
            else if (meta.has("animations")) posesEl = meta.get("animations");
        }
        if (posesEl == null) return;

        if (posesEl.isJsonArray()) {
            JsonArray arr = posesEl.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement el = arr.get(i);
                if (el != null && el.isJsonPrimitive()) {
                    SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(el.getAsString());
                    if (tag != null) SkinPoseRegistry.addRuntimeSelector(tag, skinId);
                }
            }
        } else if (posesEl.isJsonPrimitive()) {
            SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(posesEl.getAsString());
            if (tag != null) SkinPoseRegistry.addRuntimeSelector(tag, skinId);
        }
    }

    public static void put(String skinId, byte[] texturePng, byte[] modelJson) {
        if (skinId == null || skinId.isBlank()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        // Invalidate the cached PlayerSkin whenever assets change
        invalidateSkinCache(skinId);

        if (texturePng != null && texturePng.length > 0) {
            try {
                TEXTURE_BYTES.put(skinId, texturePng);
                NativeImage img = NativeImage.read(new ByteArrayInputStream(texturePng));
                DynamicTexture dt = new DynamicTexture(() -> "legacy_runtime_skin_" + skinId, img);
                TextureManager tm = mc.getTextureManager();
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("legacy", "runtime_skins/" + skinId);
                ResourceLocation old = TEXTURES.get(skinId);
                if (old != null && !old.equals(rl)) releaseTexture(tm, old);
                tm.register(rl, (AbstractTexture) dt);
                TEXTURES.put(skinId, rl);
            } catch (Throwable ignored) {
            }
        } else {
            TEXTURE_BYTES.remove(skinId);
        }

        if (modelJson != null && modelJson.length > 0) {
            try {
                MODEL_BYTES.put(skinId, modelJson);
                String s = new String(modelJson, java.nio.charset.StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(s).getAsJsonObject();
                MODELS.put(skinId, obj);
                registerRuntimePoses(skinId, obj);
            } catch (Throwable ignored) {
            }
        } else {
            MODEL_BYTES.remove(skinId);
        }
    }

    private static void releaseTexture(TextureManager tm, ResourceLocation rl) {
        if (tm == null || rl == null) return;
        if (!triedResolveTextureRelease) {
            synchronized (ClientSkinAssets.class) {
                if (!triedResolveTextureRelease) {
                    triedResolveTextureRelease = true;
                    cachedTextureRelease = null;
                    try {
                        java.lang.reflect.Method m = tm.getClass().getMethod("release", ResourceLocation.class);
                        cachedTextureRelease = m;
                    } catch (Throwable ignored) {
                        try {
                            java.lang.reflect.Method m = tm.getClass().getMethod("unregister", ResourceLocation.class);
                            cachedTextureRelease = m;
                        } catch (Throwable ignored2) {
                            try {
                                for (java.lang.reflect.Method m : tm.getClass().getMethods()) {
                                    if (m.getParameterCount() != 1) continue;
                                    if (m.getParameterTypes()[0] != ResourceLocation.class) continue;
                                    if (m.getReturnType() != void.class) continue;
                                    String n = m.getName().toLowerCase(java.util.Locale.ROOT);
                                    if (!(n.contains("release") || n.contains("unregister") || n.contains("remove") || n.contains("close"))) continue;
                                    cachedTextureRelease = m;
                                    break;
                                }
                            } catch (Throwable ignored3) {
                            }
                        }
                    }
                }
            }
        }

        if (cachedTextureRelease == null) return;
        try {
            cachedTextureRelease.invoke(tm, rl);
        } catch (Throwable ignored) {
        }
    }

    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc != null ? mc.getTextureManager() : null;
        for (ResourceLocation rl : TEXTURES.values()) {
            releaseTexture(tm, rl);
        }
        TEXTURES.clear();
        MODELS.clear();
        TEXTURE_BYTES.clear();
        MODEL_BYTES.clear();
        SKIN_CACHE.clear();
        SKIN_CACHE_CAPE.clear();
        MODEL_ID_CACHE.clear();
        BoxModelManager.clearRuntime();
        SkinPoseRegistry.clearRuntimeSelectors();
    }
}
