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
    private static volatile java.lang.reflect.Method cachedTextureRelease;
    private static volatile boolean triedResolveTextureRelease;

    private ClientSkinAssets() {
    }

    public static ResourceLocation getTexture(String skinId) {
        return skinId == null ? null : TEXTURES.get(skinId);
    }

    public static JsonObject getModelJson(String skinId) {
        return skinId == null ? null : MODELS.get(skinId);
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

        if (texturePng != null && texturePng.length > 0) {
            try {
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
        }

        if (modelJson != null && modelJson.length > 0) {
            try {
                String s = new String(modelJson, java.nio.charset.StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(s).getAsJsonObject();
                MODELS.put(skinId, obj);
                registerRuntimePoses(skinId, obj);
            } catch (Throwable ignored) {
            }
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
        BoxModelManager.clearRuntime();
        SkinPoseRegistry.clearRuntimeSelectors();
    }
}
