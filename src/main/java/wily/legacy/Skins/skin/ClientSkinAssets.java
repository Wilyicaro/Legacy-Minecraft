package wily.legacy.Skins.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.boxloader.AttachSlot;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import wily.legacy.Skins.util.DebugLog;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ClientSkinAssets {
    public record ResolvedSkin(SkinEntry entry, ResourceLocation texture, ResourceLocation boxTexture, ResourceLocation modelId, BuiltBoxModel boxModel) {}
    public record AssetData(byte[] texture, byte[] model) {}
    private static final Map<String, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> MODELS = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> TEXTURE_BYTES = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> MODEL_BYTES = new ConcurrentHashMap<>();
    private static final Map<String, PlayerSkin> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, PlayerSkin> SKIN_CACHE_CAPE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> MODEL_ID_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> PREVIEW_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<String> QUEUED_PREVIEWS = ConcurrentHashMap.newKeySet();
    private ClientSkinAssets() { }
    public static ResolvedSkin resolveSkin(String skinId) { return resolveSkin(skinId, null, null, null); }
    public static ResolvedSkin resolveSkin(RenderStateSkinIdAccess access) {
        if (access == null) return null;
        return resolveSkin(
                access.consoleskins$getSkinId(),
                access.consoleskins$getCachedTexture(),
                access.consoleskins$getCachedModelId(),
                access.consoleskins$getCachedBoxModel()
        );
    }
    public static ResolvedSkin resolveSkin(String skinId, ResourceLocation texture, ResourceLocation modelId, BuiltBoxModel boxModel) {
        skinId = SkinIdUtil.isBlankOrAutoSelect(skinId) ? null : skinId;
        SkinEntry entry = skinId == null ? null : SkinPackLoader.getSkin(skinId);
        if (texture == null) texture = resolveTexture(skinId, entry);
        if (modelId == null) modelId = entry != null && entry.modelId() != null ? entry.modelId() : getModelIdFromTexture(texture);
        if (boxModel == null) boxModel = resolveBoxModel(skinId, modelId);
        if (entry == null && texture == null && modelId == null && boxModel == null) return null;
        ResourceLocation boxTexture = modelId == null ? texture : BoxModelManager.getTexture(modelId);
        return new ResolvedSkin(entry, texture, boxTexture == null ? texture : boxTexture, modelId, boxModel);
    }
    public static ResourceLocation resolveTexture(String skinId, SkinEntry entry) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        ResourceLocation texture = TEXTURES.get(skinId);
        return texture != null ? texture : entry != null ? entry.texture() : null;
    }
    public static BuiltBoxModel resolveBoxModel(String skinId, ResourceLocation modelId) {
        if (modelId == null) return null;
        BuiltBoxModel model = BoxModelManager.get(modelId);
        if (model != null || SkinIdUtil.isBlankOrAutoSelect(skinId)) return model;
        JsonObject json = MODELS.get(skinId);
        if (json == null) return null;
        BoxModelManager.registerRuntime(modelId, json);
        return BoxModelManager.get(modelId);
    }
    public static Boolean getSlimFlag(String skinId) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
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
    public static boolean hasCape(ResolvedSkin resolved) {
        return resolved != null && resolved.entry() != null && resolved.entry().cape() != null;
    }
    public static boolean shouldShowCape(ResolvedSkin resolved, boolean blocked) {
        return hasCape(resolved) && !blocked;
    }
    public static PlayerSkin resolvePlayerSkin(String skinId, ResolvedSkin resolved, boolean wantCape) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        Map<String, PlayerSkin> cache = wantCape ? SKIN_CACHE_CAPE : SKIN_CACHE;
        PlayerSkin cached = cache.get(skinId);
        if (cached != null) return cached;
        SkinEntry entry = resolved == null ? null : resolved.entry();
        ResourceLocation texture = resolved != null && resolved.texture() != null ? resolved.texture() : resolveTexture(skinId, entry);
        if (texture == null) return null;
        ClientAsset.Texture body = new ClientAsset.ResourceTexture(texture, texture);
        ClientAsset.Texture cape = wantCape && entry != null && entry.cape() != null ? new ClientAsset.ResourceTexture(entry.cape(), entry.cape()) : null;
        PlayerSkin skin = PlayerSkin.insecure(body, cape, null, resolveModelType(skinId, entry));
        cache.put(skinId, skin);
        return skin;
    }
    public static PlayerModelType resolveModelType(String skinId, ResolvedSkin resolved) {
        return resolveModelType(skinId, resolved == null ? null : resolved.entry());
    }
    public static void clearPreviewWarmup() {
        PREVIEW_QUEUE.clear();
        QUEUED_PREVIEWS.clear();
    }
    public static void enqueuePreviewWarmup(String skinId) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId) || !QUEUED_PREVIEWS.add(skinId)) return;
        PREVIEW_QUEUE.add(skinId);
    }
    public static void pumpPreviewWarmup(Minecraft client, int budget) {
        if (client == null || budget <= 0) return;
        for (int i = 0; i < budget; i++) {
            String skinId = PREVIEW_QUEUE.poll();
            if (skinId == null) return;
            QUEUED_PREVIEWS.remove(skinId);
            warmPreview(client, skinId);
        }
    }
    public static boolean hasHeadBox(ResolvedSkin resolved) {
        BuiltBoxModel model = resolved == null ? null : resolved.boxModel();
        return model != null && model.hides(AttachSlot.HEAD)
                && (hasParts(model.get(AttachSlot.HEAD)) || hasParts(model.get(AttachSlot.HAT)));
    }
    public static AssetData resolveAssetData(Minecraft client, String skinId) {
        byte[] texture = TEXTURE_BYTES.get(skinId);
        byte[] model = MODEL_BYTES.get(skinId);
        if (texture != null && model != null) return new AssetData(texture, model);
        ResolvedSkin resolved = resolveSkin(skinId);
        SkinEntry entry = resolved == null ? null : resolved.entry();
        if (texture == null) texture = loadBytes(client, resolved == null ? resolveTexture(skinId, entry) : resolved.texture());
        if (model == null) model = loadBytes(client, resolveModelLocation(client, skinId, entry));
        return new AssetData(texture, model);
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
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
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
        } catch (IOException | RuntimeException ex) {
            DebugLog.debug("Failed to load runtime skin texture {}", skinId);
        }
    }
    public static void putModel(String skinId, byte[] modelJson) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
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
        } catch (RuntimeException ex) {
            DebugLog.debug("Failed to load runtime skin model {}", skinId);
        }
    }
    public static void clear() {
        Minecraft client = Minecraft.getInstance();
        TextureManager manager = client != null ? client.getTextureManager() : null;
        for (ResourceLocation texture : TEXTURES.values()) { releaseTexture(manager, texture); }
        clearPreviewWarmup();
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
    private static boolean hasParts(java.util.List<?> parts) { return parts != null && !parts.isEmpty(); }
    private static void warmPreview(Minecraft client, String skinId) {
        ResolvedSkin resolved = resolveSkin(skinId);
        if (resolved == null) return;
        warmTexture(client, resolved.texture());
        warmTexture(client, resolved.boxTexture());
        SkinEntry entry = resolved.entry();
        warmTexture(client, entry == null ? null : entry.cape());
        resolvePlayerSkin(skinId, resolved, hasCape(resolved));
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
    private static ResourceLocation resolveModelLocation(Minecraft client, String skinId, SkinEntry entry) {
        if (client == null || SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        if (entry != null) {
            ResourceLocation modelId = entry.modelId();
            if (modelId != null) {
                ResourceLocation jsonId = BoxModelManager.getJsonLocation(modelId);
                if (jsonId != null && client.getResourceManager().getResource(jsonId).isPresent()) return jsonId;
            }
            ResourceLocation packModel = SkinIdUtil.modelLocation(entry.texture());
            if (packModel != null && client.getResourceManager().getResource(packModel).isPresent()) return packModel;
        }
        return ResourceLocation.fromNamespaceAndPath("legacy", "box_models/" + skinId + ".json");
    }
    private static byte[] loadBytes(Minecraft client, ResourceLocation id) {
        if (client == null || id == null) return new byte[0];
        Resource resource = client.getResourceManager().getResource(id).orElse(null);
        if (resource == null) return new byte[0];
        try (var in = resource.open()) {
            return in.readAllBytes();
        } catch (IOException ex) {
            DebugLog.debug("Failed to read skin asset {}", id);
            return new byte[0];
        }
    }
    private static void warmTexture(Minecraft client, ResourceLocation id) {
        if (client != null && id != null) client.getTextureManager().getTexture(id);
    }
    private static ResourceLocation runtimeTextureId(String skinId) { return ResourceLocation.fromNamespaceAndPath("legacy", "runtime_skins/" + skinId); }
    private static void releaseTexture(TextureManager manager, ResourceLocation id) {
        if (manager == null || id == null) return;
        manager.release(id);
    }
}
