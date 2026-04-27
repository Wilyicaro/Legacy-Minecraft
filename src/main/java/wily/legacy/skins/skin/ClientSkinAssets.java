package wily.legacy.skins.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.client.render.boxloader.AttachSlot;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.render.boxloader.BuiltBoxModel;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.util.SkinsLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ClientSkinAssets {
    private static final Map<String, Identifier> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> MODELS = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> TEXTURE_BYTES = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> MODEL_BYTES = new ConcurrentHashMap<>();
    private static final Map<String, PlayerSkin> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, PlayerSkin> SKIN_CACHE_CAPE = new ConcurrentHashMap<>();
    private static final Map<Identifier, Identifier> MODEL_ID_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> PREVIEW_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<String> QUEUED_PREVIEWS = ConcurrentHashMap.newKeySet();

    private ClientSkinAssets() {
    }

    public static ResolvedSkin resolveSkin(String skinId) {
        return resolveSkin(skinId, null, null, null);
    }

    public static ResolvedSkin resolveSkin(String skinId, UUID ownerId) {
        return resolveSkin(skinId, resolveLookupAssetKey(ownerId, skinId), null, null, null);
    }

    public static ResolvedSkin resolveSkin(RenderStateSkinIdAccess access) {
        if (access == null) return null;
        return resolveSkin(
                access.consoleskins$getSkinId(),
                resolveLookupAssetKey(access.consoleskins$getEntityUuid(), access.consoleskins$getSkinId()),
                access.consoleskins$getCachedTexture(),
                access.consoleskins$getCachedModelId(),
                access.consoleskins$getCachedBoxModel()
        );
    }

    public static ResolvedSkin resolveSkin(String skinId, Identifier texture, Identifier modelId, BuiltBoxModel boxModel) {
        return resolveSkin(skinId, null, texture, modelId, boxModel);
    }

    public static String runtimeAssetKey(UUID ownerId, String skinId) {
        if (ownerId == null || SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        return ownerId + "|" + skinId;
    }

    public static Identifier resolveTexture(String skinId, SkinEntry entry) {
        return resolveTexture(null, skinId, entry);
    }

    public static BuiltBoxModel resolveBoxModel(String skinId, Identifier modelId) {
        return resolveBoxModel(null, skinId, modelId);
    }

    public static Boolean getSlimFlag(String skinId) {
        return getSlimFlag(null, skinId);
    }

    private static ResolvedSkin resolveSkin(String skinId, String assetKey, Identifier texture, Identifier modelId, BuiltBoxModel boxModel) {
        skinId = SkinIdUtil.isBlankOrAutoSelect(skinId) ? null : skinId;
        SkinEntry entry = skinId == null ? null : SkinPackLoader.getSkin(skinId);
        if (texture == null) texture = resolveTexture(assetKey, skinId, entry);
        if (modelId == null) {
            boolean runtimeModel = hasRuntimeModel(assetKey, skinId);
            modelId = runtimeModel || entry == null || entry.modelId() == null ? getModelIdFromTexture(texture) : entry.modelId();
        }
        if (boxModel == null) boxModel = resolveBoxModel(assetKey, skinId, modelId);
        if (entry == null && texture == null && modelId == null && boxModel == null) return null;
        Identifier boxTexture = modelId == null ? texture : BoxModelManager.getTexture(modelId);
        return new ResolvedSkin(entry, texture, boxTexture == null ? texture : boxTexture, modelId, boxModel);
    }

    private static Identifier resolveTexture(String assetKey, String skinId, SkinEntry entry) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        Identifier texture = assetKey == null ? null : TEXTURES.get(assetKey);
        if (texture != null) return texture;
        texture = TEXTURES.get(skinId);
        return texture != null ? texture : entry != null ? entry.texture() : null;
    }

    private static BuiltBoxModel resolveBoxModel(String assetKey, String skinId, Identifier modelId) {
        if (modelId == null) return null;
        BuiltBoxModel model = BoxModelManager.get(modelId);
        if (model != null || SkinIdUtil.isBlankOrAutoSelect(skinId)) return model == null ? null : model.copy();
        JsonObject json = assetKey == null ? null : MODELS.get(assetKey);
        if (json == null) json = MODELS.get(skinId);
        if (json == null) return null;
        BoxModelManager.registerRuntime(modelId, json);
        BuiltBoxModel runtimeModel = BoxModelManager.get(modelId);
        return runtimeModel == null ? null : runtimeModel.copy();
    }

    private static Boolean getSlimFlag(String assetKey, String skinId) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        JsonObject obj = assetKey == null ? null : MODELS.get(assetKey);
        if (obj == null) obj = MODELS.get(skinId);
        if (obj == null) return null;
        Boolean slim = getSlimFlagFrom(obj);
        return slim != null ? slim : getSlimFlagFrom(getObject(obj, "meta"));
    }

    private static Boolean getSlimFlagFrom(JsonObject obj) {
        if (obj == null) return null;
        Boolean slim = getBoolean(obj, "slim");
        if (slim != null) return slim;
        String model = getString(obj, "model");
        if (model == null) model = getString(obj, "arms");
        slim = getSlimFlagFromValue(model);
        if (slim != null) return slim;
        return getSlimFlagFromPoses(getPoseElementFrom(obj));
    }

    public static boolean hasCape(ResolvedSkin resolved) {
        return resolved != null && resolved.entry() != null && resolved.entry().cape() != null;
    }

    public static boolean shouldShowCape(ResolvedSkin resolved, boolean blocked) {
        return hasCape(resolved) && !blocked;
    }

    public static PlayerSkin resolvePlayerSkin(String skinId, ResolvedSkin resolved, boolean wantCape) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        SkinEntry entry = resolved == null ? null : resolved.entry();
        Identifier texture = resolved != null && resolved.texture() != null ? resolved.texture() : resolveTexture(skinId, entry);
        if (texture == null) return null;
        PlayerModelType modelType = resolveModelType(skinId, resolved);
        Identifier capeId = wantCape && entry != null ? entry.cape() : null;
        String cacheKey = texture + "|" + capeId + "|" + modelType;
        Map<String, PlayerSkin> cache = wantCape ? SKIN_CACHE_CAPE : SKIN_CACHE;
        PlayerSkin cached = cache.get(cacheKey);
        if (cached != null) return cached;
        ClientAsset.Texture body = new ClientAsset.ResourceTexture(texture, texture);
        ClientAsset.Texture cape = capeId != null ? new ClientAsset.ResourceTexture(capeId, capeId) : null;
        PlayerSkin skin = PlayerSkin.insecure(body, cape, null, modelType);
        cache.put(cacheKey, skin);
        return skin;
    }

    public static PlayerModelType resolveModelType(String skinId, ResolvedSkin resolved) {
        return resolveModelType(skinId, null, resolved == null ? null : resolved.entry(), resolved == null ? null : resolved.modelId());
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
        if (texture == null)
            texture = loadBytes(client, resolved == null ? resolveTexture(skinId, entry) : resolved.texture());
        if (model == null) model = loadBytes(client, resolveModelLocation(client, skinId, entry));
        return new AssetData(texture, model);
    }

    public static Identifier getModelIdFromTexture(Identifier texture) {
        if (texture == null) return null;
        return MODEL_ID_CACHE.computeIfAbsent(texture, value -> {
            String path = value.getPath();
            int slash = path.lastIndexOf('/');
            if (slash != -1) {
                path = path.substring(slash + 1);
            }
            if (path.endsWith(".png")) {
                path = path.substring(0, path.length() - 4);
            }
            return Identifier.fromNamespaceAndPath(value.getNamespace(), path);
        });
    }

    public static void putTexture(String skinId, byte[] texturePng) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        invalidateSkinCache(skinId);
        putTextureInternal(skinId, texturePng);
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
            SkinsLogger.debug("Failed to load runtime skin model {}", skinId);
        }
    }

    public static void clear() {
        Minecraft client = Minecraft.getInstance();
        TextureManager manager = client != null ? client.getTextureManager() : null;
        for (Identifier texture : TEXTURES.values()) {
            releaseTexture(manager, texture);
        }
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
        clearPreviewCaches();
    }

    private static void clearTexture(String key, TextureManager manager) {
        TEXTURE_BYTES.remove(key);
        Identifier old = TEXTURES.remove(key);
        releaseTexture(manager, old);
    }

    private static PlayerModelType resolveModelType(String skinId, String assetKey, SkinEntry entry, Identifier modelId) {
        Boolean slim = modelId == null ? null : BoxModelManager.getSlimFlag(modelId);
        if (slim == null) slim = getSlimFlag(assetKey, skinId);
        if (slim == null && entry != null && entry.modelId() != null)
            slim = BoxModelManager.getSlimFlag(entry.modelId());
        if (slim != null) {
            return slim ? PlayerModelType.SLIM : PlayerModelType.WIDE;
        }
        return entry != null && entry.slimArms() ? PlayerModelType.SLIM : PlayerModelType.WIDE;
    }

    private static boolean hasRuntimeModel(String assetKey, String skinId) {
        if (assetKey != null && (MODELS.containsKey(assetKey) || MODEL_BYTES.containsKey(assetKey))) return true;
        return skinId != null && (MODELS.containsKey(skinId) || MODEL_BYTES.containsKey(skinId));
    }

    private static String resolveLookupAssetKey(UUID ownerId, String skinId) {
        String assetKey = runtimeAssetKey(ownerId, skinId);
        if (assetKey == null) return null;
        Minecraft client = Minecraft.getInstance();
        if (client == null) return assetKey;
        UUID localId = client.player != null
                ? client.player.getUUID()
                : client.getUser() == null ? null : client.getUser().getProfileId();
        return ownerId.equals(localId) ? null : assetKey;
    }

    private static boolean hasParts(java.util.List<?> parts) {
        return parts != null && !parts.isEmpty();
    }

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
            for (JsonElement value : poses.getAsJsonArray()) {
                addRuntimePose(skinId, value);
            }
            return;
        }
        addRuntimePose(skinId, poses);
    }

    private static void addRuntimePose(String skinId, JsonElement pose) {
        if (pose == null || !pose.isJsonPrimitive()) return;
        SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(pose.getAsString());
        if (tag != null) {
            SkinPoseRegistry.addRuntimeSelector(tag, skinId);
        }
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

    private static JsonObject getObject(JsonObject obj, String key) {
        return obj != null && obj.has(key) && obj.get(key).isJsonObject() ? obj.getAsJsonObject(key) : null;
    }

    private static JsonPrimitive getPrimitive(JsonObject obj, String key) {
        return obj != null && obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.getAsJsonPrimitive(key) : null;
    }

    private static Boolean getSlimFlagFromPoses(JsonElement poses) {
        if (poses == null || poses.isJsonNull()) return null;
        if (poses.isJsonPrimitive()) return getSlimFlagFromValue(poses.getAsString());
        if (!poses.isJsonArray()) return null;
        for (JsonElement value : poses.getAsJsonArray()) {
            Boolean slim = getSlimFlagFromPoses(value);
            if (slim != null) return slim;
        }
        return null;
    }

    private static Boolean getSlimFlagFromValue(String value) {
        if (value == null) return null;
        String key = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (key.equals("slim") || key.equals("alex")) return true;
        if (key.equals("wide") || key.equals("default") || key.equals("steve")) return false;
        return null;
    }

    private static Boolean getBoolean(JsonObject obj, String key) {
        JsonPrimitive primitive = getPrimitive(obj, key);
        if (primitive == null) return null;
        try {
            return primitive.getAsBoolean();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String getString(JsonObject obj, String key) {
        JsonPrimitive primitive = getPrimitive(obj, key);
        if (primitive == null) return null;
        try {
            return primitive.getAsString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Identifier resolveModelLocation(Minecraft client, String skinId, SkinEntry entry) {
        if (client == null || SkinIdUtil.isBlankOrAutoSelect(skinId)) return null;
        if (entry != null) {
            Identifier modelId = entry.modelId();
            if (modelId != null) {
                Identifier jsonId = BoxModelManager.getJsonLocation(modelId);
                if (jsonId != null && client.getResourceManager().getResource(jsonId).isPresent()) return jsonId;
            }
            Identifier packModel = SkinIdUtil.modelLocation(entry.texture());
            if (packModel != null && client.getResourceManager().getResource(packModel).isPresent()) return packModel;
        }
        return Identifier.fromNamespaceAndPath("legacy", "box_models/" + skinId + ".json");
    }

    private static byte[] loadBytes(Minecraft client, Identifier id) {
        if (client == null || id == null) return new byte[0];
        Resource resource = client.getResourceManager().getResource(id).orElse(null);
        if (resource == null) return new byte[0];
        try (var in = resource.open()) {
            return in.readAllBytes();
        } catch (IOException ex) {
            SkinsLogger.debug("Failed to read skin asset {}", id);
            return new byte[0];
        }
    }

    private static void warmTexture(Minecraft client, Identifier id) {
        if (client != null && id != null) client.getTextureManager().getTexture(id);
    }

    private static Identifier putTextureInternal(String key, byte[] texturePng) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || key == null || key.isBlank()) return null;
        if (texturePng == null) return null;
        if (texturePng.length == 0) {
            clearTexture(key, client.getTextureManager());
            return null;
        }
        TEXTURE_BYTES.put(key, texturePng);
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(texturePng));
            DynamicTexture texture = new DynamicTexture(() -> "legacy_runtime_texture_" + key, image);
            TextureManager manager = client.getTextureManager();
            Identifier id = runtimeTextureId(key);
            Identifier old = TEXTURES.put(key, id);
            if (old != null && !old.equals(id)) releaseTexture(manager, old);
            manager.register(id, texture);
            return id;
        } catch (IOException | RuntimeException ex) {
            SkinsLogger.debug("Failed to load runtime texture {}", key);
            return null;
        }
    }

    private static Identifier runtimeTextureId(String key) {
        String safeKey = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
        return Identifier.fromNamespaceAndPath("legacy", "runtime_skins/" + safeKey);
    }

    private static void releaseTexture(TextureManager manager, Identifier id) {
        if (manager == null || id == null) return;
        manager.release(id);
    }

    public record ResolvedSkin(SkinEntry entry, Identifier texture, Identifier boxTexture,
                               Identifier modelId, BuiltBoxModel boxModel) {
    }

    public record AssetData(byte[] texture, byte[] model) {
    }
}
