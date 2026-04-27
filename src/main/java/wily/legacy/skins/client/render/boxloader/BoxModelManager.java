package wily.legacy.skins.client.render.boxloader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import wily.legacy.skins.client.lang.SkinPackLang;
import wily.legacy.skins.pose.SkinPoseRegistry;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public final class BoxModelManager {
    private static final Gson GSON = new Gson();
    private static final Map<Identifier, BoxData> CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, BoxData> RUNTIME = new ConcurrentHashMap<>();
    private static final Map<String, EnumSet<SkinPoseRegistry.PoseTag>> CACHE_POSE_TAGS = new ConcurrentHashMap<>();
    private static final Map<String, EnumSet<SkinPoseRegistry.PoseTag>> RUNTIME_POSE_TAGS = new ConcurrentHashMap<>();
    private static final Map<Identifier, Identifier> JSON_INDEX = new ConcurrentHashMap<>();
    private static final Set<Identifier> SKINPACK_MODELS = ConcurrentHashMap.newKeySet();
    private static final Map<String, Identifier> KEY_INDEX = new ConcurrentHashMap<>();
    private static final Set<Identifier> LOADED = ConcurrentHashMap.newKeySet();
    private static final Map<Identifier, Object> LOAD_LOCKS = new ConcurrentHashMap<>();
    private static final BoxData EMPTY = new BoxData(null, null, null, null, null, null, null, null, null);
    private static volatile boolean initialized;

    private BoxModelManager() {
    }

    public static void ensureReloaded(ResourceManager manager) {
        if (manager != null && !initialized) reload(manager);
    }

    public static void reload(ResourceManager manager) {
        initialized = true;
        clearState();
        index(manager, "box_models", false);
        index(manager, "skinpacks", true);
    }

    public static boolean isAvailable(Identifier id) {
        if (id == null) return false;
        ensureInitialized();
        return RUNTIME.containsKey(id) || CACHE.containsKey(id) || JSON_INDEX.containsKey(id);
    }

    public static BuiltBoxModel get(Identifier id) {
        return getValue(id, BoxData::model);
    }

    public static Identifier getTexture(Identifier id) {
        return getValue(id, BoxData::texture);
    }

    public static EnumMap<AttachSlot, float[]> getOffsets(Identifier id) {
        return getValue(id, BoxData::offsets);
    }

    public static EnumMap<AttachSlot, float[]> getScales(Identifier id) {
        return getValue(id, BoxData::scales);
    }

    public static EnumMap<ArmorSlot, float[]> getArmorOffsets(Identifier id) {
        return getValue(id, BoxData::armorOffsets);
    }

    public static EnumSet<ArmorSlot> getArmorHide(Identifier id) {
        return getValue(id, BoxData::armorHide);
    }

    public static Boolean getSlimFlag(Identifier id) {
        return getValue(id, BoxData::slim);
    }

    public static boolean hasPoseTag(String id, SkinPoseRegistry.PoseTag tag) {
        if (tag == null || id == null || id.isBlank()) return false;
        ensureInitialized();
        if (hasPoseTag(RUNTIME_POSE_TAGS, id, tag) || hasPoseTag(CACHE_POSE_TAGS, id, tag)) return true;
        Identifier modelId = resolveModelId(id);
        if (modelId == null) return false;
        ensureLoaded(modelId);
        return hasPoseTag(CACHE_POSE_TAGS, id, tag)
                || hasPoseTag(CACHE_POSE_TAGS, modelId.getPath(), tag)
                || hasPoseTag(CACHE_POSE_TAGS, modelId.toString(), tag);
    }

    public static List<String> getPoseKeys(String id) {
        if (id == null || id.isBlank()) return List.of();
        ArrayList<String> keys = new ArrayList<>();
        for (SkinPoseRegistry.PoseTag tag : SkinPoseRegistry.PoseTag.values()) {
            if (hasPoseTag(id, tag)) keys.add(tag.name().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(keys);
    }

    public static void clearRuntime() {
        RUNTIME.clear();
        RUNTIME_POSE_TAGS.clear();
    }

    public static void removeRuntime(Identifier id) {
        if (id == null) return;
        RUNTIME.remove(id);
        RUNTIME_POSE_TAGS.remove(id.getPath());
        RUNTIME_POSE_TAGS.remove(id.toString());
    }

    public static String getThemeText(Identifier id) {
        BoxData data = getData(id);
        if (data == null) return null;
        String theme = translateThemeKey(data.themeKey());
        if (theme == null) theme = data.themeName();
        return trimToNull(theme);
    }

    public static void registerRuntime(Identifier id, JsonObject root) {
        if (id == null || root == null) return;
        try {
            BoxData data = readBoxData(root);
            RUNTIME.put(id, data == null ? EMPTY : data);
            storePoseTags(RUNTIME_POSE_TAGS, id.getNamespace(), id.getPath(), true, BoxModelJsonSupport.parsePoseTags(root));
        } catch (RuntimeException ignored) {
        }
    }

    public static void registerAlias(Identifier id, Identifier jsonId) {
        if (id == null || jsonId == null) return;
        ensureInitialized();
        JSON_INDEX.put(id, jsonId);
        SKINPACK_MODELS.add(id);
        indexKeys(id.getNamespace(), id, false);
        CACHE.remove(id);
        LOADED.remove(id);
        LOAD_LOCKS.remove(id);
    }

    public static Identifier getJsonLocation(Identifier id) {
        if (id == null) return null;
        ensureInitialized();
        return JSON_INDEX.get(id);
    }

    private static void clearState() {
        CACHE.clear();
        RUNTIME.clear();
        CACHE_POSE_TAGS.clear();
        RUNTIME_POSE_TAGS.clear();
        JSON_INDEX.clear();
        SKINPACK_MODELS.clear();
        KEY_INDEX.clear();
        LOADED.clear();
        LOAD_LOCKS.clear();
    }

    private static void index(ResourceManager manager, String base, boolean skinpacksMode) {
        if (manager == null) return;
        try {
            for (Identifier jsonId : manager.listResources(base, path -> isIndexedJson(path.getPath(), skinpacksMode)).keySet()) {
                try {
                    Identifier modelId = readModelId(base, jsonId, skinpacksMode);
                    if (modelId == null) continue;
                    JSON_INDEX.put(modelId, jsonId);
                    if (skinpacksMode) SKINPACK_MODELS.add(modelId);
                    indexKeys(jsonId.getNamespace(), modelId, !skinpacksMode);
                } catch (RuntimeException ignored) {
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean isIndexedJson(String path, boolean skinpacksMode) {
        return path.endsWith(".json") && (!skinpacksMode || path.contains("/box_models/"));
    }

    private static Identifier readModelId(String base, Identifier jsonId, boolean skinpacksMode) {
        String path = jsonId.getPath();
        if (skinpacksMode) {
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            return Identifier.fromNamespaceAndPath(jsonId.getNamespace(), name.substring(0, name.length() - 5));
        }
        String prefix = base + "/";
        if (!path.startsWith(prefix)) return null;
        return Identifier.fromNamespaceAndPath(jsonId.getNamespace(), path.substring(prefix.length(), path.length() - 5));
    }

    private static void indexKeys(String namespace, Identifier modelId, boolean includeLeafAlias) {
        if (namespace == null || modelId == null) return;
        forEachAlias(namespace, modelId.getPath(), includeLeafAlias, key -> KEY_INDEX.put(key, modelId));
    }

    private static void ensureInitialized() {
        if (initialized) return;
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager != null) ensureReloaded(manager);
    }

    private static Object loadLock(Identifier id) {
        return LOAD_LOCKS.computeIfAbsent(id, ignored -> new Object());
    }

    private static void ensureLoaded(Identifier id) {
        if (id == null || LOADED.contains(id)) return;
        if (RUNTIME.containsKey(id) || CACHE.containsKey(id)) {
            LOADED.add(id);
            return;
        }
        Identifier jsonId = JSON_INDEX.get(id);
        if (jsonId == null) {
            LOADED.add(id);
            return;
        }
        synchronized (loadLock(id)) {
            if (LOADED.contains(id)) return;
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            if (manager == null) return;
            Resource resource = manager.getResource(jsonId).orElse(null);
            JsonObject root = readRoot(resource);
            if (root != null) storeCachedData(id, jsonId.getNamespace(), !SKINPACK_MODELS.contains(id), root);
            LOADED.add(id);
        }
    }

    private static JsonObject readRoot(Resource resource) {
        if (resource == null) return null;
        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static void storeCachedData(Identifier id, String namespace, boolean includeLeafAlias, JsonObject root) {
        try {
            BoxData data = readBoxData(root);
            if (data != null) CACHE.put(id, data);
            storePoseTags(CACHE_POSE_TAGS, namespace, id.getPath(), includeLeafAlias, BoxModelJsonSupport.parsePoseTags(root));
        } catch (RuntimeException ignored) {
        }
    }

    private static BoxData getData(Identifier id) {
        if (id == null) return null;
        ensureInitialized();
        BoxData data = RUNTIME.get(id);
        if (data != null) return data;
        data = CACHE.get(id);
        if (data != null) return data;
        ensureLoaded(id);
        return CACHE.get(id);
    }

    private static <T> T getValue(Identifier id, Function<BoxData, T> getter) {
        BoxData data = getData(id);
        return data == null ? null : getter.apply(data);
    }

    private static Identifier resolveModelId(String id) {
        Identifier modelId = KEY_INDEX.get(id);
        if (modelId != null) return modelId;
        try {
            return Identifier.parse(id);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean hasPoseTag(Map<String, EnumSet<SkinPoseRegistry.PoseTag>> tags, String key, SkinPoseRegistry.PoseTag tag) {
        EnumSet<SkinPoseRegistry.PoseTag> values = tags.get(key);
        return values != null && values.contains(tag);
    }

    private static String translateThemeKey(String key) {
        if (key == null) return null;
        if (key.startsWith("key:")) key = key.substring(4);
        return trimToNull(SkinPackLang.translate(key, null));
    }

    private static BoxData readBoxData(JsonObject root) {
        if (root == null) return null;
        JsonObject texture = getObject(root, "texture");
        int texW = readInt(texture, "width", 64);
        int texH = readInt(texture, "height", 64);
        float texelScale = Math.max(1.0F, readFloat(texture, "scale", 1.0F));
        BoneDef[] bones = root.has("bones") && root.get("bones").isJsonArray()
                ? GSON.fromJson(root.get("bones"), BoneDef[].class)
                : null;
        JsonObject meta = getObject(root, "meta");
        BoxData data = new BoxData(
                bones == null ? null : bake(texW, texH, texelScale, BoxModelJsonSupport.expandMirrors(root, bones), BoxModelJsonSupport.parseHideSlots(root.get("hide"))),
                readTexture(texture),
                readString(meta, "themeName"),
                readString(meta, "themeNameId"),
                nonEmpty(BoxModelJsonSupport.parseOffsets(root.get("offsets"))),
                nonEmpty(BoxModelJsonSupport.parseScales(getAny(root, "scales", "partScale", "part_scale"))),
                nonEmpty(BoxModelJsonSupport.parseArmorOffsets(getAny(root, "armor_offsets", "armorOffsets"))),
                nonEmpty(BoxModelJsonSupport.parseArmorHideSlots(getAny(root, "hidearmour", "hideArmour", "hide_armor"))),
                readSlimFlag(root, meta)
        );
        return data.isEmpty() ? null : data;
    }

    private static Identifier readTexture(JsonObject texture) {
        String path = readString(texture, "path", "texturePath");
        if (path == null) return null;
        try {
            return Identifier.parse(path);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static JsonObject getObject(JsonObject root, String key) {
        return root != null && key != null && root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : null;
    }

    private static Boolean readSlimFlag(JsonObject root, JsonObject meta) {
        Boolean slim = readSlimFlag(root);
        return slim != null ? slim : readSlimFlag(meta);
    }

    private static Boolean readSlimFlag(JsonObject root) {
        if (root == null) return null;
        try {
            if (root.has("slim") && root.get("slim").isJsonPrimitive()) return root.get("slim").getAsBoolean();
        } catch (RuntimeException ignored) {
        }
        Boolean slim = readSlimFlag(readString(root, "model", "arms"));
        if (slim != null) return slim;
        return readSlimFlag(getAny(root, "poses", "pose_tags", "animations"));
    }

    private static Boolean readSlimFlag(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) return readSlimFlag(element.getAsString());
        if (!element.isJsonArray()) return null;
        for (JsonElement value : element.getAsJsonArray()) {
            Boolean slim = readSlimFlag(value);
            if (slim != null) return slim;
        }
        return null;
    }

    private static Boolean readSlimFlag(String value) {
        if (value == null) return null;
        String key = value.trim().toLowerCase(Locale.ROOT);
        if (key.equals("slim") || key.equals("alex")) return true;
        if (key.equals("wide") || key.equals("default") || key.equals("steve")) return false;
        return null;
    }

    private static String readString(JsonObject root, String... keys) {
        if (root == null || keys == null) return null;
        for (String key : keys) {
            if (key == null || !root.has(key) || !root.get(key).isJsonPrimitive()) continue;
            return trimToNull(root.get(key).getAsString());
        }
        return null;
    }

    private static int readInt(JsonObject root, String key, int defaultValue) {
        return root != null && key != null && root.has(key) ? root.get(key).getAsInt() : defaultValue;
    }

    private static float readFloat(JsonObject root, String key, float defaultValue) {
        return root != null && key != null && root.has(key) ? root.get(key).getAsFloat() : defaultValue;
    }

    private static String trimToNull(String value) {
        return value == null || (value = value.trim()).isEmpty() ? null : value;
    }

    private static JsonElement getAny(JsonObject root, String... keys) {
        if (root == null || keys == null) return null;
        for (String key : keys) {
            if (key != null && root.has(key)) return root.get(key);
        }
        return null;
    }

    private static void storePoseTags(Map<String, EnumSet<SkinPoseRegistry.PoseTag>> target,
                                      String namespace,
                                      String key,
                                      boolean includeLeafAlias,
                                      EnumSet<SkinPoseRegistry.PoseTag> poseTags) {
        if (target == null || namespace == null || key == null || poseTags == null || poseTags.isEmpty()) return;
        forEachAlias(namespace, key, includeLeafAlias, alias -> BoxModelJsonSupport.putPoseTags(target, alias, poseTags));
    }

    private static void forEachAlias(String namespace, String key, boolean includeLeafAlias, Consumer<String> consumer) {
        if (namespace == null || key == null || consumer == null) return;
        consumer.accept(key);
        consumer.accept(namespace + ":" + key);
        if (!includeLeafAlias) return;
        int split = key.lastIndexOf('/');
        if (split < 0 || split + 1 >= key.length()) return;
        String leaf = key.substring(split + 1);
        consumer.accept(leaf);
        consumer.accept(namespace + ":" + leaf);
    }

    private static <K extends Enum<K>, V> EnumMap<K, V> nonEmpty(EnumMap<K, V> map) {
        return map == null || map.isEmpty() ? null : map;
    }

    private static <E extends Enum<E>> EnumSet<E> nonEmpty(EnumSet<E> set) {
        return set == null || set.isEmpty() ? null : set;
    }

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
                if (cube == null || Boolean.FALSE.equals(cube.visible())) continue;
                float[] origin = cube.origin();
                float[] size = cube.size();
                if (origin == null || size == null || origin.length < 3 || size.length < 3) continue;
                int[] uv = cube.uv();
                if (uv == null || uv.length < 2) uv = new int[]{0, 0};
                builder = builder.texOffs(uv[0], uv[1]);
                builder = cube.mirror() ? builder.mirror() : builder.mirror(false);
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
        EnumMap<AttachSlot, List<ModelPart>> parts = new EnumMap<>(AttachSlot.class);
        for (Map.Entry<AttachSlot, String> entry : childNames.entrySet()) {
            ModelPart child = getChild(bakedRoot, entry.getValue());
            if (child != null) parts.put(entry.getKey(), List.of(child));
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

    private static ModelPart getChild(ModelPart root, String name) {
        try {
            return root.getChild(name);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record BoxData(BuiltBoxModel model, Identifier texture, String themeName, String themeKey,
                           EnumMap<AttachSlot, float[]> offsets, EnumMap<AttachSlot, float[]> scales,
                           EnumMap<ArmorSlot, float[]> armorOffsets, EnumSet<ArmorSlot> armorHide, Boolean slim) {
        boolean isEmpty() {
            return model == null && texture == null && themeName == null && themeKey == null && offsets == null && scales == null && armorOffsets == null && armorHide == null && slim == null;
        }
    }
}
