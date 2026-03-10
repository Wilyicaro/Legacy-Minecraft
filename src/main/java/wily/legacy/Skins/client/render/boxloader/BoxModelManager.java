package wily.legacy.Skins.client.render.boxloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
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
import wily.legacy.Skins.client.render.SkinPoseRegistry;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class BoxModelManager {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, BuiltBoxModel> CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, BuiltBoxModel> RUNTIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, BoxMeta> META_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, BoxMeta> META_RUNTIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EnumMap<AttachSlot, float[]>> OFFSETS_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EnumMap<AttachSlot, float[]>> OFFSETS_RUNTIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EnumMap<ArmorSlot, float[]>> ARMOR_OFFSETS_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EnumMap<ArmorSlot, float[]>> ARMOR_OFFSETS_RUNTIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EnumSet<ArmorSlot>> ARMOR_HIDE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, EnumSet<ArmorSlot>> ARMOR_HIDE_RUNTIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, EnumSet<SkinPoseRegistry.PoseTag>> POSE_TAGS_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, EnumSet<SkinPoseRegistry.PoseTag>> POSE_TAGS_RUNTIME = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Map<ResourceLocation, ResourceLocation> JSON_INDEX = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Boolean> JSON_INDEX_SKINPACK = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> KEY_INDEX = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<ResourceLocation> META_LOADED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Map<ResourceLocation, Object> LOAD_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private BoxModelManager() {
    }

    public static void ensureReloaded(ResourceManager manager) {
        if (manager == null) return;
        if (initialized) return;
        reload(manager);
    }

    public static void reload(ResourceManager manager) {
        initialized = true;
        CACHE.clear();
        META_CACHE.clear();
        OFFSETS_CACHE.clear();
        ARMOR_OFFSETS_CACHE.clear();
        ARMOR_HIDE_CACHE.clear();
        POSE_TAGS_CACHE.clear();
        JSON_INDEX.clear();
        JSON_INDEX_SKINPACK.clear();
        KEY_INDEX.clear();
        META_LOADED.clear();
        LOAD_LOCKS.clear();

        loadFromBase(manager, "box_models", false);
        loadFromBase(manager, "skinpacks", true);
    }

    private static void loadFromBase(ResourceManager manager, String base, boolean skinpacksMode) {
        if (manager == null) return;
        try {
            for (ResourceLocation rl : manager.listResources(base, p -> {
                String path = p.getPath();
                if (!path.endsWith(".json")) return false;
                if (!skinpacksMode) return true;
                return path.contains("/box_models/");
            }).keySet()) {
                try {
                    ResourceLocation modelId;
                    if (skinpacksMode) {
                        String path = rl.getPath();
                        int slash = path.lastIndexOf('/');
                        String file = slash >= 0 ? path.substring(slash + 1) : path;
                        if (file.endsWith(".json")) file = file.substring(0, file.length() - 5);
                        modelId = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), file);
                    } else {
                        String path = rl.getPath();
                        if (!path.startsWith(base + "/")) continue;
                        String idPath = path.substring(base.length() + 1, path.length() - 5);
                        modelId = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), idPath);
                    }
                    JSON_INDEX.put(modelId, rl);
                    JSON_INDEX_SKINPACK.put(modelId, skinpacksMode);
                    indexKeys(rl, modelId, skinpacksMode);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void indexKeys(ResourceLocation jsonRl, ResourceLocation modelId, boolean skinpacksMode) {
        if (jsonRl == null || modelId == null) return;
        String ns = jsonRl.getNamespace();
        String key = modelId.getPath();
        KEY_INDEX.put(key, modelId);
        KEY_INDEX.put(ns + ":" + key, modelId);
        if (!skinpacksMode) {
            int s2 = key.lastIndexOf('/');
            if (s2 >= 0 && s2 + 1 < key.length()) {
                String last = key.substring(s2 + 1);
                KEY_INDEX.put(last, modelId);
                KEY_INDEX.put(ns + ":" + last, modelId);
            }
        }
    }

    public static boolean isAvailable(ResourceLocation id) {
        if (id == null) return false;
        ensureInitialized();
        if (RUNTIME.containsKey(id) || CACHE.containsKey(id)) return true;
        return JSON_INDEX.containsKey(id);
    }

    private static Object loadLock(ResourceLocation id) {
        return LOAD_LOCKS.computeIfAbsent(id, k -> new Object());
    }

    private static void ensureMetaLoaded(ResourceLocation id) {
        if (id == null) return;
        if (META_LOADED.contains(id)) return;
        if (META_RUNTIME.containsKey(id) || OFFSETS_RUNTIME.containsKey(id) || ARMOR_OFFSETS_RUNTIME.containsKey(id) || ARMOR_HIDE_RUNTIME.containsKey(id)) {
            META_LOADED.add(id);
            return;
        }
        ResourceLocation jsonRl = JSON_INDEX.get(id);
        if (jsonRl == null) {
            META_LOADED.add(id);
            return;
        }
        Object lock = loadLock(id);
        synchronized (lock) {
            if (META_LOADED.contains(id)) return;
            ResourceManager rm;
            try {
                rm = Minecraft.getInstance().getResourceManager();
            } catch (Throwable ignored) {
                return;
            }
            if (rm == null) return;
            Resource res = rm.getResource(jsonRl).orElse(null);
            if (res == null) {
                META_LOADED.add(id);
                return;
            }
            try (Reader reader = res.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root != null) {
                    boolean skinpacksMode = Boolean.TRUE.equals(JSON_INDEX_SKINPACK.get(id));
                    storeMetaFromRoot(id, jsonRl, skinpacksMode, root);
                }
            } catch (Throwable ignored) {
            }
            META_LOADED.add(id);
        }
    }

    private static void ensureBaked(ResourceLocation id) {
        if (id == null) return;
        if (RUNTIME.containsKey(id) || CACHE.containsKey(id)) return;
        ResourceLocation jsonRl = JSON_INDEX.get(id);
        if (jsonRl == null) return;
        Object lock = loadLock(id);
        synchronized (lock) {
            if (RUNTIME.containsKey(id) || CACHE.containsKey(id)) return;
            ResourceManager rm;
            try {
                rm = Minecraft.getInstance().getResourceManager();
            } catch (Throwable ignored) {
                return;
            }
            if (rm == null) return;
            Resource res = rm.getResource(jsonRl).orElse(null);
            if (res == null) return;
            try (Reader reader = res.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root == null) return;

                boolean skinpacksMode = Boolean.TRUE.equals(JSON_INDEX_SKINPACK.get(id));
                storeMetaFromRoot(id, jsonRl, skinpacksMode, root);

                JsonObject texObj = root.has("texture") && root.get("texture").isJsonObject() ? root.getAsJsonObject("texture") : null;
                int texW = texObj != null && texObj.has("width") ? texObj.get("width").getAsInt() : 64;
                int texH = texObj != null && texObj.has("height") ? texObj.get("height").getAsInt() : 64;
                BoneDef[] bonesArr = root.has("bones") && root.get("bones").isJsonArray() ? GSON.fromJson(root.get("bones"), BoneDef[].class) : null;
                if (bonesArr == null) return;
                List<BoneDef> bonesList = expandMirrors(root, bonesArr);
                EnumSet<AttachSlot> hide = parseHideSlots(root.get("hide"));
                CACHE.put(id, bake(texW, texH, bonesList, hide));
                META_LOADED.add(id);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void storeMetaFromRoot(ResourceLocation modelId, ResourceLocation jsonRl, boolean skinpacksMode, JsonObject root) {
        if (modelId == null || jsonRl == null || root == null) return;

        EnumSet<SkinPoseRegistry.PoseTag> poseTags = parsePoseTags(root);
        if (poseTags != null && !poseTags.isEmpty()) {
            String ns = jsonRl.getNamespace();
            String key = modelId.getPath();
            putPoseTags(POSE_TAGS_CACHE, key, poseTags);
            putPoseTags(POSE_TAGS_CACHE, ns + ":" + key, poseTags);
            if (!skinpacksMode) {
                int s2 = key.lastIndexOf('/');
                if (s2 >= 0 && s2 + 1 < key.length()) {
                    String last = key.substring(s2 + 1);
                    putPoseTags(POSE_TAGS_CACHE, last, poseTags);
                    putPoseTags(POSE_TAGS_CACHE, ns + ":" + last, poseTags);
                }
            }
        }

        JsonObject metaObj = root.has("meta") && root.get("meta").isJsonObject() ? root.getAsJsonObject("meta") : null;
        if (metaObj != null) {
            String themeName = metaObj.has("themeName") && metaObj.get("themeName").isJsonPrimitive() ? metaObj.get("themeName").getAsString() : null;
            String themeNameId = metaObj.has("themeNameId") && metaObj.get("themeNameId").isJsonPrimitive() ? metaObj.get("themeNameId").getAsString() : null;
            if ((themeName != null && !themeName.isBlank()) || (themeNameId != null && !themeNameId.isBlank())) {
                META_CACHE.put(modelId, new BoxMeta(themeName, themeNameId));
            } else {
                META_CACHE.remove(modelId);
            }
        } else {
            META_CACHE.remove(modelId);
        }

        if (root.has("offsets")) {
            EnumMap<AttachSlot, float[]> off = parseOffsets(root.get("offsets"));
            if (off != null && !off.isEmpty()) OFFSETS_CACHE.put(modelId, off);
            else OFFSETS_CACHE.remove(modelId);
        } else {
            OFFSETS_CACHE.remove(modelId);
        }

        JsonElement armorOffsetsEl = root.has("armor_offsets") ? root.get("armor_offsets") : (root.has("armorOffsets") ? root.get("armorOffsets") : null);
        if (armorOffsetsEl != null) {
            EnumMap<ArmorSlot, float[]> off = parseArmorOffsets(armorOffsetsEl);
            if (off != null && !off.isEmpty()) ARMOR_OFFSETS_CACHE.put(modelId, off);
            else ARMOR_OFFSETS_CACHE.remove(modelId);
        } else {
            ARMOR_OFFSETS_CACHE.remove(modelId);
        }

        JsonElement armorHideEl = root.has("hidearmour") ? root.get("hidearmour") : (root.has("hideArmour") ? root.get("hideArmour") : (root.has("hide_armor") ? root.get("hide_armor") : null));
        if (armorHideEl != null) {
            EnumSet<ArmorSlot> hs = parseArmorHideSlots(armorHideEl);
            if (hs != null && !hs.isEmpty()) ARMOR_HIDE_CACHE.put(modelId, hs);
            else ARMOR_HIDE_CACHE.remove(modelId);
        } else {
            ARMOR_HIDE_CACHE.remove(modelId);
        }
    }

    /**
     * Fast-path resource manager check. Once initialized, skips the expensive
     * Minecraft.getInstance().getResourceManager() call chain that was happening
     * 5+ times per player per frame.
     */
    private static void ensureInitialized() {
        if (initialized) return;
        ResourceManager rm = null;
        try {
            rm = Minecraft.getInstance().getResourceManager();
        } catch (Throwable ignored) {
        }
        if (rm != null) ensureReloaded(rm);
    }

    public static BuiltBoxModel get(ResourceLocation id) {
        if (id == null) return null;
        ensureInitialized();

        BuiltBoxModel rt = RUNTIME.get(id);
        if (rt != null) return rt;

        BuiltBoxModel ct = CACHE.get(id);
        if (ct != null) return ct;

        ensureBaked(id);
        return CACHE.get(id);
    }

    public static EnumMap<AttachSlot, float[]> getOffsets(ResourceLocation id) {
        if (id == null) return null;
        ensureInitialized();

        EnumMap<AttachSlot, float[]> rt = OFFSETS_RUNTIME.get(id);
        if (rt != null) return rt;

        EnumMap<AttachSlot, float[]> ct = OFFSETS_CACHE.get(id);
        if (ct != null) return ct;

        ensureMetaLoaded(id);
        return OFFSETS_CACHE.get(id);
    }

    public static EnumMap<ArmorSlot, float[]> getArmorOffsets(ResourceLocation id) {
        if (id == null) return null;
        ensureInitialized();

        EnumMap<ArmorSlot, float[]> rt = ARMOR_OFFSETS_RUNTIME.get(id);
        if (rt != null) return rt;

        EnumMap<ArmorSlot, float[]> ct = ARMOR_OFFSETS_CACHE.get(id);
        if (ct != null) return ct;

        ensureMetaLoaded(id);
        return ARMOR_OFFSETS_CACHE.get(id);
    }
    public static EnumSet<ArmorSlot> getArmorHide(ResourceLocation id) {
        if (id == null) return null;
        ensureInitialized();

        EnumSet<ArmorSlot> rt = ARMOR_HIDE_RUNTIME.get(id);
        if (rt != null) return rt;

        EnumSet<ArmorSlot> ct = ARMOR_HIDE_CACHE.get(id);
        if (ct != null) return ct;

        ensureMetaLoaded(id);
        return ARMOR_HIDE_CACHE.get(id);
    }

    public static boolean hasPoseTag(String id, SkinPoseRegistry.PoseTag tag) {
        if (id == null || id.isBlank() || tag == null) return false;
        ensureInitialized();

        EnumSet<SkinPoseRegistry.PoseTag> rt = POSE_TAGS_RUNTIME.get(id);
        if (rt != null && rt.contains(tag)) return true;

        EnumSet<SkinPoseRegistry.PoseTag> ct = POSE_TAGS_CACHE.get(id);
        if (ct != null && ct.contains(tag)) return true;

        ResourceLocation modelId = KEY_INDEX.get(id);
        if (modelId == null) {
            try {
                modelId = ResourceLocation.parse(id);
            } catch (Throwable ignored) {
                modelId = null;
            }
        }
        if (modelId != null) {
            ensureMetaLoaded(modelId);

            ct = POSE_TAGS_CACHE.get(id);
            if (ct != null && ct.contains(tag)) return true;

            String key = modelId.getPath();
            ct = POSE_TAGS_CACHE.get(key);
            if (ct != null && ct.contains(tag)) return true;

            ct = POSE_TAGS_CACHE.get(modelId.toString());
            return ct != null && ct.contains(tag);
        }

        return false;
    }

    public static void clearRuntime() {
        RUNTIME.clear();
        META_RUNTIME.clear();
        OFFSETS_RUNTIME.clear();
        ARMOR_OFFSETS_RUNTIME.clear();
        ARMOR_HIDE_RUNTIME.clear();
        POSE_TAGS_RUNTIME.clear();
    }

    private record BoxMeta(String themeName, String themeNameId) {
    }

    private static String trMaybe(String key) {
        if (key == null || key.isBlank()) return null;
        String k = key.startsWith("key:") ? key.substring(4) : key;
        try {
            String ev = wily.legacy.Skins.client.lang.SkinPackLang.get(k);
            if (ev != null && !ev.isEmpty() && !ev.equals(k)) return ev;
            String v = I18n.get(k);
            if (v != null && !v.isEmpty() && !v.equals(k)) return v;
        } catch (Throwable ignored) {
        }
        int us = k.lastIndexOf('_');
        if (us > 0) {
            boolean digits = true;
            for (int i = us + 1; i < k.length(); i++) {
                char c = k.charAt(i);
                if (c < '0' || c > '9') { digits = false; break; }
            }
            if (digits) {
                String base = k.substring(0, us);
                try {
                    String ev = wily.legacy.Skins.client.lang.SkinPackLang.get(base);
                    if (ev != null && !ev.isEmpty() && !ev.equals(base)) return ev;
                    String v = I18n.get(base);
                    if (v != null && !v.isEmpty() && !v.equals(base)) return v;
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    public static String getThemeText(ResourceLocation id) {
        if (id == null) return null;
        BoxMeta meta = META_RUNTIME.get(id);
        if (meta == null) meta = META_CACHE.get(id);
        if (meta == null) {
            ensureMetaLoaded(id);
            meta = META_RUNTIME.get(id);
            if (meta == null) meta = META_CACHE.get(id);
        }
        if (meta == null) return null;
        String out = trMaybe(meta.themeNameId);
        if (out == null || out.isBlank()) out = meta.themeName;
        if (out == null) return null;
        out = out.trim();
        return out.isEmpty() ? null : out;
    }

    private static EnumSet<SkinPoseRegistry.PoseTag> parsePoseTags(JsonObject root) {
        EnumSet<SkinPoseRegistry.PoseTag> out = EnumSet.noneOf(SkinPoseRegistry.PoseTag.class);
        if (root == null) return out;

        JsonElement poses = null;
        if (root.has("poses")) poses = root.get("poses");
        else if (root.has("animations")) poses = root.get("animations");
        collectPoseTags(out, poses);

        JsonElement hide = root.has("hide") ? root.get("hide") : null;
        collectPoseTags(out, hide);

        return out;
    }

    private static void collectPoseTags(EnumSet<SkinPoseRegistry.PoseTag> out, JsonElement el) {
        if (out == null || el == null || el.isJsonNull()) return;

        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isString()) {
                SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(p.getAsString());
                if (tag != null) out.add(tag);
            }
            return;
        }

        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                collectPoseTags(out, arr.get(i));
            }
            return;
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(e.getKey());
                if (tag != null) {
                    JsonElement v = e.getValue();
                    if (v != null && v.isJsonPrimitive()) {
                        JsonPrimitive pv = v.getAsJsonPrimitive();
                        if (pv.isBoolean() && !pv.getAsBoolean()) {
                            collectPoseTags(out, v);
                            continue;
                        }
                    }
                    out.add(tag);
                }
                collectPoseTags(out, e.getValue());
            }
        }
    }

    private static void putPoseTags(Map<String, EnumSet<SkinPoseRegistry.PoseTag>> map, String key, EnumSet<SkinPoseRegistry.PoseTag> tags) {
        if (map == null || key == null || key.isBlank() || tags == null || tags.isEmpty()) return;
        String k = key.trim();
        if (k.isEmpty()) return;
        EnumSet<SkinPoseRegistry.PoseTag> existing = map.get(k);
        if (existing == null || existing.isEmpty()) {
            map.put(k, EnumSet.copyOf(tags));
            return;
        }
        EnumSet<SkinPoseRegistry.PoseTag> merged = EnumSet.copyOf(existing);
        merged.addAll(tags);
        map.put(k, merged);
    }

    private static EnumSet<AttachSlot> parseHideSlots(JsonElement el) {
        EnumSet<AttachSlot> out = EnumSet.noneOf(AttachSlot.class);
        if (el == null || el.isJsonNull()) return out;

        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isString()) {
                addHideToken(out, p.getAsString());
                return out;
            }
            if (p.isBoolean()) {
                if (p.getAsBoolean()) out.addAll(EnumSet.allOf(AttachSlot.class));
                return out;
            }
            return out;
        }

        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement e = arr.get(i);
                if (e == null || !e.isJsonPrimitive()) continue;
                JsonPrimitive p = e.getAsJsonPrimitive();
                if (!p.isString()) continue;
                addHideToken(out, p.getAsString());
            }
            return out;
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("all") && obj.get("all").isJsonPrimitive() && obj.get("all").getAsJsonPrimitive().isBoolean() && obj.get("all").getAsBoolean()) {
                out.addAll(EnumSet.allOf(AttachSlot.class));
            }
            if (obj.has("parts")) {
                out.addAll(parseHideSlots(obj.get("parts")));
            }
            for (AttachSlot s : AttachSlot.values()) {
                String k1 = s.name();
                String k2 = s.name().toLowerCase(java.util.Locale.ROOT);
                if (obj.has(k1)) addHideToken(out, k1 + (obj.get(k1).getAsBoolean() ? "" : ""));
                if (obj.has(k2) && obj.get(k2).isJsonPrimitive() && obj.get(k2).getAsJsonPrimitive().isBoolean() && obj.get(k2).getAsBoolean()) out.add(s);
            }
            return out;
        }

        return out;
    }

    private static EnumMap<AttachSlot, float[]> parseOffsets(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        EnumMap<AttachSlot, float[]> out = new EnumMap<>(AttachSlot.class);
        try {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    AttachSlot slot = AttachSlot.fromString(e.getKey());
                    if (slot == null) continue;
                    float[] v = parseVec3(e.getValue());
                    if (v != null) out.put(slot, v);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static EnumMap<ArmorSlot, float[]> parseArmorOffsets(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        EnumMap<ArmorSlot, float[]> out = new EnumMap<>(ArmorSlot.class);
        try {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    ArmorSlot slot = ArmorSlot.fromString(e.getKey());
                    if (slot == null) continue;
                    float[] v = parseVec3(e.getValue());
                    if (v != null) out.put(slot, v);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static EnumSet<ArmorSlot> parseArmorHideSlots(JsonElement el) {
        EnumSet<ArmorSlot> out = EnumSet.noneOf(ArmorSlot.class);
        if (el == null || el.isJsonNull()) return out;

        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isString()) {
                addArmorHideToken(out, p.getAsString());
                return out;
            }
            if (p.isBoolean()) {
                if (p.getAsBoolean()) out.addAll(EnumSet.allOf(ArmorSlot.class));
                return out;
            }
            return out;
        }

        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement e = arr.get(i);
                if (e == null || !e.isJsonPrimitive()) continue;
                JsonPrimitive p = e.getAsJsonPrimitive();
                if (!p.isString()) continue;
                addArmorHideToken(out, p.getAsString());
            }
            return out;
        }

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("all") && obj.get("all").isJsonPrimitive() && obj.get("all").getAsJsonPrimitive().isBoolean() && obj.get("all").getAsBoolean()) {
                out.addAll(EnumSet.allOf(ArmorSlot.class));
            }
            if (obj.has("parts")) {
                out.addAll(parseArmorHideSlots(obj.get("parts")));
            }
            for (ArmorSlot s : ArmorSlot.values()) {
                String k1 = s.name();
                String k2 = s.name().toLowerCase(java.util.Locale.ROOT);
                if (obj.has(k2) && obj.get(k2).isJsonPrimitive() && obj.get(k2).getAsJsonPrimitive().isBoolean() && obj.get(k2).getAsBoolean()) out.add(s);
                if (obj.has(k1) && obj.get(k1).isJsonPrimitive() && obj.get(k1).getAsJsonPrimitive().isBoolean() && obj.get(k1).getAsBoolean()) out.add(s);
            }
            return out;
        }

        return out;
    }

    private static void addArmorHideToken(EnumSet<ArmorSlot> out, String token) {
        if (token == null) return;
        String t = token.trim();
        if (t.isEmpty()) return;
        String u = t.toUpperCase(java.util.Locale.ROOT);

        if ("ALL".equals(u)) {
            out.addAll(EnumSet.allOf(ArmorSlot.class));
            return;
        }
        if ("NONE".equals(u)) {
            out.clear();
            return;
        }

        ArmorSlot slot = ArmorSlot.fromString(u);
        if (slot != null) out.add(slot);
    }

    private static float[] parseVec3(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        try {
            if (el.isJsonArray()) {
                JsonArray a = el.getAsJsonArray();
                float x = a.size() > 0 ? a.get(0).getAsFloat() : 0f;
                float y = a.size() > 1 ? a.get(1).getAsFloat() : 0f;
                float z = a.size() > 2 ? a.get(2).getAsFloat() : 0f;
                return new float[]{x, y, z};
            }
            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                float x = o.has("x") ? o.get("x").getAsFloat() : (o.has("X") ? o.get("X").getAsFloat() : 0f);
                float y = o.has("y") ? o.get("y").getAsFloat() : (o.has("Y") ? o.get("Y").getAsFloat() : 0f);
                float z = o.has("z") ? o.get("z").getAsFloat() : (o.has("Z") ? o.get("Z").getAsFloat() : 0f);
                return new float[]{x, y, z};
            }
            if (el.isJsonPrimitive()) {
                JsonPrimitive p = el.getAsJsonPrimitive();
                if (p.isNumber()) {
                    return new float[]{0f, p.getAsFloat(), 0f};
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void addHideToken(EnumSet<AttachSlot> out, String token) {
        if (token == null) return;
        String t = token.trim();
        if (t.isEmpty()) return;
        String u = t.toUpperCase(java.util.Locale.ROOT);

        if ("ALL".equals(u)) {
            out.addAll(EnumSet.allOf(AttachSlot.class));
            return;
        }
        if ("NONE".equals(u)) {
            out.clear();
            return;
        }
        if ("LIMBS".equals(u)) {
            out.add(AttachSlot.LEFT_ARM);
            out.add(AttachSlot.RIGHT_ARM);
            out.add(AttachSlot.LEFT_LEG);
            out.add(AttachSlot.RIGHT_LEG);
            return;
        }
        if ("ARMS".equals(u)) {
            out.add(AttachSlot.LEFT_ARM);
            out.add(AttachSlot.RIGHT_ARM);
            return;
        }
        if ("LEGS".equals(u)) {
            out.add(AttachSlot.LEFT_LEG);
            out.add(AttachSlot.RIGHT_LEG);
            return;
        }
        if ("HEAD_AND_HAT".equals(u) || "HEAD+HAT".equals(u)) {
            out.add(AttachSlot.HEAD);
            out.add(AttachSlot.HAT);
            return;
        }

        try {
            out.add(AttachSlot.valueOf(u));
        } catch (Throwable ignored) {
        }
    }

    private static java.util.List<BoneDef> expandMirrors(JsonObject root, BoneDef[] bonesArr) {
        java.util.List<BoneDef> bones = new java.util.ArrayList<>();
        java.util.Collections.addAll(bones, bonesArr);

        if (root == null) return bones;

        boolean mirrorArm = false;
        boolean mirrorLeg = false;

        JsonElement el = root.has("mirror_limbs") ? root.get("mirror_limbs") : (root.has("mirrorLimbs") ? root.get("mirrorLimbs") : null);
        if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean() && el.getAsBoolean()) {
            mirrorArm = true;
            mirrorLeg = true;
        }

        JsonElement el2 = root.has("mirror_right_arm") ? root.get("mirror_right_arm") : (root.has("mirrorRightArm") ? root.get("mirrorRightArm") : null);
        if (el2 != null && el2.isJsonPrimitive() && el2.getAsJsonPrimitive().isBoolean() && el2.getAsBoolean()) mirrorArm = true;
        JsonElement el3 = root.has("mirror_right_leg") ? root.get("mirror_right_leg") : (root.has("mirrorRightLeg") ? root.get("mirrorRightLeg") : null);
        if (el3 != null && el3.isJsonPrimitive() && el3.getAsJsonPrimitive().isBoolean() && el3.getAsBoolean()) mirrorLeg = true;

        JsonObject mirrorObj = root.has("mirror") && root.get("mirror").isJsonObject() ? root.getAsJsonObject("mirror") : null;
        if (mirrorObj != null) {
            JsonElement a = mirrorObj.has("rightArmFromLeft") ? mirrorObj.get("rightArmFromLeft") : (mirrorObj.has("right_arm_from_left") ? mirrorObj.get("right_arm_from_left") : null);
            if (a != null && a.isJsonPrimitive() && a.getAsJsonPrimitive().isBoolean() && a.getAsBoolean()) mirrorArm = true;
            JsonElement l = mirrorObj.has("rightLegFromLeft") ? mirrorObj.get("rightLegFromLeft") : (mirrorObj.has("right_leg_from_left") ? mirrorObj.get("right_leg_from_left") : null);
            if (l != null && l.isJsonPrimitive() && l.getAsJsonPrimitive().isBoolean() && l.getAsBoolean()) mirrorLeg = true;
        }

        if (!mirrorArm && !mirrorLeg) return bones;

        java.util.HashSet<String> names = new java.util.HashSet<>();
        for (BoneDef b : bones) if (b != null && b.name() != null) names.add(b.name());

        if (mirrorArm) {
            boolean hasLeftArm = false;
            boolean hasLeftSleeve = false;
            for (BoneDef b : bonesArr) {
                if (b == null || b.attach() == null) continue;
                if (b.attach() == AttachSlot.LEFT_ARM) hasLeftArm = true;
                if (b.attach() == AttachSlot.LEFT_SLEEVE) hasLeftSleeve = true;
            }
            if (hasLeftArm) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_ARM);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_ARM) continue;
                    bones.add(mirrorBone(b, AttachSlot.RIGHT_ARM, names));
                }
            }
            if (hasLeftSleeve) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_SLEEVE);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_SLEEVE) continue;
                    bones.add(mirrorBone(b, AttachSlot.RIGHT_SLEEVE, names));
                }
            } else if (hasLeftArm) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_SLEEVE);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_ARM) continue;
                    BoneDef mb = mirrorBoneFiltered(b, AttachSlot.RIGHT_SLEEVE, names, true);
                    if (mb != null) bones.add(mb);
                }
            }
        }

        if (mirrorLeg) {
            boolean hasLeftLeg = false;
            boolean hasLeftPants = false;
            for (BoneDef b : bonesArr) {
                if (b == null || b.attach() == null) continue;
                if (b.attach() == AttachSlot.LEFT_LEG) hasLeftLeg = true;
                if (b.attach() == AttachSlot.LEFT_PANTS) hasLeftPants = true;
            }
            if (hasLeftLeg) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_LEG);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_LEG) continue;
                    bones.add(mirrorBone(b, AttachSlot.RIGHT_LEG, names));
                }
            }
            if (hasLeftPants) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_PANTS);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_PANTS) continue;
                    bones.add(mirrorBone(b, AttachSlot.RIGHT_PANTS, names));
                }
            } else if (hasLeftLeg) {
                bones.removeIf(b -> b != null && b.attach() == AttachSlot.RIGHT_PANTS);
                for (BoneDef b : bonesArr) {
                    if (b == null || b.attach() != AttachSlot.LEFT_LEG) continue;
                    BoneDef mb = mirrorBoneFiltered(b, AttachSlot.RIGHT_PANTS, names, true);
                    if (mb != null) bones.add(mb);
                }
            }
        }

        return bones;
    }

    private static BoneDef mirrorBone(BoneDef src, AttachSlot dstAttach, java.util.Set<String> usedNames) {
        return mirrorBoneFiltered(src, dstAttach, usedNames, false);
    }

    private static BoneDef mirrorBoneFiltered(BoneDef src, AttachSlot dstAttach, java.util.Set<String> usedNames, boolean onlyInflated) {
        String base = src.name() == null ? "bone" : src.name();
        String n = base;
        if (usedNames != null) {
            int i = 0;
            while (usedNames.contains(n)) {
                i++;
                n = base + "_m" + i;
            }
            usedNames.add(n);
        }

        java.util.List<CubeDef> outCubes = new java.util.ArrayList<>();
        if (src.cubes() != null) {
            for (CubeDef c : src.cubes()) {
                if (c == null) continue;
                if (onlyInflated && !(c.inflate() > 0.0001F)) continue;
                float[] o = c.origin();
                float[] s = c.size();
                if (o == null || s == null || o.length < 3 || s.length < 3) continue;
                float[] no = new float[]{-(o[0] + s[0]), o[1], o[2]};
                int[] uv = c.uv() == null ? new int[]{0, 0} : new int[]{c.uv()[0], c.uv()[1]};
                outCubes.add(new CubeDef(uv, no, new float[]{s[0], s[1], s[2]}, c.inflate(), true));
            }
        }

        if (outCubes.isEmpty()) return null;

        return new BoneDef(n, dstAttach, outCubes);
    }

private static BuiltBoxModel bake(int texW, int texH, java.util.List<BoneDef> bones, EnumSet<AttachSlot> hide) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        EnumMap<AttachSlot, CubeListBuilder> builders = new EnumMap<>(AttachSlot.class);
        EnumSet<AttachSlot> hasAny = EnumSet.noneOf(AttachSlot.class);

        for (BoneDef bone : bones) {
            if (bone == null) continue;
            AttachSlot slot = bone.attach();
            if (slot == null) continue;
            java.util.List<CubeDef> cubes = bone.cubes();
            if (cubes == null || cubes.isEmpty()) continue;

            CubeListBuilder clb = builders.get(slot);
            if (clb == null) clb = CubeListBuilder.create();

            for (CubeDef c : cubes) {
                if (c == null) continue;

                int[] uv = c.uv();
                if (uv == null || uv.length < 2) uv = new int[]{0, 0};
                clb = clb.texOffs(uv[0], uv[1]);
                clb = c.mirror() ? clb.mirror() : clb.mirror(false);

                float[] o = c.origin();
                float[] s = c.size();
                if (o == null || s == null || o.length < 3 || s.length < 3) continue;

                CubeDeformation deform = new CubeDeformation(c.inflate());

                minX = Math.min(minX, o[0]);
                minY = Math.min(minY, o[1]);
                minZ = Math.min(minZ, o[2]);
                maxX = Math.max(maxX, o[0] + s[0]);
                maxY = Math.max(maxY, o[1] + s[1]);
                maxZ = Math.max(maxZ, o[2] + s[2]);

                clb = clb.addBox(o[0], o[1], o[2], s[0], s[1], s[2], deform);
                hasAny.add(slot);
            }

            builders.put(slot, clb);
        }

        EnumMap<AttachSlot, String> slotChildren = new EnumMap<>(AttachSlot.class);
        for (AttachSlot slot : hasAny) {
            CubeListBuilder clb = builders.get(slot);
            if (clb == null) continue;
            String childName = "consoleskins$slot_" + slot.name();
            slotChildren.put(slot, childName);
            root.addOrReplaceChild(childName, clb, PartPose.ZERO);
        }

        ModelPart bakedRoot = LayerDefinition.create(mesh, texW, texH).bakeRoot();

        EnumMap<AttachSlot, java.util.List<ModelPart>> partsByAttach = new EnumMap<>(AttachSlot.class);
        for (Map.Entry<AttachSlot, String> e : slotChildren.entrySet()) {
            try {
                ModelPart part = bakedRoot.getChild(e.getValue());
                java.util.List<ModelPart> list = new java.util.ArrayList<>(1);
                list.add(part);
                partsByAttach.put(e.getKey(), list);
            } catch (Exception ignored) {
            }
        }

        if (hide == null) hide = EnumSet.noneOf(AttachSlot.class);

        float bboxH = 1.8F;
        float bboxW = 0.6F;
        if (minX != Float.POSITIVE_INFINITY && minY != Float.POSITIVE_INFINITY && minZ != Float.POSITIVE_INFINITY
                && maxX != Float.NEGATIVE_INFINITY && maxY != Float.NEGATIVE_INFINITY && maxZ != Float.NEGATIVE_INFINITY) {
            float spanX = Math.max(0.0F, maxX - minX);
            float spanY = Math.max(0.0F, maxY - minY);
            float spanZ = Math.max(0.0F, maxZ - minZ);

            float h = spanY / 16.0F;
            float w = Math.max(spanX, spanZ) / 16.0F;

            if (h > 0.01F) bboxH = Math.max(bboxH, h);
            if (w > 0.01F) bboxW = Math.max(bboxW, w);
        }

        return new BuiltBoxModel(texW, texH, bboxH, bboxW, partsByAttach, hide);
}

    public static void registerRuntime(ResourceLocation id, JsonObject root) {
        if (id == null || root == null) return;
        try {
            JsonObject texObj = root.has("texture") && root.get("texture").isJsonObject() ? root.getAsJsonObject("texture") : null;
            int texW = texObj != null && texObj.has("width") ? texObj.get("width").getAsInt() : 64;
            int texH = texObj != null && texObj.has("height") ? texObj.get("height").getAsInt() : 64;
            BoneDef[] bonesArr = root.has("bones") && root.get("bones").isJsonArray() ? GSON.fromJson(root.get("bones"), BoneDef[].class) : null;
            if (bonesArr == null) return;
            java.util.List<BoneDef> bonesList = expandMirrors(root, bonesArr);
            EnumSet<AttachSlot> hide = parseHideSlots(root.get("hide"));
            BuiltBoxModel baked = bake(texW, texH, bonesList, hide);
            RUNTIME.put(id, baked);
            EnumSet<SkinPoseRegistry.PoseTag> poseTags = parsePoseTags(root);
            if (poseTags != null && !poseTags.isEmpty()) {
                String ns = id.getNamespace();
                String key = id.getPath();
                putPoseTags(POSE_TAGS_RUNTIME, key, poseTags);
                putPoseTags(POSE_TAGS_RUNTIME, ns + ":" + key, poseTags);
                int s2 = key.lastIndexOf('/');
                if (s2 >= 0 && s2 + 1 < key.length()) {
                    String last = key.substring(s2 + 1);
                    putPoseTags(POSE_TAGS_RUNTIME, last, poseTags);
                    putPoseTags(POSE_TAGS_RUNTIME, ns + ":" + last, poseTags);
                }
            }

            if (root.has("offsets")) {
                EnumMap<AttachSlot, float[]> off = parseOffsets(root.get("offsets"));
                if (off != null && !off.isEmpty()) OFFSETS_RUNTIME.put(id, off);
                else OFFSETS_RUNTIME.remove(id);
            } else {
                OFFSETS_RUNTIME.remove(id);
            }

            JsonElement armorOffsetsEl = root.has("armor_offsets") ? root.get("armor_offsets") : (root.has("armorOffsets") ? root.get("armorOffsets") : null);
            if (armorOffsetsEl != null) {
                EnumMap<ArmorSlot, float[]> off = parseArmorOffsets(armorOffsetsEl);
                if (off != null && !off.isEmpty()) ARMOR_OFFSETS_RUNTIME.put(id, off);
                else ARMOR_OFFSETS_RUNTIME.remove(id);
            } else {
                ARMOR_OFFSETS_RUNTIME.remove(id);
            }

            JsonElement armorHideEl = root.has("hidearmour") ? root.get("hidearmour") : (root.has("hideArmour") ? root.get("hideArmour") : (root.has("hide_armor") ? root.get("hide_armor") : null));
            if (armorHideEl != null) {
                EnumSet<ArmorSlot> hs = parseArmorHideSlots(armorHideEl);
                if (hs != null && !hs.isEmpty()) ARMOR_HIDE_RUNTIME.put(id, hs);
                else ARMOR_HIDE_RUNTIME.remove(id);
            } else {
                ARMOR_HIDE_RUNTIME.remove(id);
            }

            JsonObject metaObj = root.has("meta") && root.get("meta").isJsonObject() ? root.getAsJsonObject("meta") : null;
            if (metaObj != null) {
                String themeName = metaObj.has("themeName") && metaObj.get("themeName").isJsonPrimitive() ? metaObj.get("themeName").getAsString() : null;
                String themeNameId = metaObj.has("themeNameId") && metaObj.get("themeNameId").isJsonPrimitive() ? metaObj.get("themeNameId").getAsString() : null;
                if ((themeName != null && !themeName.isBlank()) || (themeNameId != null && !themeNameId.isBlank())) {
                    META_RUNTIME.put(id, new BoxMeta(themeName, themeNameId));
                } else {
                    META_RUNTIME.remove(id);
                }
            } else {
                META_RUNTIME.remove(id);
            }
        } catch (Throwable ignored) {
        }

}
}