package wily.legacy.Skins.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientSkinAssets {
    public record ResolvedSkin(SkinEntry entry, ResourceLocation texture, ResourceLocation boxTexture, ResourceLocation modelId, BuiltBoxModel boxModel) {}
    private static final Map<String, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> MODELS = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> TEXTURE_BYTES = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> MODEL_BYTES = new ConcurrentHashMap<>();
    private static final Map<String, PlayerSkin> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, PlayerSkin> SKIN_CACHE_CAPE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> MODEL_ID_CACHE = new ConcurrentHashMap<>();
    private ClientSkinAssets() { }
    public static ResourceLocation getTexture(String skinId) { return skinId == null ? null : TEXTURES.get(skinId); }
    public static JsonObject getModelJson(String skinId) { return skinId == null ? null : MODELS.get(skinId); }
    public static byte[] getTextureBytes(String skinId) { return skinId == null ? null : TEXTURE_BYTES.get(skinId); }
    public static byte[] getModelBytes(String skinId) { return skinId == null ? null : MODEL_BYTES.get(skinId); }
    public static ResolvedSkin resolveSkin(String skinId) { return resolveSkin(skinId, null, null, null); }
    public static ResolvedSkin resolveSkin(String skinId, ResourceLocation texture, ResourceLocation modelId, BuiltBoxModel boxModel) {
        boolean hasSkinId = skinId != null && !skinId.isBlank();
        SkinEntry entry = hasSkinId ? SkinPackLoader.getSkin(skinId) : null;
        if (texture == null && hasSkinId) { texture = resolveTexture(skinId, entry); }
        if (modelId == null && texture != null) { modelId = getModelIdFromTexture(texture); }
        if (boxModel == null && modelId != null) { boxModel = resolveBoxModel(skinId, modelId); }
        ResourceLocation boxTexture = modelId == null ? null : BoxModelManager.getTexture(modelId);
        if (boxTexture == null) boxTexture = texture;
        if (entry == null && texture == null && boxTexture == null && modelId == null && boxModel == null) { return null; }
        return new ResolvedSkin(entry, texture, boxTexture, modelId, boxModel);
    }
    public static ResourceLocation resolveTexture(String skinId, SkinEntry entry) {
        if (skinId == null || skinId.isBlank()) return null;
        ResourceLocation texture = TEXTURES.get(skinId);
        return texture != null ? texture : entry != null ? entry.texture() : null;
    }
    public static BuiltBoxModel resolveBoxModel(String skinId, ResourceLocation modelId) {
        if (modelId == null) return null;
        BuiltBoxModel model = BoxModelManager.get(modelId);
        if (model != null || skinId == null || skinId.isBlank()) return model;
        JsonObject json = MODELS.get(skinId);
        if (json == null) return null;
        BoxModelManager.registerRuntime(modelId, json);
        return BoxModelManager.get(modelId);
    }
    public static Boolean getSlimFlag(String skinId) {
        if (skinId == null || skinId.isBlank()) return null;
        JsonObject obj = MODELS.get(skinId);
        if (obj == null) return null;
        Boolean slim = getSlimFlagFrom(obj);
        return slim != null ? slim : getSlimFlagFrom(getObject(obj, "meta"));
    }
    private static Boolean getSlimFlagFrom(JsonObject obj) {
        if (obj == null) return null;
        Boolean slim = getBoolean(obj, "slim");
        if (slim != null) return slim;
        String model = getString(obj, "model");
        if (model == null) { model = getString(obj, "arms"); }
        if (model == null) return null;
        String key = model.trim().toLowerCase(java.util.Locale.ROOT);
        if (key.equals("slim") || key.equals("alex")) return true;
        if (key.equals("wide") || key.equals("default") || key.equals("steve")) return false;
        return null;
    }
    public static PlayerSkin getCachedPlayerSkin(String skinId, SkinEntry entry, boolean wantCape) {
        if (skinId == null || skinId.isBlank()) return null;
        Map<String, PlayerSkin> cache = wantCape ? SKIN_CACHE_CAPE : SKIN_CACHE;
        PlayerSkin cached = cache.get(skinId);
        if (cached != null) return cached;
        ResourceLocation texture = resolveTexture(skinId, entry);
        if (texture == null) return null;
        net.minecraft.core.ClientAsset.ResourceTexture body = new net.minecraft.core.ClientAsset.ResourceTexture(texture, texture);
        net.minecraft.core.ClientAsset.Texture cape = null;
        if (wantCape && entry != null && entry.cape() != null) {
            ResourceLocation capePath = entry.cape();
            cape = new net.minecraft.core.ClientAsset.ResourceTexture(capePath, capePath);
        }
        PlayerSkin skin = PlayerSkin.insecure(body, cape, null, resolveModelType(skinId, entry));
        cache.put(skinId, skin);
        return skin;
    }
    public static ResourceLocation getModelIdFromTexture(ResourceLocation texture) {
        if (texture == null) return null;
        return MODEL_ID_CACHE.computeIfAbsent(texture, value -> {
            String path = value.getPath();
            int slash = path.lastIndexOf('/');
            if (slash != -1) { path = path.substring(slash + 1); }
            if (path.endsWith(".png")) { path = path.substring(0, path.length() - 4); }
            return ResourceLocation.fromNamespaceAndPath(value.getNamespace(), path);
        });
    }
    public static void putTexture(String skinId, byte[] texturePng) {
        if (skinId == null || skinId.isBlank()) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        invalidateSkinCache(skinId);
        if (texturePng == null) return;
        if (texturePng.length == 0) {
            clearTexture(skinId, client.getTextureManager());
            return;
        }
        TEXTURE_BYTES.put(skinId, texturePng);
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(texturePng));
            DynamicTexture texture = new DynamicTexture(() -> "legacy_runtime_skin_" + skinId, image);
            TextureManager manager = client.getTextureManager();
            ResourceLocation id = runtimeTextureId(skinId);
            ResourceLocation old = TEXTURES.put(skinId, id);
            if (old != null && !old.equals(id)) { releaseTexture(manager, old); }
            manager.register(id, texture);
        } catch (IOException | RuntimeException ignored) { }
    }
    public static void putModel(String skinId, byte[] modelJson) {
        if (skinId == null || skinId.isBlank()) return;
        invalidateSkinCache(skinId);
        if (modelJson == null) return;
        if (modelJson.length == 0) {
            MODEL_BYTES.remove(skinId);
            MODELS.remove(skinId);
            return;
        }
        try {
            MODEL_BYTES.put(skinId, modelJson);
            JsonObject obj = JsonParser.parseString(new String(modelJson, StandardCharsets.UTF_8)).getAsJsonObject();
            MODELS.put(skinId, obj);
            registerRuntimePoses(skinId, obj);
        } catch (RuntimeException ignored) { }
    }
    public static void clear() {
        Minecraft client = Minecraft.getInstance();
        TextureManager manager = client != null ? client.getTextureManager() : null;
        for (ResourceLocation texture : TEXTURES.values()) { releaseTexture(manager, texture); }
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
    public static void clearPreviewCaches() {
        SKIN_CACHE.clear();
        SKIN_CACHE_CAPE.clear();
        MODEL_ID_CACHE.clear();
    }
    private static void invalidateSkinCache(String skinId) {
        if (skinId == null) return;
        SKIN_CACHE.remove(skinId);
        SKIN_CACHE_CAPE.remove(skinId);
    }
    private static void clearTexture(String skinId, TextureManager manager) {
        TEXTURE_BYTES.remove(skinId);
        ResourceLocation old = TEXTURES.remove(skinId);
        releaseTexture(manager, old);
    }
    private static PlayerModelType resolveModelType(String skinId, SkinEntry entry) {
        Boolean slim = getSlimFlag(skinId);
        if (slim != null) { return slim ? PlayerModelType.SLIM : PlayerModelType.WIDE; }
        return entry != null && entry.slimArms() ? PlayerModelType.SLIM : PlayerModelType.WIDE;
    }
    private static void registerRuntimePoses(String skinId, JsonObject obj) {
        if (skinId == null || skinId.isBlank() || obj == null) return;
        JsonElement poses = getPoseElement(obj);
        if (poses == null) return;
        if (poses.isJsonArray()) {
            for (JsonElement value : poses.getAsJsonArray()) { addRuntimePose(skinId, value); }
            return;
        }
        addRuntimePose(skinId, poses);
    }
    private static void addRuntimePose(String skinId, JsonElement pose) {
        if (pose == null || !pose.isJsonPrimitive()) return;
        SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(pose.getAsString());
        if (tag != null) { SkinPoseRegistry.addRuntimeSelector(tag, skinId); }
    }
    private static JsonElement getPoseElement(JsonObject obj) {
        JsonElement direct = getPoseElementFrom(obj);
        return direct != null ? direct : getPoseElementFrom(getObject(obj, "meta"));
    }
    private static JsonElement getPoseElementFrom(JsonObject obj) {
        if (obj == null) return null;
        if (obj.has("poses")) return obj.get("poses");
        if (obj.has("pose_tags")) return obj.get("pose_tags");
        if (obj.has("animations")) return obj.get("animations");
        return null;
    }
    private static JsonObject getObject(JsonObject obj, String key) { return obj != null && obj.has(key) && obj.get(key).isJsonObject() ? obj.getAsJsonObject(key) : null; }
    private static JsonPrimitive getPrimitive(JsonObject obj, String key) { return obj != null && obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.getAsJsonPrimitive(key) : null; }
    private static Boolean getBoolean(JsonObject obj, String key) {
        JsonPrimitive primitive = getPrimitive(obj, key);
        if (primitive == null) return null;
        try {
            return primitive.getAsBoolean();
        } catch (RuntimeException ignored) { return null; }
    }
    private static String getString(JsonObject obj, String key) {
        JsonPrimitive primitive = getPrimitive(obj, key);
        if (primitive == null) return null;
        try {
            return primitive.getAsString();
        } catch (RuntimeException ignored) { return null; }
    }
    private static ResourceLocation runtimeTextureId(String skinId) { return ResourceLocation.fromNamespaceAndPath("legacy", "runtime_skins/" + skinId); }
    private static void releaseTexture(TextureManager manager, ResourceLocation id) {
        if (manager == null || id == null) return;
        manager.release(id);
    }
}
