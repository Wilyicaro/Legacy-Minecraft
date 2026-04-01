package wily.legacy.Skins.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import wily.legacy.Skins.client.lang.SkinPackLang;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import wily.legacy.Skins.util.DebugLog;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class SkinPackLoader {
    private static final Object LOCK = new Object();
    private static volatile Map<String, SkinPack> PACKS = Map.of();
    private static volatile Map<String, SkinEntry> SKINS_BY_ID = Map.of();
    private static volatile Map<String, String> PACK_BY_SKIN = Map.of();
    private static volatile boolean loaded;
    private static volatile int reloadVersion;
    private static volatile String LAST_USED_CUSTOM_PACK_ID;
    private static final ResourceLocation DEFAULT_PACK_ICON = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "skinpacks/default/pack.png");
    private static final String SKINPACKS_PREFIX = "skinpacks/";
    private static final String DEFAULT_SKINPACKS_PREFIX = "default_skinpacks/";
    private static final String PACK_JSON_SUFFIX = "/pack.json";
    private SkinPackLoader() { }
    public static Map<String, SkinPack> getPacks() {
        ensureLoaded();
        return PACKS;
    }
    public static SkinEntry getSkin(String id) {
        if (loaded) return SKINS_BY_ID.get(id);
        ensureLoaded();
        return SKINS_BY_ID.get(id);
    }
    public static String getSourcePackId(String skinId) {
        ensureLoaded();
        return PACK_BY_SKIN.get(skinId);
    }
    public static String getLastUsedCustomPackId() { return LAST_USED_CUSTOM_PACK_ID; }
    public static void setLastUsedCustomPackId(String packId) {
        if (packId == null || packId.isBlank()) {
            LAST_USED_CUSTOM_PACK_ID = null;
            ConsoleSkinsClientSettings.setLastUsedCustomPackId(null);
            return;
        }
        if (SkinIdUtil.PACK_DEFAULT.equals(packId) || SkinIdUtil.isFavouritesPack(packId)) {
            LAST_USED_CUSTOM_PACK_ID = null;
            ConsoleSkinsClientSettings.setLastUsedCustomPackId(null);
            return;
        }
        LAST_USED_CUSTOM_PACK_ID = packId;
        ConsoleSkinsClientSettings.setLastUsedCustomPackId(packId);
    }
    public static void ensureLoaded() {
        if (loaded) return;
        synchronized (LOCK) {
            if (!loaded) {
                LAST_USED_CUSTOM_PACK_ID = ConsoleSkinsClientSettings.getLastUsedCustomPackId();
                loadPacks();
            }
        }
    }
    public static boolean isLoaded() { return loaded; }
    public static int getReloadVersion() { return reloadVersion; }
    public static String getPreferredDefaultPackId() {
        ensureLoaded();
        Map<String, SkinPack> packs = PACKS;
        if (packs.containsKey(SkinIdUtil.PACK_DEFAULT)) return SkinIdUtil.PACK_DEFAULT;
        return packs.keySet().stream().findFirst().orElse(null);
    }
    public static void rebuildFavouritesPack() {
        ensureLoaded();
        LinkedHashMap<String, SkinPack> basePacks = new LinkedHashMap<>(PACKS);
        basePacks.remove(SkinIdUtil.PACK_FAVOURITES);
        LinkedHashMap<String, SkinPack> orderedPacks = withFavourites(basePacks, SKINS_BY_ID);
        synchronized (LOCK) { PACKS = Collections.unmodifiableMap(orderedPacks); }
    }
    public static void loadPacks() {
        Minecraft client = Minecraft.getInstance();
        ResourceManager rm = client == null ? null : client.getResourceManager();
        if (rm == null) {
            loaded = false;
            return;
        }
        if (!isClientThread(client)) {
            client.execute(() -> {
                ResourceManager queuedRm = client.getResourceManager();
                if (queuedRm != null) loadPacks(queuedRm);
            });
            return;
        }
        try {
            loadPacks(rm);
        } catch (RuntimeException ex) {
            loaded = false;
            DebugLog.warn("SkinPackLoader.loadPacks called before resources are ready: {}", ex.toString());
        }
    }
    public static void loadPacks(ResourceManager rm) {
        SkinPackLang.reload(rm);
        SkinPoseRegistry.beginReload();
        if (LAST_USED_CUSTOM_PACK_ID == null) { LAST_USED_CUSTOM_PACK_ID = ConsoleSkinsClientSettings.getLastUsedCustomPackId(); }
        PackLoadState state = new PackLoadState();
        try {
            Map<ResourceLocation, Resource> jsons = listPackJsonResources(rm);
            for (ResourceLocation rl : sortedResourceLocations(jsons)) {
                String ns = rl.getNamespace();
                String path = rl.getPath();
                String packId = parsePackId(path);
                if (packId == null || packId.isEmpty()) continue;
                if (state.packs.containsKey(packId)) continue;
                if (SkinDataStore.isExcludedPack(packId)) continue;
                JsonObject json = SkinPackJson.readObject(jsons.get(rl));
                if (json == null) continue;
                String prefix = path.startsWith(DEFAULT_SKINPACKS_PREFIX) ? DEFAULT_SKINPACKS_PREFIX : SKINPACKS_PREFIX;
                loadSinglePack(ns, packId, packId, prefix, json, rm, state);
            }
        } finally { SkinPoseRegistry.endReload(); }
        LinkedHashMap<String, SkinPack> orderedBasePacks = sortPacks(state);
        LinkedHashMap<String, SkinPack> orderedPacks = withFavourites(orderedBasePacks, state.skinsById);
        applyBuiltinAutoSelection(orderedPacks);
        synchronized (LOCK) {
            PACKS = Collections.unmodifiableMap(orderedPacks);
            SKINS_BY_ID = Collections.unmodifiableMap(state.skinsById);
            PACK_BY_SKIN = Collections.unmodifiableMap(state.packBySkin);
            loaded = true;
            reloadVersion++;
        }
    }
    private static boolean isClientThread(Minecraft client) { return client == null || client.isSameThread(); }
    private static Map<ResourceLocation, Resource> listPackJsonResources(ResourceManager rm) {
        HashMap<ResourceLocation, Resource> jsons = new HashMap<>();
        addPackJsonResources(jsons, rm, "skinpacks");
        addPackJsonResources(jsons, rm, "default_skinpacks");
        return jsons;
    }
    private static void addPackJsonResources(Map<ResourceLocation, Resource> jsons, ResourceManager rm, String root) {
        try {
            jsons.putAll(rm.listResources(root, rl -> rl.getPath().endsWith(".json")));
        } catch (RuntimeException ex) {
            DebugLog.warn("Failed to list {} resources: {}", root, ex.toString());
        }
    }
    private static List<ResourceLocation> sortedResourceLocations(Map<ResourceLocation, Resource> resources) {
        ArrayList<ResourceLocation> ids = new ArrayList<>(resources.keySet());
        ids.sort(Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath));
        return ids;
    }
    private static String parsePackId(String path) {
        if (path == null) return null;
        String id = parsePackIdForPrefix(path, SKINPACKS_PREFIX);
        return id != null ? id : parsePackIdForPrefix(path, DEFAULT_SKINPACKS_PREFIX);
    }
    private static String parsePackIdForPrefix(String path, String prefix) {
        if (path == null || prefix == null) return null;
        if (!path.startsWith(prefix) || !path.endsWith(PACK_JSON_SUFFIX)) return null;
        String packPath = path.substring(prefix.length(), path.length() - PACK_JSON_SUFFIX.length());
        if (packPath.indexOf('/') >= 0 || packPath.startsWith("_")) return null;
        return packPath;
    }
    private static LinkedHashMap<String, SkinPack> withFavourites(Map<String, SkinPack> base, Map<String, SkinEntry> skinsById) {
        ArrayList<SkinEntry> fav = new ArrayList<>();
        for (String id : SkinDataStore.getFavorites()) {
            SkinEntry e = skinsById.get(id);
            if (e != null) fav.add(e);
        }
        ResourceLocation favIcon = DEFAULT_PACK_ICON;
        SkinPack def = base.get(SkinIdUtil.PACK_DEFAULT);
        if (def != null && def.icon() != null) { favIcon = def.icon(); }
        SkinPack favPack = new SkinPack(SkinIdUtil.PACK_FAVOURITES, "key:legacy.favorites.pack", "", "", favIcon, fav);
        LinkedHashMap<String, SkinPack> out = new LinkedHashMap<>();
        if (def != null) out.put(SkinIdUtil.PACK_DEFAULT, def);
        out.put(SkinIdUtil.PACK_FAVOURITES, favPack);
        for (Map.Entry<String, SkinPack> e : base.entrySet()) {
            String k = e.getKey();
            if (SkinIdUtil.PACK_DEFAULT.equals(k)) continue;
            if (SkinIdUtil.PACK_FAVOURITES.equals(k)) continue;
            out.put(k, e.getValue());
        }
        return out;
    }
    private static void loadSinglePack(String namespace,
                                       String packId,
                                       String packFolder,
                                       String packPrefix,
                                       JsonObject json,
                                       ResourceManager rm,
                                       PackLoadState state) {
        if (state == null) return;
        collectPoseTagsFromPackJson(namespace, json);
        String name = SkinPackJson.string(json.get("name"));
        if (name == null || name.isBlank()) name = packId;
        name = SkinPackLang.translateMaybeKey(name, packId);
        String author = SkinPackJson.string(json.get("author"));
        if (author == null) author = "";
        String type = SkinPackJson.string(json.get("type"));
        if (type == null) type = "";
        JsonArray skins = json.getAsJsonArray("skins");
        ArrayList<SkinEntryWithIndex> tmp = new ArrayList<>();
        if (skins != null) {
            for (int i = 0; i < skins.size(); i++) {
                JsonElement e = skins.get(i);
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                String skinId = SkinPackJson.string(o.get("id"));
                String texPath = SkinPackJson.string(o.get("texture"));
                if (skinId == null || skinId.isBlank() || texPath == null || texPath.isBlank()) continue;
                String skinName = SkinPackJson.string(o.get("name"));
                if (skinName == null || skinName.isBlank()) skinName = skinId;
                skinName = SkinPackLang.translateMaybeKey(skinName, skinId);
                int order = SkinPackJson.integer(o.get("order"), i + 1);
                String capePath = SkinPackJson.string(o.get("cape"));
                boolean slimArms = SkinPackJson.bool(o.get("slim"), false);
                boolean modelExplicit = o.has("slim") && o.get("slim").isJsonPrimitive();
                String modelStr = SkinPackJson.string(o.get("model"));
                if (modelStr != null) {
                    modelExplicit = true;
                } else {
                    modelStr = SkinPackJson.string(o.get("arms"));
                    if (modelStr != null) modelExplicit = true;
                }
                if (modelStr != null) {
                    String m = modelStr.trim().toLowerCase(Locale.ROOT);
                    if (m.equals("slim") || m.equals("alex")) slimArms = true;
                    if (m.equals("wide") || m.equals("default") || m.equals("steve")) slimArms = false;
                }
                if (!modelExplicit && SkinIdUtil.PACK_DEFAULT.equals(packId)) {
                    String sidLower = skinId.toLowerCase(Locale.ROOT);
                    String tpLower = texPath.toLowerCase(Locale.ROOT);
                    if (sidLower.contains("alex") || tpLower.contains("alex")) slimArms = true;
                }
                collectPoseTagsFromSkinJson(o, skinId);
                ResourceLocation texture = texPath.startsWith(SKINPACKS_PREFIX) || texPath.startsWith(DEFAULT_SKINPACKS_PREFIX)
                        ? ResourceLocation.fromNamespaceAndPath(namespace, texPath)
                        : ResourceLocation.fromNamespaceAndPath(namespace, packPrefix + packFolder + "/" + texPath);
                ResourceLocation cape = null;
                if (capePath != null && !capePath.isBlank()) {
                    if (capePath.startsWith(SKINPACKS_PREFIX) || capePath.startsWith(DEFAULT_SKINPACKS_PREFIX)) cape = ResourceLocation.fromNamespaceAndPath(namespace, capePath);
                    else cape = ResourceLocation.fromNamespaceAndPath(namespace, packPrefix + packFolder + "/" + capePath);
                }
                tmp.add(new SkinEntryWithIndex(new SkinEntry(skinId, skinName, texture, cape, slimArms, order), i));
            }
        }
        tmp.sort(Comparator.comparingInt((SkinEntryWithIndex w) -> w.entry.order()).thenComparingInt(w -> w.index));
        ArrayList<SkinEntry> entries = new ArrayList<>(tmp.size());
        for (SkinEntryWithIndex w : tmp) {
            SkinEntry entry = w.entry;
            entries.add(entry);
            state.skinsById.putIfAbsent(entry.id(), entry);
            state.packBySkin.putIfAbsent(entry.id(), packId);
        }
        if (entries.isEmpty()) return;
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(namespace, packPrefix + packFolder + "/pack.png");
        if (rm.getResource(icon).isEmpty()) {
            ResourceLocation alt = ResourceLocation.fromNamespaceAndPath(namespace, DEFAULT_SKINPACKS_PREFIX + packFolder + "/pack.png");
            if (!DEFAULT_SKINPACKS_PREFIX.equals(packPrefix) && rm.getResource(alt).isPresent()) icon = alt;
            else icon = DEFAULT_PACK_ICON;
        }
        boolean has = json.has("sort_index") || json.has("sort");
        int sort = has
                ? json.has("sort_index")
                    ? SkinPackJson.integer(json.get("sort_index"), 0)
                    : SkinPackJson.integer(json.get("sort"), 0)
                : 0;
        state.packs.put(packId, new LoadedPack(new SkinPack(packId, name, author, type, icon, entries), sort, has, state.nextInsertIndex++));
    }
    private static void applyBuiltinAutoSelection(Map<String, SkinPack> ordered) {
        if (LAST_USED_CUSTOM_PACK_ID != null) {
            if (ordered.containsKey(LAST_USED_CUSTOM_PACK_ID)) return;
            LAST_USED_CUSTOM_PACK_ID = null;
            ConsoleSkinsClientSettings.setLastUsedCustomPackId(null);
        }
        if (ordered == null || ordered.isEmpty()) return;
        for (String packId : ordered.keySet()) {
            if (packId == null || packId.isBlank()) continue;
            if (SkinIdUtil.PACK_DEFAULT.equals(packId) || SkinIdUtil.PACK_FAVOURITES.equals(packId)) continue;
            LAST_USED_CUSTOM_PACK_ID = packId;
            ConsoleSkinsClientSettings.setLastUsedCustomPackId(packId);
            return;
        }
    }
    private static void collectPoseTagsFromPackJson(String namespace, JsonObject packObj) {
        if (packObj == null) return;
        JsonObject poses = poseObject(packObj);
        if (poses == null) return;
        for (Map.Entry<String, JsonElement> e : poses.entrySet()) {
            SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(e.getKey());
            if (tag == null) continue;
            collectPoseSelectors(e.getValue(), selector -> SkinPoseRegistry.addSelector(tag, selector, namespace));
        }
    }
    private static void collectPoseTagsFromSkinJson(JsonObject skinObj, String skinId) {
        if (skinObj == null || skinId == null || skinId.isBlank()) return;
        JsonElement poses = poseValue(skinObj);
        if (poses == null) return;
        collectPoseSelectors(poses, selector -> {
            SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(selector);
            if (tag != null) SkinPoseRegistry.addSelector(tag, skinId, null);
        });
    }
    private static JsonObject poseObject(JsonObject obj) {
        JsonElement poses = poseValue(obj);
        return poses != null && poses.isJsonObject() ? poses.getAsJsonObject() : null;
    }
    private static JsonElement poseValue(JsonObject obj) {
        if (obj == null) return null;
        if (obj.has("poses")) return obj.get("poses");
        return obj.has("animations") ? obj.get("animations") : null;
    }
    private static void collectPoseSelectors(JsonElement value, Consumer<String> consumer) {
        if (value == null || consumer == null) return;
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                String selector = SkinPackJson.string(array.get(i));
                if (selector != null) consumer.accept(selector);
            }
            return;
        }
        String selector = SkinPackJson.string(value);
        if (selector != null) consumer.accept(selector);
    }
    private record SkinEntryWithIndex(SkinEntry entry, int index) { }
    private record LoadedPack(SkinPack pack, int sortIndex, boolean hasSort, int insertIndex) { }
    private static final class PackLoadState {
        private final LinkedHashMap<String, LoadedPack> packs = new LinkedHashMap<>();
        private final LinkedHashMap<String, SkinEntry> skinsById = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> packBySkin = new LinkedHashMap<>();
        private int nextInsertIndex;
    }
    public static String nameString(String name, String fallbackId) { return SkinPackLang.translateMaybeKey(name, fallbackId); }
    private static LinkedHashMap<String, SkinPack> sortPacks(PackLoadState state) {
        ArrayList<Map.Entry<String, LoadedPack>> entries = new ArrayList<>(state.packs.entrySet());
        entries.sort((left, right) -> {
            LoadedPack leftPack = left.getValue();
            LoadedPack rightPack = right.getValue();
            if (leftPack.hasSort() != rightPack.hasSort()) return leftPack.hasSort() ? -1 : 1;
            if (leftPack.sortIndex() != rightPack.sortIndex()) return Integer.compare(leftPack.sortIndex(), rightPack.sortIndex());
            return Integer.compare(leftPack.insertIndex(), rightPack.insertIndex());
        });
        LinkedHashMap<String, SkinPack> ordered = new LinkedHashMap<>(entries.size());
        for (Map.Entry<String, LoadedPack> entry : entries) { ordered.put(entry.getKey(), entry.getValue().pack()); }
        return ordered;
    }
}
