package wily.legacy.Skins.client.render.boxloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import wily.legacy.Skins.client.lang.SkinPackLang;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import java.io.*;
import java.util.*;

public final class BoxModelManager {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, BoxData> CACHE = new java.util.concurrent.ConcurrentHashMap<>(), RUNTIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, EnumSet<SkinPoseRegistry.PoseTag>> POSE_TAGS_CACHE = new java.util.concurrent.ConcurrentHashMap<>(),
            POSE_TAGS_RUNTIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> JSON_INDEX = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Boolean> JSON_INDEX_SKINPACK = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> KEY_INDEX = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<ResourceLocation> LOADED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Map<ResourceLocation, Object> LOAD_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile boolean initialized;
    private record BoxMeta(String themeName, String themeNameId) { }
    private record BoxData(
            BuiltBoxModel model,
            ResourceLocation texture,
            BoxMeta meta,
            EnumMap<AttachSlot, float[]> offsets,
            EnumMap<AttachSlot, float[]> scales,
            EnumMap<ArmorSlot, float[]> armorOffsets,
            EnumSet<ArmorSlot> armorHide
    ) {
        boolean isEmpty() { return model == null && texture == null && meta == null && offsets == null && scales == null && armorOffsets == null && armorHide == null; }
    }
    private BoxModelManager() { }
    public static void ensureReloaded(ResourceManager manager) {
        if (manager == null || initialized) return;
        reload(manager);
    }
    public static void reload(ResourceManager manager) {
        initialized = true;
        CACHE.clear();
        RUNTIME.clear();
        POSE_TAGS_CACHE.clear();
        POSE_TAGS_RUNTIME.clear();
        JSON_INDEX.clear();
        JSON_INDEX_SKINPACK.clear();
        KEY_INDEX.clear();
        LOADED.clear();
        LOAD_LOCKS.clear();
        loadFromBase(manager, "box_models", false);
        loadFromBase(manager, "skinpacks", true);
    }
    private static void loadFromBase(ResourceManager manager, String base, boolean skinpacksMode) {
        if (manager == null) return;
        try {
            for (ResourceLocation rl : manager.listResources(base, path -> {
                String value = path.getPath();
                return value.endsWith(".json") && (!skinpacksMode || value.contains("/box_models/"));
            }).keySet()) {
                try {
                    ResourceLocation modelId = skinpacksMode ? readSkinpackModelId(rl) : readModelId(base, rl);
                    if (modelId == null) continue;
                    JSON_INDEX.put(modelId, rl);
                    JSON_INDEX_SKINPACK.put(modelId, skinpacksMode);
                    indexKeys(rl, modelId, skinpacksMode);
                } catch (RuntimeException ignored) { }
            }
        } catch (RuntimeException ignored) { }
    }
    private static ResourceLocation readSkinpackModelId(ResourceLocation rl) {
        String path = rl.getPath();
        int slash = path.lastIndexOf('/');
        String file = slash >= 0 ? path.substring(slash + 1) : path;
        if (file.endsWith(".json")) { file = file.substring(0, file.length() - 5); }
        return ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), file);
    }
    private static ResourceLocation readModelId(String base, ResourceLocation rl) {
        String path = rl.getPath();
        if (!path.startsWith(base + "/")) return null;
        return ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), path.substring(base.length() + 1, path.length() - 5));
    }
    private static void indexKeys(ResourceLocation jsonRl, ResourceLocation modelId, boolean skinpacksMode) {
        if (jsonRl == null || modelId == null) return;
        String namespace = jsonRl.getNamespace();
        String key = modelId.getPath();
        KEY_INDEX.put(key, modelId);
        KEY_INDEX.put(namespace + ":" + key, modelId);
        if (skinpacksMode) return;
        int split = key.lastIndexOf('/');
        if (split < 0 || split + 1 >= key.length()) return;
        String leaf = key.substring(split + 1);
        KEY_INDEX.put(leaf, modelId);
        KEY_INDEX.put(namespace + ":" + leaf, modelId);
    }
    public static boolean isAvailable(ResourceLocation id) {
        if (id == null) return false;
        ensureInitialized();
        return RUNTIME.containsKey(id) || CACHE.containsKey(id) || JSON_INDEX.containsKey(id);
    }
    private static void ensureInitialized() {
        if (initialized) return;
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager != null) ensureReloaded(manager);
    }
    private static Object loadLock(ResourceLocation id) { return LOAD_LOCKS.computeIfAbsent(id, ignored -> new Object()); }
    private static void ensureLoaded(ResourceLocation id) {
        if (id == null || LOADED.contains(id)) return;
        if (RUNTIME.containsKey(id) || CACHE.containsKey(id)) {
            LOADED.add(id);
            return;
        }
        ResourceLocation jsonId = JSON_INDEX.get(id);
        if (jsonId == null) {
            LOADED.add(id);
            return;
        }
        synchronized (loadLock(id)) {
            if (LOADED.contains(id)) return;
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            if (manager == null) return;
            Resource resource = manager.getResource(jsonId).orElse(null);
            if (resource == null) {
                LOADED.add(id);
                return;
            }
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root != null) {
                    boolean leafAliases = !Boolean.TRUE.equals(JSON_INDEX_SKINPACK.get(id));
                    BoxData data = readBoxData(root);
                    if (data != null) { CACHE.put(id, data); }
                    storePoseTags(POSE_TAGS_CACHE, jsonId.getNamespace(), id.getPath(), leafAliases, BoxModelJsonSupport.parsePoseTags(root));
                }
            } catch (IOException | RuntimeException ignored) { }
            LOADED.add(id);
        }
    }
    private static BoxData getData(ResourceLocation id) {
        if (id == null) return null;
        ensureInitialized();
        BoxData runtime = RUNTIME.get(id);
        if (runtime != null) return runtime;
        BoxData cached = CACHE.get(id);
        if (cached != null) return cached;
        ensureLoaded(id);
        return CACHE.get(id);
    }
    public static BuiltBoxModel get(ResourceLocation id) {
        BoxData data = getData(id);
        return data == null ? null : data.model();
    }
    public static ResourceLocation getTexture(ResourceLocation id) {
        BoxData data = getData(id);
        return data == null ? null : data.texture();
    }
    public static EnumMap<AttachSlot, float[]> getOffsets(ResourceLocation id) {
        BoxData data = getData(id);
        return data == null ? null : data.offsets();
    }
    public static EnumMap<AttachSlot, float[]> getScales(ResourceLocation id) {
        BoxData data = getData(id);
        return data == null ? null : data.scales();
    }
    public static EnumMap<ArmorSlot, float[]> getArmorOffsets(ResourceLocation id) {
        BoxData data = getData(id);
        return data == null ? null : data.armorOffsets();
    }
    public static EnumSet<ArmorSlot> getArmorHide(ResourceLocation id) {
        BoxData data = getData(id);
        return data == null ? null : data.armorHide();
    }
    public static boolean hasPoseTag(String id, SkinPoseRegistry.PoseTag tag) {
        if (id == null || id.isBlank() || tag == null) return false;
        ensureInitialized();
        if (hasPoseTag(POSE_TAGS_RUNTIME, id, tag) || hasPoseTag(POSE_TAGS_CACHE, id, tag)) return true;
        ResourceLocation modelId = KEY_INDEX.get(id);
        if (modelId == null) {
            try {
                modelId = ResourceLocation.parse(id);
            } catch (RuntimeException ignored) { modelId = null; }
        }
        if (modelId == null) return false;
        ensureLoaded(modelId);
        return hasPoseTag(POSE_TAGS_CACHE, id, tag)
                || hasPoseTag(POSE_TAGS_CACHE, modelId.getPath(), tag)
                || hasPoseTag(POSE_TAGS_CACHE, modelId.toString(), tag);
    }
    private static boolean hasPoseTag(Map<String, EnumSet<SkinPoseRegistry.PoseTag>> tags, String key, SkinPoseRegistry.PoseTag tag) {
        EnumSet<SkinPoseRegistry.PoseTag> values = tags.get(key);
        return values != null && values.contains(tag);
    }
    public static void clearRuntime() {
        RUNTIME.clear();
        POSE_TAGS_RUNTIME.clear();
    }
    public static String getThemeText(ResourceLocation id) {
        BoxData data = getData(id);
        if (data == null || data.meta() == null) return null;
        String out = translateThemeKey(data.meta().themeNameId());
        if (out == null || out.isBlank()) out = data.meta().themeName();
        if (out == null) return null;
        out = out.trim();
        return out.isEmpty() ? null : out;
    }
    private static String translateThemeKey(String key) {
        if (key == null || key.isBlank()) return null;
        if (key.startsWith("key:")) key = key.substring(4);
        return SkinPackLang.translate(key, null);
    }
    private static BoxData readBoxData(JsonObject root) {
        if (root == null) return null;
        JsonObject texture = root.has("texture") && root.get("texture").isJsonObject() ? root.getAsJsonObject("texture") : null;
        int texW = texture != null && texture.has("width") ? texture.get("width").getAsInt() : 64;
        int texH = texture != null && texture.has("height") ? texture.get("height").getAsInt() : 64;
        float texelScale = texture != null && texture.has("scale") ? Math.max(1.0F, texture.get("scale").getAsFloat()) : 1.0F;
        BoneDef[] bones = root.has("bones") && root.get("bones").isJsonArray() ? GSON.fromJson(root.get("bones"), BoneDef[].class) : null;
        BoxData data = new BoxData(
                bones == null ? null : bake(texW, texH, texelScale, BoxModelJsonSupport.expandMirrors(root, bones), BoxModelJsonSupport.parseHideSlots(root.get("hide"))),
                readTexture(root),
                readBoxMeta(root),
                nonEmpty(BoxModelJsonSupport.parseOffsets(root.get("offsets"))),
                nonEmpty(BoxModelJsonSupport.parseScales(getAny(root, "scales", "partScale", "part_scale"))),
                nonEmpty(BoxModelJsonSupport.parseArmorOffsets(getAny(root, "armor_offsets", "armorOffsets"))),
                nonEmpty(BoxModelJsonSupport.parseArmorHideSlots(getAny(root, "hidearmour", "hideArmour", "hide_armor")))
        );
        return data.isEmpty() ? null : data;
    }
    private static void storePoseTags(Map<String, EnumSet<SkinPoseRegistry.PoseTag>> target,
                                      String namespace,
                                      String key,
                                      boolean leafAliases,
                                      EnumSet<SkinPoseRegistry.PoseTag> poseTags) {
        if (target == null || namespace == null || key == null || poseTags == null || poseTags.isEmpty()) return;
        BoxModelJsonSupport.putPoseTags(target, key, poseTags);
        BoxModelJsonSupport.putPoseTags(target, namespace + ":" + key, poseTags);
        if (!leafAliases) return;
        int split = key.lastIndexOf('/');
        if (split < 0 || split + 1 >= key.length()) return;
        String leaf = key.substring(split + 1);
        BoxModelJsonSupport.putPoseTags(target, leaf, poseTags);
        BoxModelJsonSupport.putPoseTags(target, namespace + ":" + leaf, poseTags);
    }
    private static BoxMeta readBoxMeta(JsonObject root) {
        JsonObject meta = root.has("meta") && root.get("meta").isJsonObject() ? root.getAsJsonObject("meta") : null;
        if (meta == null) return null;
        String themeName = meta.has("themeName") && meta.get("themeName").isJsonPrimitive() ? meta.get("themeName").getAsString() : null;
        String themeNameId = meta.has("themeNameId") && meta.get("themeNameId").isJsonPrimitive() ? meta.get("themeNameId").getAsString() : null;
        if ((themeName == null || themeName.isBlank()) && (themeNameId == null || themeNameId.isBlank())) return null;
        return new BoxMeta(themeName, themeNameId);
    }
    private static ResourceLocation readTexture(JsonObject root) {
        JsonObject texture = root.has("texture") && root.get("texture").isJsonObject() ? root.getAsJsonObject("texture") : null;
        if (texture == null) return null;
        String path = null;
        if (texture.has("path") && texture.get("path").isJsonPrimitive()) path = texture.get("path").getAsString();
        else if (texture.has("texturePath") && texture.get("texturePath").isJsonPrimitive()) path = texture.get("texturePath").getAsString();
        if (path == null || path.isBlank()) return null;
        try {
            return ResourceLocation.parse(path.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    private static JsonElement getAny(JsonObject root, String... keys) {
        if (root == null || keys == null) return null;
        for (String key : keys) { if (key != null && root.has(key)) return root.get(key); }
        return null;
    }
    private static <K extends Enum<K>, V> EnumMap<K, V> nonEmpty(EnumMap<K, V> map) { return map == null || map.isEmpty() ? null : map; }
    private static <E extends Enum<E>> EnumSet<E> nonEmpty(EnumSet<E> set) { return set == null || set.isEmpty() ? null : set; }
    private static BuiltBoxModel bake(int texW, int texH, float texelScale, List<BoneDef> bones, EnumSet<AttachSlot> hide) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        EnumMap<AttachSlot, CubeListBuilder> builders = new EnumMap<>(AttachSlot.class);
        EnumSet<AttachSlot> present = EnumSet.noneOf(AttachSlot.class);
        for (BoneDef bone : bones) {
            if (bone == null || bone.attach() == null || bone.cubes() == null || bone.cubes().isEmpty()) continue;
            if (Boolean.FALSE.equals(bone.visible())) continue;
            CubeListBuilder builder = builders.computeIfAbsent(bone.attach(), ignored -> CubeListBuilder.create());
            for (CubeDef cube : bone.cubes()) {
                if (cube == null) continue;
                if (Boolean.FALSE.equals(cube.visible())) continue;
                int[] uv = cube.uv();
                if (uv == null || uv.length < 2) uv = new int[]{0, 0};
                builder = builder.texOffs(uv[0], uv[1]);
                builder = cube.mirror() ? builder.mirror() : builder.mirror(false);
                float[] origin = cube.origin();
                float[] size = cube.size();
                if (origin == null || size == null || origin.length < 3 || size.length < 3) continue;
                builder = builder.addBox(
                        origin[0] * texelScale,
                        origin[1] * texelScale,
                        origin[2] * texelScale,
                        size[0] * texelScale,
                        size[1] * texelScale,
                        size[2] * texelScale,
                        new CubeDeformation(cube.inflate() * texelScale)
                );
                present.add(bone.attach());
                minX = Math.min(minX, origin[0]);
                minY = Math.min(minY, origin[1]);
                minZ = Math.min(minZ, origin[2]);
                maxX = Math.max(maxX, origin[0] + size[0]);
                maxY = Math.max(maxY, origin[1] + size[1]);
                maxZ = Math.max(maxZ, origin[2] + size[2]);
            }
            builders.put(bone.attach(), builder);
        }
        EnumMap<AttachSlot, String> childNames = new EnumMap<>(AttachSlot.class);
        for (AttachSlot slot : present) {
            CubeListBuilder builder = builders.get(slot);
            if (builder == null) continue;
            String child = "consoleskins$slot_" + slot.name();
            childNames.put(slot, child);
            root.addOrReplaceChild(child, builder, PartPose.ZERO);
        }
        ModelPart bakedRoot = LayerDefinition.create(mesh, texW, texH).bakeRoot();
        EnumMap<AttachSlot, java.util.List<ModelPart>> parts = new EnumMap<>(AttachSlot.class);
        for (Map.Entry<AttachSlot, String> entry : childNames.entrySet()) {
            try {
                parts.put(entry.getKey(), java.util.List.of(bakedRoot.getChild(entry.getValue())));
            } catch (RuntimeException ignored) { }
        }
        float bboxH = 1.8F;
        float bboxW = 0.6F;
        if (minX != Float.POSITIVE_INFINITY && minY != Float.POSITIVE_INFINITY && minZ != Float.POSITIVE_INFINITY
                && maxX != Float.NEGATIVE_INFINITY && maxY != Float.NEGATIVE_INFINITY && maxZ != Float.NEGATIVE_INFINITY) {
            float h = Math.max(0.0F, maxY - minY) / 16.0F;
            float w = Math.max(Math.max(0.0F, maxX - minX), Math.max(0.0F, maxZ - minZ)) / 16.0F;
            if (h > 0.01F) bboxH = Math.max(bboxH, h);
            if (w > 0.01F) bboxW = Math.max(bboxW, w);
        }
        return new BuiltBoxModel(texW, texH, 1.0F / texelScale, bboxH, bboxW, parts, hide == null ? EnumSet.noneOf(AttachSlot.class) : hide);
    }
    public static void registerRuntime(ResourceLocation id, JsonObject root) {
        if (id == null || root == null) return;
        try {
            BoxData data = readBoxData(root);
            if (data == null || data.model() == null) return;
            RUNTIME.put(id, data);
            storePoseTags(POSE_TAGS_RUNTIME, id.getNamespace(), id.getPath(), true, BoxModelJsonSupport.parsePoseTags(root));
        } catch (RuntimeException ignored) { }
    }
}
