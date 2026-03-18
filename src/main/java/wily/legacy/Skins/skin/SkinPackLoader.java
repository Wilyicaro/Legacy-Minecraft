package wily.legacy.Skins.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import wily.legacy.Skins.client.lang.SkinPackLang;
import wily.legacy.Skins.client.compat.ExternalSkinDescriptor;
import wily.legacy.Skins.client.compat.ExternalSkinPackDescriptor;
import wily.legacy.Skins.client.compat.ExternalSkinProviders;
import wily.legacy.Skins.client.compat.legacyskins.LegacySkinsCompat;
import wily.legacy.Skins.client.render.SkinPoseRegistry;
import wily.legacy.Skins.util.DebugLog;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

public final class SkinPackLoader {

    private static final Object LOCK = new Object();

    private static volatile Map<String, SkinPack> PACKS = Map.of();
    private static volatile Map<String, SkinPack> ALL_PACKS = Map.of();
    private static volatile Map<String, SkinEntry> SKINS_BY_ID = Map.of();
    private static volatile Map<String, String> PACK_BY_SKIN = Map.of();
    private static volatile Map<String, SkinPackSourceKind> PACK_SOURCE_KINDS = Map.of();
    private static volatile Map<String, Integer> PACK_SOURCE_ORDER = Map.of();
    private static volatile Set<String> HIDDEN_DUPLICATE_PACK_IDS = Set.of();
    private static volatile boolean HAS_MANAGED_PACK_ORDER;
    private static volatile boolean loaded;

    private static volatile String LAST_USED_CUSTOM_PACK_ID;

    private static final ResourceLocation DEFAULT_PACK_ICON = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "skinpacks/default/pack.png");

    private static final String BUILTIN_PACK_NAMESPACE = "lce_skinpacks";
    private static final ResourceLocation BUILTIN_PACK_MANIFEST = ResourceLocation.fromNamespaceAndPath(BUILTIN_PACK_NAMESPACE, "skinpacks/manifest.json");
    private static final ResourceLocation PACK_ORDER_RULES_FILE = ResourceLocation.fromNamespaceAndPath(BUILTIN_PACK_NAMESPACE, "skinpacks/pack_order_rules.json");
    private static final ResourceLocation PACK_FAMILY_RULES_FILE = ResourceLocation.fromNamespaceAndPath(BUILTIN_PACK_NAMESPACE, "skinpacks/pack_family_rules.json");

    private static final String SKINPACKS_PREFIX = "skinpacks/";
    private static final String DEFAULT_SKINPACKS_PREFIX = "default_skinpacks/";
    private static final String PACK_JSON_SUFFIX = "/pack.json";
    private static final int EXTERNAL_PACK_SORT_BASE = 20000;

    private SkinPackLoader() {
    }

    public static Map<String, SkinPack> getPacks() {
        ensureLoaded();
        return PACKS;
    }

    public static Map<String, SkinPack> getAllPacks() {
        ensureLoaded();
        return ALL_PACKS;
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

    public static SkinPackSourceKind getPackSourceKind(String packId) {
        ensureLoaded();
        if (SkinIds.PACK_FAVOURITES.equals(packId)) return SkinPackSourceKind.SPECIAL;
        if (SkinIds.PACK_DEFAULT.equals(packId)) {
            return PACK_SOURCE_KINDS.getOrDefault(packId, SkinPackSourceKind.BOX_MODEL);
        }
        return PACK_SOURCE_KINDS.getOrDefault(packId, SkinPackSourceKind.SPECIAL);
    }

    public static boolean isHiddenDuplicatePack(String packId) {
        ensureLoaded();
        return HIDDEN_DUPLICATE_PACK_IDS.contains(packId);
    }

    public static int getHiddenDuplicatePackCount() {
        ensureLoaded();
        return HIDDEN_DUPLICATE_PACK_IDS.size();
    }

    public static int getPackSourceOrder(String packId) {
        ensureLoaded();
        if (packId == null || packId.isBlank()) return Integer.MAX_VALUE;
        return PACK_SOURCE_ORDER.getOrDefault(packId, Integer.MAX_VALUE);
    }

    public static String getLastUsedCustomPackId() {
        return LAST_USED_CUSTOM_PACK_ID;
    }

    public static boolean hasManagedPackOrder() {
        ensureLoaded();
        return HAS_MANAGED_PACK_ORDER;
    }

    public static void setLastUsedCustomPackId(String packId) {
        if (packId == null || packId.isBlank()) {
            LAST_USED_CUSTOM_PACK_ID = null;
            ConsoleSkinsClientSettings.setLastUsedCustomPackId(null);
            return;
        }

        if (SkinIds.PACK_DEFAULT.equals(packId) || SkinIdUtil.isFavouritesPack(packId)) {
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

    public static boolean isLoaded() {
        return loaded;
    }

    public static String getPreferredDefaultPackId() {
        ensureLoaded();
        Map<String, SkinPack> packs = PACKS;
        if (packs.containsKey(SkinIds.PACK_DEFAULT)) return SkinIds.PACK_DEFAULT;
        return packs.keySet().stream().findFirst().orElse(null);
    }

    public static void rebuildFavouritesPack() {
        ensureLoaded();
        FavoritesStore.ensureLoaded();
        LinkedHashMap<String, SkinPack> visibleBase = new LinkedHashMap<>(PACKS);
        visibleBase.remove(SkinIds.PACK_FAVOURITES);
        LinkedHashMap<String, SkinPack> orderedVisible = withFavourites(visibleBase, SKINS_BY_ID);

        LinkedHashMap<String, SkinPack> allBase = new LinkedHashMap<>(ALL_PACKS);
        allBase.remove(SkinIds.PACK_FAVOURITES);
        LinkedHashMap<String, SkinPack> orderedAll = withFavourites(allBase, SKINS_BY_ID);

        LinkedHashMap<String, SkinPackSourceKind> sourceKinds = new LinkedHashMap<>(PACK_SOURCE_KINDS);
        sourceKinds.put(SkinIds.PACK_FAVOURITES, SkinPackSourceKind.SPECIAL);
        synchronized (LOCK) {
            PACKS = Collections.unmodifiableMap(orderedVisible);
            ALL_PACKS = Collections.unmodifiableMap(orderedAll);
            PACK_SOURCE_KINDS = Collections.unmodifiableMap(sourceKinds);
        }
    }

    public static void loadPacks() {
        try {
            Minecraft mc = Minecraft.getInstance();
            try {
                if (mc != null) {
                    boolean same;
                    try {
                        java.lang.reflect.Method m = mc.getClass().getMethod("isSameThread");
                        Object r = m.invoke(mc);
                        same = r instanceof Boolean b ? b : true;
                    } catch (Throwable ignored) {
                        same = true;
                    }
                    if (!same) {
                        mc.execute(() -> {
                            try {
                                ResourceManager rm = mc.getResourceManager();
                                if (rm != null) loadPacks(rm);
                            } catch (Throwable ignored) {
                            }
                        });
                        return;
                    }
                }
            } catch (Throwable ignored) {
            }
            ResourceManager rm = mc != null ? mc.getResourceManager() : null;
            if (rm == null) {
                loaded = false;
                return;
            }
            loadPacks(rm);
        } catch (Throwable t) {
            loaded = false;
            DebugLog.warn("SkinPackLoader.loadPacks called before resources are ready: {}", t.toString());
        }
    }

    public static void loadPacks(ResourceManager rm) {

        SkinPackLang.reload(rm);

        SkinPoseRegistry.beginReload();

        if (LAST_USED_CUSTOM_PACK_ID == null) {
            LAST_USED_CUSTOM_PACK_ID = ConsoleSkinsClientSettings.getLastUsedCustomPackId();
        }
        FavoritesStore.ensureLoaded();
        PackExclusions.reload();

        LinkedHashMap<String, SkinPack> packs = new LinkedHashMap<>();
        HashMap<String, Integer> packSortIndex = new HashMap<>();
        HashSet<String> packHasSort = new HashSet<>();
        HashMap<String, Integer> packInsertIndex = new HashMap<>();
        HashMap<String, Integer> packSortAnchorInsertIndex = new HashMap<>();
        HashMap<String, Integer> packSortPriority = new HashMap<>();
        HashMap<String, SkinPackSourceKind> packSourceKinds = new HashMap<>();
        HashMap<String, Integer> packSourceOrder = new HashMap<>();
        LinkedHashSet<String> hiddenDuplicatePackIds = new LinkedHashSet<>();
        final int[] insertCounter = new int[]{0};
        LinkedHashMap<String, SkinEntry> skinsById = new LinkedHashMap<>();
        LinkedHashMap<String, String> packBySkin = new LinkedHashMap<>();
        HashMap<String, String> preferredNewFormatPackAliases = new HashMap<>();
        HashMap<String, String> preferredNewFormatPackFingerprints = new HashMap<>();
        LinkedHashMap<String, List<String>> preferredNewFormatPackTokens = new LinkedHashMap<>();
        SkinPackOrderRules packOrderRules = SkinPackOrderRules.load(rm, PACK_ORDER_RULES_FILE);
        SkinPackFamilyRules packFamilyRules = SkinPackFamilyRules.load(rm, PACK_FAMILY_RULES_FILE);
        HAS_MANAGED_PACK_ORDER = packOrderRules.hasEntries();

        try {
            List<String> namespaces = new ArrayList<>(rm.getNamespaces());
            namespaces.sort(String::compareTo);

            boolean anyManifest = false;
            for (String ns : namespaces) {
                if (loadFromManifest(rm, ns, packs, skinsById, packBySkin, packSortIndex, packHasSort, packInsertIndex, insertCounter, packSourceKinds, packSourceOrder, preferredNewFormatPackAliases, preferredNewFormatPackFingerprints, preferredNewFormatPackTokens, packOrderRules)) anyManifest = true;
            }

            try {
                Map<ResourceLocation, Resource> jsons = new java.util.HashMap<>();
                try { jsons.putAll(rm.listResources("skinpacks", rl -> rl.getPath().endsWith(".json"))); } catch (Throwable ignored) {}
                try { jsons.putAll(rm.listResources("default_skinpacks", rl -> rl.getPath().endsWith(".json"))); } catch (Throwable ignored) {}
                ArrayList<ResourceLocation> keys = new ArrayList<>(jsons.keySet());
                keys.sort(Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath));

                for (ResourceLocation rl : keys) {
                    String ns = rl.getNamespace();
                    String path = rl.getPath();

                    String packId = parsePackId(path);
                    if (packId == null || packId.isEmpty()) continue;
                    if (packs.containsKey(packId)) continue;
                    if (PackExclusions.isExcluded(packId)) continue;

                    Integer explicitBoxModelOrder = packOrderRules.findBoxModelPackOrder(ns, packId, BUILTIN_PACK_NAMESPACE);

                    if (anyManifest && packId.startsWith("_")) continue;

                    JsonObject json = readObj(jsons.get(rl));
                    if (json != null) {
                        String prefix = path.startsWith(DEFAULT_SKINPACKS_PREFIX) ? DEFAULT_SKINPACKS_PREFIX : SKINPACKS_PREFIX;
                        boolean hadPack = packs.containsKey(packId);
                        loadSinglePack(ns, packId, packId, prefix, json, rm, packs, skinsById, packBySkin, packSortIndex, packHasSort, packInsertIndex, insertCounter);
                        if (!hadPack && packs.containsKey(packId)) {
                            SkinPackOrderingSupport.registerPackSource(packSourceKinds, packId, SkinPackSourceKind.BOX_MODEL);
                            SkinPackOrderingSupport.registerPackSourceOrder(packSourceOrder, packId, explicitBoxModelOrder, packSortIndex, packInsertIndex);
                        }
                        if (!hadPack && packs.containsKey(packId) && BUILTIN_PACK_NAMESPACE.equals(ns)) {
                            if (explicitBoxModelOrder != null) {
                                packSortIndex.put(packId, explicitBoxModelOrder);
                                packHasSort.add(packId);
                            }
                            SkinPack pack = packs.get(packId);
                            registerNewFormatPackAliases(preferredNewFormatPackAliases, ns, packId, packId, pack.name());
                            registerManagedBoxModelNameAlias(preferredNewFormatPackAliases, packOrderRules, packId);
                            registerNewFormatPackFingerprint(preferredNewFormatPackFingerprints, pack);
                            registerNewFormatPackTokens(preferredNewFormatPackTokens, pack);
                        }
                    }
                }
            } catch (Throwable ex) {
                DebugLog.warn("ResourceManager skinpack scan failed: {}", ex.toString());
            }

            boolean hideDuplicateExternalPacks = true;
            loadLegacySkinPacksIndex(rm, packs, skinsById, packBySkin, packSortIndex, packHasSort, packInsertIndex, packSortAnchorInsertIndex, packSortPriority, packSourceKinds, packSourceOrder, hiddenDuplicatePackIds, insertCounter, preferredNewFormatPackAliases, preferredNewFormatPackFingerprints, preferredNewFormatPackTokens, hideDuplicateExternalPacks, packOrderRules);
            loadBedrockSkinPacksIndex(packs, skinsById, packBySkin, packSortIndex, packHasSort, packInsertIndex, packSortAnchorInsertIndex, packSortPriority, packSourceKinds, packSourceOrder, hiddenDuplicatePackIds, insertCounter, preferredNewFormatPackAliases, preferredNewFormatPackFingerprints, preferredNewFormatPackTokens, hideDuplicateExternalPacks, packOrderRules);
        } finally {
            SkinPoseRegistry.endReload();
        }

        packs = SkinPackOrderingSupport.sortPacks(packs, packSortIndex, packHasSort, packInsertIndex, packSortAnchorInsertIndex, packSortPriority);

        SkinPackDefaultOrganizer.OrganizationResult organization = SkinPackDefaultOrganizer.organize(
                packs,
                packSourceKinds,
                packSourceOrder,
                packInsertIndex,
                packOrderRules,
                packFamilyRules,
                true
        );
        LinkedHashMap<String, SkinPack> organizedBasePacks = organization.orderedBasePacks();
        LinkedHashSet<String> organizedHiddenDuplicatePackIds = new LinkedHashSet<>(organization.hiddenDuplicatePackIds());
        LinkedHashMap<String, Integer> organizedSourceOrder = new LinkedHashMap<>(organization.sourceOrder());
        enforcePreferredFamilyVisibility(organizedBasePacks, packSourceKinds, packFamilyRules, organizedHiddenDuplicatePackIds);

        LinkedHashMap<String, SkinPack> orderedAll = withFavourites(organizedBasePacks, skinsById);
        LinkedHashMap<String, SkinPack> orderedVisible = SkinPackOrderingSupport.filterVisiblePacks(orderedAll, organizedHiddenDuplicatePackIds);
        SkinPackDiagnostics.logLoadDiagnostics(organizedBasePacks, packBySkin, packSourceKinds, organizedHiddenDuplicatePackIds, packOrderRules, packFamilyRules);
        applyBuiltinAutoSelection(rm, orderedVisible);
        packSourceKinds.put(SkinIds.PACK_FAVOURITES, SkinPackSourceKind.SPECIAL);
        synchronized (LOCK) {
            PACKS = Collections.unmodifiableMap(orderedVisible);
            ALL_PACKS = Collections.unmodifiableMap(orderedAll);
            SKINS_BY_ID = Collections.unmodifiableMap(skinsById);
            PACK_BY_SKIN = Collections.unmodifiableMap(packBySkin);
            PACK_SOURCE_KINDS = Collections.unmodifiableMap(new LinkedHashMap<>(packSourceKinds));
            PACK_SOURCE_ORDER = Collections.unmodifiableMap(organizedSourceOrder);
            HIDDEN_DUPLICATE_PACK_IDS = Collections.unmodifiableSet(organizedHiddenDuplicatePackIds);
            loaded = true;
        }
    }

    private static void enforcePreferredFamilyVisibility(Map<String, SkinPack> basePacks,
                                                         Map<String, SkinPackSourceKind> packSourceKinds,
                                                         SkinPackFamilyRules familyRules,
                                                         Set<String> hiddenDuplicatePackIds) {
        if (basePacks == null || basePacks.isEmpty() || packSourceKinds == null || packSourceKinds.isEmpty() || familyRules == null || !familyRules.hasEntries() || hiddenDuplicatePackIds == null) {
            return;
        }

        HashMap<String, SkinPackSourceKind> preferredSourceByFamily = new HashMap<>();
        HashMap<String, Set<SkinPackSourceKind>> presentSourcesByFamily = new HashMap<>();
        HashMap<String, String> familyByPackId = new HashMap<>();

        for (Map.Entry<String, SkinPack> entry : basePacks.entrySet()) {
            String packId = entry.getKey();
            SkinPack pack = entry.getValue();
            if (packId == null || pack == null || SkinIds.PACK_DEFAULT.equals(packId) || SkinIds.PACK_FAVOURITES.equals(packId)) continue;

            SkinPackSourceKind sourceKind = packSourceKinds.getOrDefault(packId, SkinPackSourceKind.SPECIAL);
            if (sourceKind != SkinPackSourceKind.BOX_MODEL && sourceKind != SkinPackSourceKind.LEGACY_SKINS && sourceKind != SkinPackSourceKind.BEDROCK_SKINS) continue;

            String familyId = familyRules.findFamilyId(sourceKind, packId, nameString(pack.name(), packId));
            if (familyId == null || familyId.isBlank()) continue;

            familyByPackId.put(packId, familyId);
            preferredSourceByFamily.putIfAbsent(familyId, familyRules.preferredSource(familyId));
            presentSourcesByFamily.computeIfAbsent(familyId, ignored -> new LinkedHashSet<>()).add(sourceKind);
        }

        for (Map.Entry<String, String> entry : familyByPackId.entrySet()) {
            String packId = entry.getKey();
            String familyId = entry.getValue();
            SkinPackSourceKind sourceKind = packSourceKinds.getOrDefault(packId, SkinPackSourceKind.SPECIAL);
            SkinPackSourceKind preferredSource = preferredSourceByFamily.get(familyId);
            Set<SkinPackSourceKind> presentSources = presentSourcesByFamily.get(familyId);
            if (preferredSource == null || presentSources == null || !presentSources.contains(preferredSource)) continue;
            if (sourceKind != preferredSource) {
                hiddenDuplicatePackIds.add(packId);
            }
        }
    }

    private static void registerManagedBoxModelNameAlias(Map<String, String> newFormatPackAliases,
                                                         SkinPackOrderRules packOrderRules,
                                                         String packId) {
        if (newFormatPackAliases == null || packOrderRules == null || packId == null || packId.isBlank()) return;
        String managedName = packOrderRules.managedBoxModelName(packId);
        if (managedName == null || managedName.isBlank()) return;
        registerNewFormatPackNameAlias(newFormatPackAliases, managedName, packId);
    }

    private static boolean loadFromManifest(ResourceManager rm,
                                        String namespace,
                                        Map<String, SkinPack> packsOut,
                                        Map<String, SkinEntry> skinsByIdOut,
                                        Map<String, String> packBySkinOut,
                                        Map<String, Integer> packSortIndex,
                                        Set<String> packHasSort,
                                        Map<String, Integer> packInsertIndex,
                                        int[] insertCounter,
                                        Map<String, SkinPackSourceKind> packSourceKinds,
                                        Map<String, Integer> packSourceOrder,
                                        Map<String, String> preferredNewFormatPackAliases,
                                        Map<String, String> preferredNewFormatPackFingerprints,
                                        Map<String, List<String>> preferredNewFormatPackTokens,
                                        SkinPackOrderRules packOrderRules) {
    if (rm == null || namespace == null || namespace.isBlank()) return false;

    ResourceLocation manifestRl = ResourceLocation.fromNamespaceAndPath(namespace, SKINPACKS_PREFIX + "manifest.json");

    List<Resource> resources = rm.getResourceStack(manifestRl);
    if (resources == null || resources.isEmpty()) return false;

    String mode = null;

    LinkedHashMap<String, ManifestPack> byId = new LinkedHashMap<>();
    int manifestOrder = 0;

    for (int r = 0; r < resources.size(); r++) {
        Resource manifestRes = resources.get(r);
        JsonObject manifest = readObj(manifestRes);
        if (manifest == null) continue;

        if (mode == null && manifest.has("order_mode")) {
            String mm = safeString(manifest.get("order_mode"));
            if (mm != null && !mm.isBlank()) mode = mm;
        }

        JsonArray packsArr = manifest.has("packs") && manifest.get("packs").isJsonArray() ? manifest.getAsJsonArray("packs") : null;
        if (packsArr == null || packsArr.isEmpty()) continue;

        for (int i = 0; i < packsArr.size(); i++) {
            JsonElement el = packsArr.get(i);
            if (el == null || !el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();

            String id = o.has("id") ? safeString(o.get("id")) : null;
            if (id == null || id.isBlank()) continue;
            if (id.startsWith("_")) continue;
            if (PackExclusions.isExcluded(id)) continue;

            String name = o.has("name") ? safeString(o.get("name")) : null;
            String author = o.has("author") ? safeString(o.get("author")) : null;
            String path = o.has("path") ? safeString(o.get("path")) : null;
            int sort = o.has("sort_index") ? safeInt(o.get("sort_index"), 0) : 0;
            String type = o.has("type") ? safeString(o.get("type")) : null;

            byId.put(id, new ManifestPack(id, name, author, type, path, sort, manifestOrder++));
        }
    }

    if (byId.isEmpty()) return true;

    ArrayList<ManifestPack> list = new ArrayList<>(byId.values());

    Comparator<ManifestPack> cmp;
    String m = mode == null ? "manifest_order" : mode.toLowerCase(Locale.ROOT).trim();
    if ("sort_index".equals(m) || "sort".equals(m)) {
        cmp = Comparator.comparingInt(ManifestPack::sortIndex).thenComparingInt(ManifestPack::manifestIndex);
    } else if ("alpha".equals(m) || "alphabetical".equals(m)) {
        cmp = Comparator.comparing(ManifestPack::nameOrId, String::compareToIgnoreCase).thenComparingInt(ManifestPack::manifestIndex);
    } else {
        cmp = Comparator.comparingInt(ManifestPack::manifestIndex);
    }
    list.sort(cmp);

    for (ManifestPack mp : list) {
        String packId = mp.id();
        if (packsOut.containsKey(packId)) continue;
        Integer explicitBoxModelOrder = packOrderRules.findBoxModelPackOrder(namespace, packId, BUILTIN_PACK_NAMESPACE);

        String resolved = resolvePackJsonPath(mp.path(), packId);
        ResourceLocation packRl = ResourceLocation.fromNamespaceAndPath(namespace, resolved);
        Resource packRes = rm.getResource(packRl).orElse(null);
        if (packRes == null) continue;

        JsonObject packObj = readObj(packRes);
        if (packObj == null) continue;

        if (mp.name() != null && !mp.name().isBlank() && (!packObj.has("name") || safeString(packObj.get("name")).isBlank())) {
            packObj.addProperty("name", mp.name());
        }
        if (mp.author() != null && !mp.author().isBlank() && (!packObj.has("author") || safeString(packObj.get("author")).isBlank())) {
            packObj.addProperty("author", mp.author());
        }
        if (mp.type() != null && !mp.type().isBlank() && (!packObj.has("type") || safeString(packObj.get("type")).isBlank())) {
            packObj.addProperty("type", mp.type());
        }

        if (!packObj.has("sort_index") && !packObj.has("sort")) {
            packObj.addProperty("sort_index", mp.sortIndex());
        }

        String folder = inferPackFolderFromPackPath(resolved, packId);
        String prefix = resolved.startsWith(DEFAULT_SKINPACKS_PREFIX) ? DEFAULT_SKINPACKS_PREFIX : SKINPACKS_PREFIX;
        boolean hadPack = packsOut.containsKey(packId);
        loadSinglePack(namespace, packId, folder, prefix, packObj, rm, packsOut, skinsByIdOut, packBySkinOut, packSortIndex, packHasSort, packInsertIndex, insertCounter);
        if (!hadPack && packsOut.containsKey(packId)) {
            SkinPackOrderingSupport.registerPackSource(packSourceKinds, packId, SkinPackSourceKind.BOX_MODEL);
            SkinPackOrderingSupport.registerPackSourceOrder(packSourceOrder, packId, explicitBoxModelOrder, packSortIndex, packInsertIndex);
        }
        if (!hadPack && packsOut.containsKey(packId) && BUILTIN_PACK_NAMESPACE.equals(namespace)) {
            if (explicitBoxModelOrder != null) {
                packSortIndex.put(packId, explicitBoxModelOrder);
                packHasSort.add(packId);
            }
            SkinPack pack = packsOut.get(packId);
            registerNewFormatPackAliases(preferredNewFormatPackAliases, namespace, packId, folder, pack.name());
            registerManagedBoxModelNameAlias(preferredNewFormatPackAliases, packOrderRules, packId);
            registerNewFormatPackFingerprint(preferredNewFormatPackFingerprints, pack);
            registerNewFormatPackTokens(preferredNewFormatPackTokens, pack);
        }
    }

    return true;
}

    private static String resolvePackJsonPath(String manifestPath, String packId) {
        if (manifestPath != null && !manifestPath.isBlank()) {
            String p = manifestPath.trim();
            if (p.startsWith("/")) p = p.substring(1);
            if (p.startsWith(SKINPACKS_PREFIX) || p.startsWith(DEFAULT_SKINPACKS_PREFIX)) return p;
            return SKINPACKS_PREFIX + p;
        }
        String id = packId;
        int colon = id != null ? id.indexOf(':') : -1;
        if (colon > 0) id = id.substring(colon + 1);
        return SKINPACKS_PREFIX + id + PACK_JSON_SUFFIX;
    }

    private static String inferPackFolderFromPackPath(String packJsonPath, String packId) {
        if (packJsonPath != null) {
            String p = packJsonPath;
            if (p.startsWith("/")) p = p.substring(1);
            if (p.endsWith(PACK_JSON_SUFFIX)) {
    if (p.startsWith(SKINPACKS_PREFIX)) {
        String mid = p.substring(SKINPACKS_PREFIX.length(), p.length() - PACK_JSON_SUFFIX.length());
        if (!mid.isBlank()) return mid;
    }
    if (p.startsWith(DEFAULT_SKINPACKS_PREFIX)) {
        String mid = p.substring(DEFAULT_SKINPACKS_PREFIX.length(), p.length() - PACK_JSON_SUFFIX.length());
        if (!mid.isBlank()) return mid;
    }
}
        }
        String id = packId;
        int colon = id != null ? id.indexOf(':') : -1;
        if (colon > 0) id = id.substring(colon + 1);
        return id;
    }

    private static String parsePackId(String path) {
    if (path == null) return null;

    String id = parsePackIdForPrefix(path, SKINPACKS_PREFIX);
    if (id != null) return id;
    return parsePackIdForPrefix(path, DEFAULT_SKINPACKS_PREFIX);
}

private static String parsePackIdForPrefix(String path, String prefix) {
    if (path == null || prefix == null) return null;

    if ((prefix + "manifest.json").equals(path)) return null;

    if (path.startsWith(prefix) && path.endsWith(PACK_JSON_SUFFIX)) {
        String p = path.substring(prefix.length(), path.length() - PACK_JSON_SUFFIX.length());
        if (p.indexOf('/') >= 0) return null;
        if (p.startsWith("_")) return null;
        return p;
    }

    if (path.startsWith(prefix) && path.endsWith(".json")) {
        String file = path.substring(prefix.length());
        if (file.indexOf('/') >= 0) return null;
        if ("pack.json".equals(file)) return null;
        if ("manifest.json".equals(file)) return null;
        if (file.startsWith("_")) return null;
        return file.substring(0, file.length() - 5);
    }

    return null;
}

    private static LinkedHashMap<String, SkinPack> withFavourites(Map<String, SkinPack> base, Map<String, SkinEntry> skinsById) {
        ArrayList<SkinEntry> fav = new ArrayList<>();
        for (String id : FavoritesStore.getFavorites()) {
            SkinEntry e = skinsById.get(id);
            if (e != null) fav.add(e);
        }

        ResourceLocation favIcon = DEFAULT_PACK_ICON;
        SkinPack def = base.get(SkinIds.PACK_DEFAULT);
        if (def != null) {
            try {
                if (def.icon() != null) favIcon = def.icon();
            } catch (Throwable ignored) {
            }
        }

        SkinPack favPack = new SkinPack(SkinIds.PACK_FAVOURITES, "key:legacy.favorites.pack", "", "", favIcon, fav);

        LinkedHashMap<String, SkinPack> out = new LinkedHashMap<>();
        if (def != null) out.put(SkinIds.PACK_DEFAULT, def);

        out.put(SkinIds.PACK_FAVOURITES, favPack);

        for (Map.Entry<String, SkinPack> e : base.entrySet()) {
            String k = e.getKey();
            if (SkinIds.PACK_DEFAULT.equals(k)) continue;
            if (SkinIds.PACK_FAVOURITES.equals(k)) continue;
            out.put(k, e.getValue());
        }
        return out;
    }

    private static void loadBedrockSkinPacksIndex(Map<String, SkinPack> packsOut,
                                                  Map<String, SkinEntry> skinsByIdOut,
                                                  Map<String, String> packBySkinOut,
                                                  Map<String, Integer> packSortIndex,
                                                  Set<String> packHasSort,
                                                  Map<String, Integer> packInsertIndex,
                                                  Map<String, Integer> packSortAnchorInsertIndex,
                                                  Map<String, Integer> packSortPriority,
                                                  Map<String, SkinPackSourceKind> packSourceKinds,
                                                  Map<String, Integer> packSourceOrder,
                                                  Set<String> hiddenDuplicatePackIds,
                                                  int[] insertCounter,
                                                  Map<String, String> preferredNewFormatPackAliases,
                                                  Map<String, String> preferredNewFormatPackFingerprints,
                                                  Map<String, List<String>> preferredNewFormatPackTokens,
                                                  boolean hideDuplicateExternalPacks,
                                                  SkinPackOrderRules packOrderRules) {
        List<ExternalSkinPackDescriptor> bedrockPacks = ExternalSkinProviders.loadPackDescriptors(SkinPackSourceKind.BEDROCK_SKINS);
        if (bedrockPacks == null || bedrockPacks.isEmpty()) return;

        for (ExternalSkinPackDescriptor descriptor : bedrockPacks) {
            if (descriptor == null) continue;
            List<ExternalSkinDescriptor> bedrockSkins = descriptor.skins();
            if (bedrockSkins == null || bedrockSkins.isEmpty()) continue;

            ArrayList<SkinEntry> entries = new ArrayList<>(bedrockSkins.size());
            int ordinal = 0;
            for (ExternalSkinDescriptor skin : bedrockSkins) {
                if (skin == null || skin.id() == null || skin.id().isBlank()) continue;
                String name = skin.name();
                if (name == null || name.isBlank()) name = skin.id();
                entries.add(new SkinEntry(skin.id(), name, null, null, false, ++ordinal));
            }
            if (entries.isEmpty()) continue;

            String packId = descriptor.id();
            if (packId == null || packId.isBlank()) continue;
            String packName = descriptor.name();
            if (packName == null || packName.isBlank()) packName = packId;
            String packType = descriptor.type() == null ? "" : descriptor.type();
            Integer explicitBedrockOrder = packOrderRules.findBedrockPackOrder(packId);
            int reservedInsertIndex = packInsertIndex.containsKey(packId) ? packInsertIndex.get(packId) : insertCounter[0]++;
            boolean hiddenDuplicate = false;

            String preferredPackId = SkinPackDuplicateResolver.findNewFormatDuplicatePackId(
                    null,
                    packId,
                    packName,
                    entries,
                    preferredNewFormatPackAliases,
                    preferredNewFormatPackFingerprints,
                    preferredNewFormatPackTokens
            );

            if (explicitBedrockOrder != null) {
                SkinPackOrderingSupport.applyManagedPackFamilyOrder(preferredPackId, explicitBedrockOrder, packSortIndex, packHasSort, packOrderRules);
            }

            if (hideDuplicateExternalPacks && preferredPackId != null) {
                DebugLog.debug("Keeping duplicate bedrock pack {} hidden behind preferred new-format pack {}", packId, preferredPackId);
                hiddenDuplicate = true;
            }

            packsOut.put(packId, new SkinPack(packId, packName, "", packType, descriptor.icon(), entries));
            SkinPackOrderingSupport.registerPackSource(packSourceKinds, packId, SkinPackSourceKind.BEDROCK_SKINS);
            SkinPackOrderingSupport.registerPackSourceOrder(packSourceOrder, packId, descriptor.nativeOrder(), null, Map.of(packId, reservedInsertIndex));
            for (SkinEntry entry : entries) {
                skinsByIdOut.put(entry.id(), entry);
                packBySkinOut.put(entry.id(), packId);
            }

            if (explicitBedrockOrder != null) {
                packSortIndex.put(packId, explicitBedrockOrder);
                packHasSort.add(packId);
            } else if (preferredPackId != null) {
                SkinPackOrderingSupport.inheritPreferredPackSort(packId, preferredPackId, packSortIndex, packHasSort, packInsertIndex, packSortAnchorInsertIndex, packSortPriority, reservedInsertIndex);
            } else {
                Integer externalSortIndex = SkinPackDuplicateResolver.findExternalPackSortIndexOverride(null, packId, packName);
                if (externalSortIndex == null) {
                    Integer descriptorSortIndex = descriptor.nativeOrder();
                    externalSortIndex = EXTERNAL_PACK_SORT_BASE + (descriptorSortIndex == null ? reservedInsertIndex : descriptorSortIndex);
                }
                packSortIndex.put(packId, externalSortIndex);
                packHasSort.add(packId);
            }
            packInsertIndex.put(packId, reservedInsertIndex);
            if (hiddenDuplicate) hiddenDuplicatePackIds.add(packId);
            else hiddenDuplicatePackIds.remove(packId);
        }
    }

    private static void loadLegacySkinPacksIndex(ResourceManager rm,
                                                 Map<String, SkinPack> packsOut,
                                                 Map<String, SkinEntry> skinsByIdOut,
                                                 Map<String, String> packBySkinOut,
                                                 Map<String, Integer> packSortIndex,
                                                 Set<String> packHasSort,
                                                 Map<String, Integer> packInsertIndex,
                                                 Map<String, Integer> packSortAnchorInsertIndex,
                                                 Map<String, Integer> packSortPriority,
                                                 Map<String, SkinPackSourceKind> packSourceKinds,
                                                 Map<String, Integer> packSourceOrder,
                                                 Set<String> hiddenDuplicatePackIds,
                                                 int[] insertCounter,
                                                 Map<String, String> preferredNewFormatPackAliases,
                                                 Map<String, String> preferredNewFormatPackFingerprints,
                                                 Map<String, List<String>> preferredNewFormatPackTokens,
                                                 boolean hideDuplicateExternalLegacyPacks,
                                                 SkinPackOrderRules packOrderRules) {
        LegacySkinsCompat.clearRegisteredSkins();

        HashMap<String, List<String>> legacySkinIdsByPack = new HashMap<>();
        ArrayList<String> namespaces = new ArrayList<>(rm.getNamespaces());
        namespaces.sort(Comparator.comparingInt((String s) -> "legacyskins".equals(s) ? 0 : 1).thenComparing(String::compareTo));

        for (String ns : namespaces) {
            loadLegacySkinPackFile(rm, ns, "skin_packs.json", packsOut, skinsByIdOut, packBySkinOut, packSortIndex, packHasSort, packInsertIndex, packSortAnchorInsertIndex, packSortPriority, packSourceKinds, packSourceOrder, hiddenDuplicatePackIds, insertCounter, legacySkinIdsByPack, preferredNewFormatPackAliases, preferredNewFormatPackFingerprints, preferredNewFormatPackTokens, hideDuplicateExternalLegacyPacks, packOrderRules);
            loadLegacySkinPackFile(rm, ns, "skin_packs2.json", packsOut, skinsByIdOut, packBySkinOut, packSortIndex, packHasSort, packInsertIndex, packSortAnchorInsertIndex, packSortPriority, packSourceKinds, packSourceOrder, hiddenDuplicatePackIds, insertCounter, legacySkinIdsByPack, preferredNewFormatPackAliases, preferredNewFormatPackFingerprints, preferredNewFormatPackTokens, hideDuplicateExternalLegacyPacks, packOrderRules);
        }

        LegacySkinsCompat.importCurrentSelectionIfAbsent();
    }

    private static void loadLegacySkinPackFile(ResourceManager rm,
                                               String namespace,
                                               String fileName,
                                               Map<String, SkinPack> packsOut,
                                               Map<String, SkinEntry> skinsByIdOut,
                                               Map<String, String> packBySkinOut,
                                               Map<String, Integer> packSortIndex,
                                               Set<String> packHasSort,
                                               Map<String, Integer> packInsertIndex,
                                               Map<String, Integer> packSortAnchorInsertIndex,
                                               Map<String, Integer> packSortPriority,
                                               Map<String, SkinPackSourceKind> packSourceKinds,
                                               Map<String, Integer> packSourceOrder,
                                               Set<String> hiddenDuplicatePackIds,
                                               int[] insertCounter,
                                               Map<String, List<String>> legacySkinIdsByPack,
                                               Map<String, String> preferredNewFormatPackAliases,
                                               Map<String, String> preferredNewFormatPackFingerprints,
                                               Map<String, List<String>> preferredNewFormatPackTokens,
                                               boolean hideDuplicateExternalLegacyPacks,
                                               SkinPackOrderRules packOrderRules) {
        if (rm == null || namespace == null || namespace.isBlank() || fileName == null || fileName.isBlank()) return;

        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(namespace, fileName);
        Resource res = rm.getResource(rl).orElse(null);
        if (res == null) return;

        try {
            JsonObject root = readObj(res);
            if (root == null) return;
            ArrayList<SkinPackOrderingSupport.LegacyPackCandidate> pendingPacks = new ArrayList<>();

            for (Map.Entry<String, JsonElement> pe : root.entrySet()) {
                if (!pe.getValue().isJsonObject()) continue;

                ResourceLocation packLoc = parseLegacyPackLocation(namespace, pe.getKey());
                if (packLoc == null) continue;

                String packId = packLoc.getNamespace() + ":" + packLoc.getPath();
                Integer explicitLegacyOrder = packOrderRules.findLegacyPackOrder(packId);
                boolean legacyOwned = legacySkinIdsByPack.containsKey(packId);
                if (!legacyOwned && packsOut.containsKey(packId)) continue;
                if (PackExclusions.isExcluded(packId) || SkinIdUtil.containsMinecon(packLoc.getPath())) continue;

                JsonObject obj = pe.getValue().getAsJsonObject();
                collectPoseTagsFromPackJson(namespace, obj);

                String fallbackPackName = tr("skin_pack." + packLoc.getNamespace() + "." + packLoc.getPath(), packLoc.getPath());
                String name = trMaybeKey(safeString(obj.get("name")), fallbackPackName);
                String author = safeString(obj.get("author"));
                if (author == null) author = "";
                String type = safeString(obj.get("type"));
                if (type == null) type = "";

                if (LegacySkinsCompat.shouldSkipPack(packLoc, type)) {
                    if (legacyOwned) {
                        cleanupLegacyPackEntries(packId, skinsByIdOut, packBySkinOut, legacySkinIdsByPack);
                        packsOut.remove(packId);
                        packSortIndex.remove(packId);
                        packHasSort.remove(packId);
                        packInsertIndex.remove(packId);
                        packSortAnchorInsertIndex.remove(packId);
                        packSortPriority.remove(packId);
                    }
                    continue;
                }

                ResourceLocation icon = DEFAULT_PACK_ICON;
                String iconPath = safeString(obj.get("icon"));
                if (iconPath != null && !iconPath.isBlank()) {
                    try {
                        icon = ResourceLocation.parse(iconPath);
                    } catch (Throwable ignored) {
                        icon = DEFAULT_PACK_ICON;
                    }
                }

                JsonArray skinsArr = obj.has("skins") && obj.get("skins").isJsonArray() ? obj.getAsJsonArray("skins") : null;
                if (skinsArr == null) continue;

                String packFolder = packLoc.getPath();
                ArrayList<SkinEntry> entries = new ArrayList<>();
                ArrayList<String> insertedIds = new ArrayList<>();
                HashMap<String, Integer> legacyRuntimeOrdinals = new HashMap<>();

                for (int i = 0; i < skinsArr.size(); i++) {
                    JsonElement se = skinsArr.get(i);
                    if (!se.isJsonObject()) continue;
                    JsonObject so = se.getAsJsonObject();

                    int runtimeOrdinal = LegacySkinsCompat.runtimeOrdinal(packLoc, i);

                    if (so.has("id") && so.has("texture")) {
                        String skinId = safeString(so.get("id"));
                        if (skinId == null || skinId.isBlank()) continue;

                        String fallbackName = legacyFallbackSkinName(null, packLoc, runtimeOrdinal);
                        String skinName = trMaybeKey(safeString(so.get("name")),
                                tr("skin_pack." + packLoc.getNamespace() + "." + packLoc.getPath() + "." + runtimeOrdinal, fallbackName));
                        int order = safeInt(so.get("order"), i + 1);

                        String texPath = safeString(so.get("texture"));
                        if (texPath == null || texPath.isBlank()) continue;
                        ResourceLocation tex = parseLegacyTextureLocation(namespace, packFolder, texPath);
                        if (tex == null) continue;

                        SkinEntry entry = new SkinEntry(skinId, skinName, tex, null, false, order);
                        entries.add(entry);
                        insertedIds.add(entry.id());
                        collectPoseTagsFromSkinJson(so, skinId);
                        continue;
                    }

                    String modelPath = safeString(so.get("model"));
                    if (modelPath == null || modelPath.isBlank()) continue;

                    String modelType = safeString(so.get("modelType"));
                    if (modelType == null || modelType.isBlank()) modelType = "cpm";
                    if (!"cpm".equalsIgnoreCase(modelType.trim())) continue;
                    if (!LegacySkinsCompat.isSkinUsable(packLoc, runtimeOrdinal)) continue;

                    String skinId = LegacySkinsCompat.syntheticSkinId(packLoc, runtimeOrdinal);
                    if (skinId == null || skinId.isBlank()) continue;

                    String fallbackName = legacyFallbackSkinName(modelPath, packLoc, runtimeOrdinal);
                    String skinName = trMaybeKey(safeString(so.get("name")),
                            tr("skin_pack." + packLoc.getNamespace() + "." + packLoc.getPath() + "." + runtimeOrdinal, fallbackName));
                    int order = safeInt(so.get("order"), i + 1);

                    SkinEntry entry = new SkinEntry(skinId, skinName, null, null, false, order);
                    entries.add(entry);
                    insertedIds.add(entry.id());
                    legacyRuntimeOrdinals.put(entry.id(), runtimeOrdinal);
                }

                String preferredPackId = null;
                if (!BUILTIN_PACK_NAMESPACE.equals(packLoc.getNamespace())) {
                    preferredPackId = SkinPackDuplicateResolver.findNewFormatDuplicatePackId(
                            packLoc,
                            packId,
                            name,
                            entries,
                            preferredNewFormatPackAliases,
                            preferredNewFormatPackFingerprints,
                            preferredNewFormatPackTokens
                    );
                }

                if (explicitLegacyOrder != null) {
                    SkinPackOrderingSupport.applyManagedPackFamilyOrder(preferredPackId, explicitLegacyOrder, packSortIndex, packHasSort, packOrderRules);
                }

                int reservedInsertIndex = packInsertIndex.containsKey(packId) ? packInsertIndex.get(packId) : insertCounter[0]++;
                if (hideDuplicateExternalLegacyPacks && preferredPackId != null) {
                    if (legacyOwned) {
                        cleanupLegacyPackEntries(packId, skinsByIdOut, packBySkinOut, legacySkinIdsByPack);
                        packsOut.remove(packId);
                    }
                    packSortIndex.remove(packId);
                    packHasSort.remove(packId);
                    packInsertIndex.remove(packId);
                    packSortAnchorInsertIndex.remove(packId);
                    packSortPriority.remove(packId);
                    pendingPacks.add(new SkinPackOrderingSupport.LegacyPackCandidate(
                            packLoc,
                            packId,
                            name,
                            author,
                            type,
                            icon,
                            List.copyOf(entries),
                            List.copyOf(insertedIds),
                            Map.copyOf(legacyRuntimeOrdinals),
                            preferredPackId,
                            null,
                            reservedInsertIndex,
                            reservedInsertIndex,
                            true
                    ));
                    continue;
                }

                if (legacyOwned) {
                    cleanupLegacyPackEntries(packId, skinsByIdOut, packBySkinOut, legacySkinIdsByPack);
                }
                packsOut.remove(packId);
                packSortAnchorInsertIndex.remove(packId);
                packSortPriority.remove(packId);

                if (entries.isEmpty()) {
                    packSortIndex.remove(packId);
                    packHasSort.remove(packId);
                    packInsertIndex.remove(packId);
                    packSortAnchorInsertIndex.remove(packId);
                    packSortPriority.remove(packId);
                    continue;
                }

                pendingPacks.add(new SkinPackOrderingSupport.LegacyPackCandidate(
                        packLoc,
                        packId,
                        name,
                        author,
                        type,
                        icon,
                        List.copyOf(entries),
                        List.copyOf(insertedIds),
                        Map.copyOf(legacyRuntimeOrdinals),
                        preferredPackId,
                        explicitLegacyOrder,
                        reservedInsertIndex,
                        reservedInsertIndex,
                        false
                ));
            }

            SkinPackOrderingSupport.applyLegacyPackSortMetadata(pendingPacks, packSortIndex, packHasSort, packInsertIndex, packSortAnchorInsertIndex, packSortPriority);

            for (SkinPackOrderingSupport.LegacyPackCandidate candidate : pendingPacks) {
                String packId = candidate.packId();
                packInsertIndex.put(packId, candidate.insertIndex());
                packsOut.put(packId, new SkinPack(packId, candidate.name(), candidate.author(), candidate.type(), candidate.icon(), candidate.entries()));
                SkinPackOrderingSupport.registerPackSource(packSourceKinds, packId, SkinPackSourceKind.LEGACY_SKINS);
                SkinPackOrderingSupport.registerPackSourceOrder(packSourceOrder, packId, candidate.sourceOrder(), null, Map.of(packId, candidate.insertIndex()));
                legacySkinIdsByPack.put(packId, candidate.insertedIds());
                if (candidate.hiddenDuplicate()) hiddenDuplicatePackIds.add(packId);
                else hiddenDuplicatePackIds.remove(packId);

                for (SkinEntry entry : candidate.entries()) {
                    skinsByIdOut.put(entry.id(), entry);
                    packBySkinOut.put(entry.id(), packId);
                }

                for (Map.Entry<String, Integer> legacySkin : candidate.legacyRuntimeOrdinals().entrySet()) {
                    LegacySkinsCompat.registerLegacySkin(legacySkin.getKey(), candidate.packLoc(), legacySkin.getValue());
                }
            }
        } catch (Throwable ex) {
            DebugLog.warn("LegacySkins pack import failed for {}:{}: {}", namespace, fileName, ex.toString());
        }
    }
    private static void registerNewFormatPackAliases(Map<String, String> newFormatPackAliases,
                                                     String namespace,
                                                     String packId,
                                                     String packFolder,
                                                     String packName) {
        if (newFormatPackAliases == null || packId == null || packId.isBlank()) return;

        registerNewFormatPackAlias(newFormatPackAliases, packId, packId);

        String strippedPackId = SkinPackDuplicateResolver.stripNamespace(packId);
        if (strippedPackId != null && !strippedPackId.isBlank()) {
            registerNewFormatPackAlias(newFormatPackAliases, strippedPackId, packId);
            String namespacedIdAlias = SkinPackDuplicateResolver.namespaceOf(packId);
            if (namespacedIdAlias == null || namespacedIdAlias.isBlank()) namespacedIdAlias = namespace;
            if (namespacedIdAlias != null && !namespacedIdAlias.isBlank()) {
                registerNewFormatPackAlias(newFormatPackAliases, namespacedIdAlias + ":" + strippedPackId, packId);
            }
        }

        if (packFolder != null && !packFolder.isBlank()) {
            registerNewFormatPackAlias(newFormatPackAliases, packFolder, packId);
            if (namespace != null && !namespace.isBlank()) {
                registerNewFormatPackAlias(newFormatPackAliases, namespace + ":" + packFolder, packId);
            }
        }

        registerNewFormatPackNameAlias(newFormatPackAliases, packName, packId);
    }

    private static void registerNewFormatPackFingerprint(Map<String, String> preferredNewFormatPackFingerprints,
                                                         SkinPack pack) {
        if (preferredNewFormatPackFingerprints == null || pack == null) return;
        for (String fingerprint : SkinPackDuplicateResolver.buildDuplicatePackFingerprints(nameString(pack.name(), pack.id()), pack.skins())) {
            preferredNewFormatPackFingerprints.putIfAbsent(fingerprint, pack.id());
        }
    }

    private static void registerNewFormatPackTokens(Map<String, List<String>> preferredNewFormatPackTokens,
                                                    SkinPack pack) {
        if (preferredNewFormatPackTokens == null || pack == null) return;
        List<String> tokens = SkinPackDuplicateResolver.buildDuplicatePackTokens(pack.skins());
        if (tokens.isEmpty()) return;
        preferredNewFormatPackTokens.putIfAbsent(pack.id(), List.copyOf(tokens));
    }

    private static void registerNewFormatPackAlias(Map<String, String> newFormatPackAliases,
                                                   String alias,
                                                   String packId) {
        if (newFormatPackAliases == null || packId == null || packId.isBlank()) return;
        for (String aliasKey : SkinPackDuplicateResolver.buildDuplicatePackAliasKeys(alias)) {
            newFormatPackAliases.putIfAbsent(aliasKey, packId);
        }
    }

    private static void registerNewFormatPackNameAlias(Map<String, String> newFormatPackAliases,
                                                       String packName,
                                                       String packId) {
        String normalizedPackName = SkinPackDuplicateResolver.normalizeDuplicatePackNameKey(nameString(packName, packId));
        if (normalizedPackName == null) return;
        newFormatPackAliases.putIfAbsent("name:" + normalizedPackName, packId);
    }

    private static void cleanupLegacyPackEntries(String packId,
                                                 Map<String, SkinEntry> skinsByIdOut,
                                                 Map<String, String> packBySkinOut,
                                                 Map<String, List<String>> legacySkinIdsByPack) {
        List<String> oldIds = legacySkinIdsByPack.remove(packId);
        if (oldIds == null || oldIds.isEmpty()) return;
        for (String oldId : oldIds) {
            if (oldId == null || oldId.isBlank()) continue;
            skinsByIdOut.remove(oldId);
            packBySkinOut.remove(oldId);
            LegacySkinsCompat.unregisterLegacySkin(oldId);
        }
    }

    private static ResourceLocation parseLegacyPackLocation(String namespace, String rawPackId) {
        if (namespace == null || namespace.isBlank() || rawPackId == null || rawPackId.isBlank()) return null;
        try {
            return rawPackId.indexOf(':') > 0
                    ? ResourceLocation.parse(rawPackId)
                    : ResourceLocation.fromNamespaceAndPath(namespace, rawPackId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ResourceLocation parseLegacyTextureLocation(String namespace, String packFolder, String texturePath) {
        if (namespace == null || namespace.isBlank() || texturePath == null || texturePath.isBlank()) return null;
        try {
            ResourceLocation tex = texturePath.indexOf(':') > 0
                    ? ResourceLocation.parse(texturePath)
                    : ResourceLocation.fromNamespaceAndPath(namespace, texturePath);
            if (tex.getPath().startsWith(SKINPACKS_PREFIX)) return tex;
            String folder = packFolder == null ? "" : packFolder;
            return ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), SKINPACKS_PREFIX + folder + "/" + tex.getPath());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String legacyFallbackSkinName(String modelPath, ResourceLocation packLoc, int runtimeOrdinal) {
        if (modelPath != null && !modelPath.isBlank()) {
            try {
                ResourceLocation modelLoc = modelPath.indexOf(':') > 0
                        ? ResourceLocation.parse(modelPath)
                        : ResourceLocation.fromNamespaceAndPath(packLoc.getNamespace(), modelPath);
                String path = modelLoc.getPath();
                int slash = path.lastIndexOf('/');
                if (slash >= 0 && slash + 1 < path.length()) path = path.substring(slash + 1);
                if (path.endsWith(".cpmmodel")) path = path.substring(0, path.length() - ".cpmmodel".length());
                path = path.replace('_', ' ').replace('-', ' ').trim();
                if (!path.isBlank()) return path;
            } catch (Throwable ignored) {
            }
        }
        if (packLoc == null) return "legacy skin #" + runtimeOrdinal;
        return packLoc.getPath() + " #" + runtimeOrdinal;
    }

    private static void loadSinglePack(String namespace, String packId, String packFolder, String packPrefix, JsonObject json, ResourceManager rm,
                                       Map<String, SkinPack> packsOut,
                                       Map<String, SkinEntry> skinsByIdOut,
                                       Map<String, String> packBySkinOut,
                                       Map<String, Integer> packSortIndex,
                                       Set<String> packHasSort,
                                       Map<String, Integer> packInsertIndex,
                                       int[] insertCounter) {

        collectPoseTagsFromPackJson(namespace, json);
        String name = json.has("name") ? json.get("name").getAsString() : packId;
        String author = json.has("author") ? json.get("author").getAsString() : "";
        String type = json.has("type") ? json.get("type").getAsString() : "";
        if (name.startsWith("key:")) name = tr(name.substring(4), packId);

        JsonArray skins = json.getAsJsonArray("skins");
        ArrayList<SkinEntryWithIndex> tmp = new ArrayList<>();

        if (skins != null) {
            for (int i = 0; i < skins.size(); i++) {
                JsonElement e = skins.get(i);
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                if (!o.has("id") || !o.has("texture")) continue;

                String skinId = o.get("id").getAsString();
                String skinName = o.has("name") ? o.get("name").getAsString() : skinId;
                if (skinName.startsWith("key:")) skinName = tr(skinName.substring(4), skinId);
                int order = o.has("order") ? o.get("order").getAsInt() : (i + 1);
                String texPath = o.get("texture").getAsString();
                String capePath = o.has("cape") ? o.get("cape").getAsString() : null;

                boolean slimArms = false;
                boolean modelExplicit = false;

                if (o.has("slim") && o.get("slim").isJsonPrimitive()) {
                    try { slimArms = o.get("slim").getAsBoolean(); modelExplicit = true; } catch (Throwable ignored) {}
                }

                String modelStr = null;
                if (o.has("model")) {
                    try { modelStr = o.get("model").getAsString(); modelExplicit = true; } catch (Throwable ignored) {}
                } else if (o.has("arms")) {
                    try { modelStr = o.get("arms").getAsString(); modelExplicit = true; } catch (Throwable ignored) {}
                }
                if (modelStr != null) {
                    String m = modelStr.trim().toLowerCase(java.util.Locale.ROOT);
                    if (m.equals("slim") || m.equals("alex")) slimArms = true;
                    if (m.equals("wide") || m.equals("default") || m.equals("steve")) slimArms = false;
                }

                if (!modelExplicit && SkinIds.PACK_DEFAULT.equals(packId)) {
                    String sidLower = skinId.toLowerCase(java.util.Locale.ROOT);
                    String tpLower = texPath.toLowerCase(java.util.Locale.ROOT);
                    if (sidLower.contains("alex") || tpLower.contains("alex")) slimArms = true;
                }

                collectPoseTagsFromSkinJson(o, skinId);
                  ResourceLocation texture;
if (texPath.startsWith(SKINPACKS_PREFIX) || texPath.startsWith(DEFAULT_SKINPACKS_PREFIX)) {
    texture = ResourceLocation.fromNamespaceAndPath(namespace, texPath);
} else {
    texture = ResourceLocation.fromNamespaceAndPath(namespace, packPrefix + packFolder + "/" + texPath);
}
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
            skinsByIdOut.putIfAbsent(entry.id(), entry);
            packBySkinOut.putIfAbsent(entry.id(), packId);
        }

        if (entries.isEmpty()) return;

        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(namespace, packPrefix + packFolder + "/pack.png");
                  if (rm.getResource(icon).isEmpty()) {
    ResourceLocation alt = ResourceLocation.fromNamespaceAndPath(namespace, DEFAULT_SKINPACKS_PREFIX + packFolder + "/pack.png");
    if (!DEFAULT_SKINPACKS_PREFIX.equals(packPrefix) && rm.getResource(alt).isPresent()) icon = alt;
    else icon = DEFAULT_PACK_ICON;
}

        packsOut.put(packId, new SkinPack(packId, name, author, type, icon, entries));

        boolean has = json.has("sort_index") || json.has("sort");
        if (has) {
            int sort = json.has("sort_index") ? safeInt(json.get("sort_index"), 0) : safeInt(json.get("sort"), 0);
            packSortIndex.put(packId, sort);
            packHasSort.add(packId);
        }
        packInsertIndex.putIfAbsent(packId, insertCounter[0]++);

    }

    private static void applyBuiltinAutoSelection(ResourceManager rm, Map<String, SkinPack> ordered) {
        if (LAST_USED_CUSTOM_PACK_ID != null) {
            if (ordered.containsKey(LAST_USED_CUSTOM_PACK_ID)) return;
            LAST_USED_CUSTOM_PACK_ID = null;
            ConsoleSkinsClientSettings.setLastUsedCustomPackId(null);
            return;
        }
        if (rm == null || rm.getResource(BUILTIN_PACK_MANIFEST).isEmpty()) return;
        try {
            Resource res = rm.getResource(BUILTIN_PACK_MANIFEST).orElse(null);
            if (res == null) return;
            JsonObject manifest = readObj(res);
            if (manifest == null) return;
            JsonArray packsArr = manifest.has("packs") && manifest.get("packs").isJsonArray() ? manifest.getAsJsonArray("packs") : null;
            if (packsArr == null || packsArr.isEmpty()) return;
            String mode = manifest.has("order_mode") ? safeString(manifest.get("order_mode")) : "manifest_order";
            ArrayList<ManifestPack> list = new ArrayList<>(packsArr.size());
            for (int i = 0; i < packsArr.size(); i++) {
                JsonElement el = packsArr.get(i);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String id = o.has("id") ? safeString(o.get("id")) : null;
                if (id == null || id.isBlank()) continue;
                if (id.startsWith("_")) continue;
                int sort = o.has("sort_index") ? safeInt(o.get("sort_index"), 0) : 0;
                String name = o.has("name") ? safeString(o.get("name")) : null;
                list.add(new ManifestPack(id, name, null, null, null, sort, i));
            }
            if (list.isEmpty()) return;
            Comparator<ManifestPack> cmp;
            String m = mode == null ? "" : mode.toLowerCase(Locale.ROOT).trim();
            if ("sort_index".equals(m) || "sort".equals(m)) {
                cmp = Comparator.comparingInt(ManifestPack::sortIndex).thenComparingInt(ManifestPack::manifestIndex);
            } else if ("alpha".equals(m) || "alphabetical".equals(m)) {
                cmp = Comparator.comparing(ManifestPack::nameOrId, String::compareToIgnoreCase).thenComparingInt(ManifestPack::manifestIndex);
            } else {
                cmp = Comparator.comparingInt(ManifestPack::manifestIndex);
            }
            list.sort(cmp);
            String target = list.get(0).id();
            if (target != null && ordered.containsKey(target)) {
                LAST_USED_CUSTOM_PACK_ID = target;
                ConsoleSkinsClientSettings.setLastUsedCustomPackId(target);
            }
        } catch (Throwable ignored) {
        }
    }
    private static void collectPoseTagsFromPackJson(String namespace, JsonObject packObj) {
        if (packObj == null) return;
        JsonObject poses = null;
        if (packObj.has("poses") && packObj.get("poses").isJsonObject()) poses = packObj.getAsJsonObject("poses");
        else if (packObj.has("animations") && packObj.get("animations").isJsonObject()) poses = packObj.getAsJsonObject("animations");
        if (poses == null) return;

        for (Map.Entry<String, JsonElement> e : poses.entrySet()) {
            SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(e.getKey());
            if (tag == null) continue;
            JsonElement v = e.getValue();
            if (v == null) continue;

            if (v.isJsonArray()) {
                JsonArray arr = v.getAsJsonArray();
                for (int i = 0; i < arr.size(); i++) {
                    JsonElement el = arr.get(i);
                    if (el != null && el.isJsonPrimitive()) {
                        try {
                            SkinPoseRegistry.addSelector(tag, el.getAsString(), namespace);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } else if (v.isJsonPrimitive()) {
                try {
                    SkinPoseRegistry.addSelector(tag, v.getAsString(), namespace);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void collectPoseTagsFromSkinJson(JsonObject skinObj, String skinId) {
        if (skinObj == null || skinId == null || skinId.isBlank()) return;
        JsonElement poses = null;
        if (skinObj.has("poses")) poses = skinObj.get("poses");
        else if (skinObj.has("animations")) poses = skinObj.get("animations");
        if (poses == null) return;

        if (poses.isJsonArray()) {
            JsonArray arr = poses.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement el = arr.get(i);
                if (el != null && el.isJsonPrimitive()) {
                    SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(el.getAsString());
                    if (tag != null) SkinPoseRegistry.addSelector(tag, skinId, null);
                }
            }
        } else if (poses.isJsonPrimitive()) {
            SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(poses.getAsString());
            if (tag != null) SkinPoseRegistry.addSelector(tag, skinId, null);
        }
    }

    private static JsonObject readObj(Resource res) {
        try (Reader r = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
            JsonReader jr = new JsonReader(r);
            jr.setLenient(true);
            JsonElement e = JsonParser.parseReader(jr);
            return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String tr(String key, String fallback) {
        try {
            String ev = SkinPackLang.get(key);
            if (ev != null && !ev.isEmpty() && !ev.equals(key)) return ev;
            String v = I18n.get(key);
            return v == null || v.isEmpty() || v.equals(key) ? fallback : v;
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static String trOpt(String key) {
        if (key == null || key.isBlank()) return null;
        try {
            String ev = SkinPackLang.get(key);
            if (ev != null && !ev.isEmpty() && !ev.equals(key)) return ev;
            String v = I18n.get(key);
            return v == null || v.isEmpty() || v.equals(key) ? null : v;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String trMaybeKey(String name, String fallback) {
        if (name == null || name.isBlank()) return fallback;
        String key = name.startsWith("key:") ? name.substring(4) : name;
        String v = trOpt(key);
        if (v != null) return v;
        int us = key.lastIndexOf('_');
        if (us > 0) {
            boolean digits = true;
            for (int i = us + 1; i < key.length(); i++) {
                char c = key.charAt(i);
                if (c < '0' || c > '9') { digits = false; break; }
            }
            if (digits) {
                String base = key.substring(0, us);
                v = trOpt(base);
                if (v != null) return v;
            }
        }
        return name.startsWith("key:") ? fallback : name;
    }

    private static String safeString(JsonElement el) {
        if (el == null || !el.isJsonPrimitive()) return null;
        try {
            return el.getAsString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int safeInt(JsonElement el, int def) {
        if (el == null || !el.isJsonPrimitive()) return def;
        try {
            return el.getAsInt();
        } catch (Throwable ignored) {
            return def;
        }
    }

    private record SkinEntryWithIndex(SkinEntry entry, int index) {
    }

    private record ManifestPack(String id, String name, String author, String type, String path, int sortIndex, int manifestIndex) {
        private String nameOrId() {
            return name == null || name.isBlank() ? id : name;
        }
    }

    public static String nameString(String name, String fallbackId) {
    return trMaybeKey(name, fallbackId);
}

public static Component nameComponent(String name, String fallbackId) {
    return Component.literal(nameString(name, fallbackId));
}

public static boolean packContainsSkinId(String packId, String skinId) {
    ensureLoaded();
    if (packId == null || skinId == null) return false;
    return packId.equals(PACK_BY_SKIN.get(skinId));
}
}
