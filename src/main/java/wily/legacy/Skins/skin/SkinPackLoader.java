package wily.legacy.Skins.skin;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import wily.legacy.Skins.client.lang.SkinPackLang;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import wily.legacy.Skins.util.DebugLog;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
public final class SkinPackLoader {
    private static final Object LOCK = new Object();
    private static volatile Map<String, SkinPack> PACKS = Map.of();
    private static volatile Map<String, SkinEntry> SKINS_BY_ID = Map.of();
    private static volatile Map<String, String> PACK_BY_SKIN = Map.of();
    private static volatile boolean loaded;
    private static volatile int reloadVersion;
    private static volatile String LAST_USED_CUSTOM_PACK_ID;
    private static volatile String REQUESTED_FOCUS_PACK_ID;
    private static volatile String REQUESTED_FOCUS_SKIN_ID;
    private static volatile String REQUESTED_EDIT_PACK_ID;
    private static volatile String REQUESTED_REORDER_PACK_ID;
    private static final ResourceLocation DEFAULT_PACK_ICON = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "skinpacks/default/pack.png");
    private static final String SKINPACKS_PREFIX = "skinpacks/";
    private static final String DEFAULT_SKINPACKS_PREFIX = "default_skinpacks/";
    private static final String PACK_JSON_SUFFIX = "/pack.json";
    private static final String[] RESOURCE_ROOTS = {"skinpacks", "default_skinpacks"};
    private SkinPackLoader() { }
    public static Map<String, SkinPack> getPacks() {
        ensureLoaded();
        return PACKS;
    }
    public static SkinEntry getSkin(String id) { if (!loaded) ensureLoaded(); return SKINS_BY_ID.get(id); }
    public static String getSourcePackId(String skinId) { ensureLoaded(); return PACK_BY_SKIN.get(skinId); }
    public static String getLastUsedCustomPackId() { return LAST_USED_CUSTOM_PACK_ID; }
    public static void requestFocusPack(String packId) { REQUESTED_FOCUS_PACK_ID = requestId(packId); }
    public static String consumeRequestedFocusPackId() {
        String packId = REQUESTED_FOCUS_PACK_ID;
        REQUESTED_FOCUS_PACK_ID = null;
        return packId;
    }
    public static void requestFocusSkin(String skinId) { REQUESTED_FOCUS_SKIN_ID = requestId(skinId); }
    public static String consumeRequestedFocusSkinId() {
        String skinId = REQUESTED_FOCUS_SKIN_ID;
        REQUESTED_FOCUS_SKIN_ID = null;
        return skinId;
    }
    public static void requestEditPack(String packId) { REQUESTED_EDIT_PACK_ID = requestId(packId); }
    public static String consumeRequestedEditPackId() {
        String packId = REQUESTED_EDIT_PACK_ID;
        REQUESTED_EDIT_PACK_ID = null;
        return packId;
    }
    public static void requestReorderPack(String packId) { REQUESTED_REORDER_PACK_ID = requestId(packId); }
    public static String consumeRequestedReorderPackId() {
        String packId = REQUESTED_REORDER_PACK_ID;
        REQUESTED_REORDER_PACK_ID = null;
        return packId;
    }
    public static void setLastUsedCustomPackId(String packId) {
        if (packId == null || packId.isBlank() || SkinIdUtil.PACK_DEFAULT.equals(packId) || SkinIdUtil.isFavouritesPack(packId)) packId = null;
        storeLastUsedCustomPackId(packId);
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
    static int nextCustomPackSortIndex() {
        int minSort = Integer.MAX_VALUE;
        for (SkinPack pack : PACKS.values()) {
            if (pack == null || !pack.hasSort()) continue;
            if (SkinIdUtil.PACK_DEFAULT.equals(pack.id()) || SkinIdUtil.PACK_FAVOURITES.equals(pack.id())) continue;
            minSort = Math.min(minSort, pack.sortIndex());
        }
        return minSort == Integer.MAX_VALUE ? 0 : minSort - 1;
    }
    private static String requestId(String id) {
        return id == null || id.isBlank() ? null : id;
    }
    private static void storeLastUsedCustomPackId(String packId) {
        LAST_USED_CUSTOM_PACK_ID = packId;
        ConsoleSkinsClientSettings.setLastUsedCustomPackId(packId);
    }
    public static void saveCustomPackOrder(Minecraft minecraft, List<String> orderedPackIds) throws IOException {
        if (minecraft == null) throw new IOException("Minecraft is not available");
        if (orderedPackIds == null || orderedPackIds.isEmpty()) return;
        ArrayList<SkinPack> orderedPacks = new ArrayList<>();
        for (String packId : orderedPackIds) {
            SkinPack pack = PACKS.get(packId);
            if (pack == null || SkinIdUtil.PACK_DEFAULT.equals(pack.id()) || SkinIdUtil.PACK_FAVOURITES.equals(pack.id())) continue;
            orderedPacks.add(pack);
        }
        ArrayList<SkinPack> pending = new ArrayList<>();
        Integer leftRank = null;
        boolean wroteAny = false;
        for (SkinPack pack : orderedPacks) {
            if (pack.editable()) {
                pending.add(pack);
                continue;
            }
            if (!pack.hasSort()) continue;
            wroteAny |= writeCustomPackSorts(minecraft, pending, leftRank, sortRank(pack));
            pending.clear();
            leftRank = sortRank(pack);
        }
        wroteAny |= writeCustomPackSorts(minecraft, pending, leftRank, null);
        if (!wroteAny) throw new IOException("There were no editable skin packs to reorder");
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

    private static boolean writeCustomPackSorts(Minecraft minecraft, List<SkinPack> packs, Integer leftRank, Integer rightRank) throws IOException {
        List<Integer> ranks = assignSortRanks(leftRank, rightRank, packs.size());
        boolean wroteAny = false;
        for (int i = 0; i < packs.size(); i++) {
            Path packJson = CustomSkinPackStore.packJsonPath(minecraft, packs.get(i).id());
            if (!Files.isRegularFile(packJson)) continue;
            JsonObject root = SkinPackFiles.readJson(packJson);
            int rank = ranks.get(i);
            root.addProperty("sort_index", Math.floorDiv(rank, 1000));
            root.addProperty("sort_sub_index", Math.floorMod(rank, 1000));
            root.addProperty("editable", true);
            SkinPackFiles.writeJson(packJson, root);
            wroteAny = true;
        }
        return wroteAny;
    }
    private static List<Integer> assignSortRanks(Integer leftRank, Integer rightRank, int count) throws IOException {
        ArrayList<Integer> values = new ArrayList<>(count);
        if (count <= 0) return values;
        if (leftRank == null && rightRank == null) {
            int rank = 6_000_000;
            for (int i = 0; i < count; i++, rank += 100) values.add(rank);
            return values;
        }
        if (leftRank == null) {
            int start = rightRank - count * 100;
            for (int i = 0; i < count; i++) values.add(start + i * 100);
            return values;
        }
        if (rightRank == null) {
            int value = leftRank + 100;
            for (int i = 0; i < count; i++, value += 100) values.add(value);
            return values;
        }
        int gap = rightRank - leftRank - 1;
        if (gap < count) throw new IOException("There isn't enough room to place that Skinpack there");
        int step = Math.max(1, (rightRank - leftRank) / (count + 1));
        for (int i = 1; i <= count; i++) values.add(leftRank + step * i);
        return values;
    }
    private static int sortRank(SkinPack pack) { return pack.sortIndex() * 1000 + pack.sortSubIndex(); }
    private static Map<ResourceLocation, Resource> listPackJsonResources(ResourceManager rm) {
        HashMap<ResourceLocation, Resource> jsons = new HashMap<>();
        for (String root : RESOURCE_ROOTS) addPackJsonResources(jsons, rm, root);
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
        SkinPack favPack = new SkinPack(SkinIdUtil.PACK_FAVOURITES, "key:legacy.favorites.pack", "", "", favIcon, fav, false, Integer.MIN_VALUE, true, 0);
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
        String name = SkinPackJson.string(json.get("name"));
        if (name == null || name.isBlank()) name = packId;
        name = SkinPackLang.translateMaybeKey(name, packId);
        String author = SkinPackJson.string(json.get("author"));
        if (author == null) author = "";
        String type = SkinPackJson.string(json.get("type"));
        if (type == null) type = "";
        boolean editable = SkinPackJson.bool(json.get("editable"), false);
        if (!editable) editable = CustomSkinPackStore.isCustomPack(Minecraft.getInstance(), packId);
        JsonArray skins = json.getAsJsonArray("skins");
        ArrayList<SkinEntryWithIndex> tmp = new ArrayList<>();
        LinkedHashSet<String> usedSkinIds = new LinkedHashSet<>(state.skinsById.keySet());
        LinkedHashMap<String, List<String>> skinIdsBySourceId = new LinkedHashMap<>();
        if (skins != null) {
            for (int i = 0; i < skins.size(); i++) {
                JsonElement e = skins.get(i);
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                String skinId = SkinPackJson.string(o.get("id"));
                String texPath = SkinPackJson.string(o.get("texture"));
                if (skinId == null || skinId.isBlank() || texPath == null || texPath.isBlank()) continue;
                String sourceId = skinId;
                String skinName = SkinPackJson.string(o.get("name"));
                if (skinName == null || skinName.isBlank()) skinName = sourceId;
                skinName = SkinPackLang.translateMaybeKey(skinName, sourceId);
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
                    String sidLower = sourceId.toLowerCase(Locale.ROOT);
                    String tpLower = texPath.toLowerCase(Locale.ROOT);
                    if (sidLower.contains("alex") || tpLower.contains("alex")) slimArms = true;
                }
                ResourceLocation texture = texPath.startsWith(SKINPACKS_PREFIX) || texPath.startsWith(DEFAULT_SKINPACKS_PREFIX)
                        ? ResourceLocation.fromNamespaceAndPath(namespace, texPath)
                        : ResourceLocation.fromNamespaceAndPath(namespace, packPrefix + packFolder + "/" + texPath);
                String loadedSkinId = SkinIdUtil.uniqueLoadedSkinId(packId, sourceId, usedSkinIds);
                usedSkinIds.add(loadedSkinId);
                ResourceLocation modelId = SkinIdUtil.modelId(texture.getNamespace(), loadedSkinId);
                ResourceLocation modelLocation = SkinIdUtil.modelLocation(texture);
                if (modelId != null && modelLocation != null) BoxModelManager.registerAlias(modelId, modelLocation);
                ResourceLocation cape = null;
                if (capePath != null && !capePath.isBlank()) {
                    if (capePath.startsWith(SKINPACKS_PREFIX) || capePath.startsWith(DEFAULT_SKINPACKS_PREFIX)) cape = ResourceLocation.fromNamespaceAndPath(namespace, capePath);
                    else cape = ResourceLocation.fromNamespaceAndPath(namespace, packPrefix + packFolder + "/" + capePath);
                }
                collectPoseTagsFromSkinJson(o, loadedSkinId);
                skinIdsBySourceId.computeIfAbsent(sourceId, ignored -> new ArrayList<>()).add(loadedSkinId);
                tmp.add(new SkinEntryWithIndex(new SkinEntry(loadedSkinId, sourceId, skinName, texture, modelId, cape, slimArms, order), i));
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
        boolean allowEmpty = SkinPackJson.bool(json.get("allow_empty"), false);
        if (entries.isEmpty() && !allowEmpty) return;
        collectPoseTagsFromPackJson(namespace, json, skinIdsBySourceId);
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
        Integer sortSub = null;
        if (json.has("sort_sub_index")) sortSub = SkinPackJson.integer(json.get("sort_sub_index"), 0);
        else if (json.has("sort_sub")) sortSub = SkinPackJson.integer(json.get("sort_sub"), 0);
        state.packs.put(packId, new LoadedPack(new SkinPack(packId, name, author, type, icon, entries, editable, sort, has, sortSub == null ? 0 : sortSub), sort, has, sortSub, state.nextInsertIndex++));
    }
    private static void applyBuiltinAutoSelection(Map<String, SkinPack> ordered) {
        if (LAST_USED_CUSTOM_PACK_ID != null) {
            if (ordered.containsKey(LAST_USED_CUSTOM_PACK_ID)) return;
            storeLastUsedCustomPackId(null);
        }
        if (ordered == null || ordered.isEmpty()) return;
        for (String packId : ordered.keySet()) {
            if (packId == null || packId.isBlank()) continue;
            if (SkinIdUtil.PACK_DEFAULT.equals(packId) || SkinIdUtil.PACK_FAVOURITES.equals(packId)) continue;
            storeLastUsedCustomPackId(packId);
            return;
        }
    }
    private static void collectPoseTagsFromPackJson(String namespace, JsonObject packObj, Map<String, List<String>> skinIdsBySourceId) {
        if (packObj == null) return;
        JsonObject poses = poseObject(packObj);
        if (poses == null) return;
        for (Map.Entry<String, JsonElement> e : poses.entrySet()) {
            SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(e.getKey());
            if (tag == null) continue;
            collectPoseSelectors(e.getValue(), selector -> addPackPoseSelector(tag, selector, namespace, skinIdsBySourceId));
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
    private static void addPackPoseSelector(SkinPoseRegistry.PoseTag tag,
                                            String selector,
                                            String namespace,
                                            Map<String, List<String>> skinIdsBySourceId) {
        String sourceId = exactSourceId(selector, namespace);
        if (sourceId != null && skinIdsBySourceId != null) {
            List<String> ids = skinIdsBySourceId.get(sourceId);
            if (ids != null && !ids.isEmpty()) {
                for (String id : ids) SkinPoseRegistry.addSelector(tag, id, null);
                return;
            }
        }
        SkinPoseRegistry.addSelector(tag, selector, namespace);
    }
    private static String exactSourceId(String selector, String namespace) {
        if (selector == null) return null;
        String value = selector.trim();
        if (value.isEmpty() || value.indexOf('*') >= 0 || value.indexOf('?') >= 0) return null;
        int split = value.indexOf(':');
        if (split < 0) return value;
        return namespace == null || namespace.isBlank() || split != namespace.length() || !value.startsWith(namespace + ":") ? null : value.substring(split + 1);
    }
    private record SkinEntryWithIndex(SkinEntry entry, int index) { }
    private record LoadedPack(SkinPack pack, int sortIndex, boolean hasSort, Integer sortSubIndex, int insertIndex) { }
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
            if (!leftPack.hasSort()) return Integer.compare(leftPack.insertIndex(), rightPack.insertIndex());
            if (leftPack.sortIndex() != rightPack.sortIndex()) return Integer.compare(leftPack.sortIndex(), rightPack.sortIndex());
            return Integer.compare(leftPack.insertIndex(), rightPack.insertIndex());
        });

        LinkedHashMap<String, SkinPack> ordered = new LinkedHashMap<>(entries.size());
        ArrayList<Map.Entry<String, LoadedPack>> bucket = new ArrayList<>();
        for (Map.Entry<String, LoadedPack> entry : entries) {
            LoadedPack pack = entry.getValue();
            if (!pack.hasSort()) {
                flushSortedBucket(ordered, bucket);
                ordered.put(entry.getKey(), pack.pack());
                continue;
            }
            if (!bucket.isEmpty() && bucket.getFirst().getValue().sortIndex() != pack.sortIndex()) {
                flushSortedBucket(ordered, bucket);
            }
            bucket.add(entry);
        }
        flushSortedBucket(ordered, bucket);
        return ordered;
    }
    private static void flushSortedBucket(LinkedHashMap<String, SkinPack> ordered, ArrayList<Map.Entry<String, LoadedPack>> bucket) {
        if (bucket.isEmpty()) return;
        ArrayList<ResolvedPack> resolved = new ArrayList<>(bucket.size());
        int nextSubIndex = 0;
        for (Map.Entry<String, LoadedPack> entry : bucket) {
            LoadedPack loadedPack = entry.getValue();
            int sortSubIndex = loadedPack.sortSubIndex() == null ? nextSubIndex : loadedPack.sortSubIndex();
            nextSubIndex = Math.max(nextSubIndex, sortSubIndex + 100);
            SkinPack pack = loadedPack.pack();
            resolved.add(new ResolvedPack(entry.getKey(), new SkinPack(pack.id(), pack.name(), pack.author(), pack.type(), pack.icon(), pack.skins(), pack.editable(), loadedPack.sortIndex(), true, sortSubIndex), loadedPack.insertIndex()));
        }
        resolved.sort(Comparator.comparingInt((ResolvedPack pack) -> pack.pack().sortSubIndex()).thenComparingInt(ResolvedPack::insertIndex));
        for (ResolvedPack pack : resolved) ordered.put(pack.id(), pack.pack());
        bucket.clear();
    }
    private record ResolvedPack(String id, SkinPack pack, int insertIndex) { }
}
