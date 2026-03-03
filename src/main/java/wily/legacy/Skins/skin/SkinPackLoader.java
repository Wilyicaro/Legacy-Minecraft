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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkinPackLoader {

    private static final Object LOCK = new Object();

    private static volatile Map<String, SkinPack> PACKS = Map.of();
    private static volatile Map<String, SkinEntry> SKINS_BY_ID = Map.of();
    private static volatile Map<String, String> PACK_BY_SKIN = Map.of();
    private static volatile boolean loaded;

    private static volatile String LAST_USED_CUSTOM_PACK_ID;

    private static final ResourceLocation DEFAULT_PACK_ICON = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "skinpacks/default/pack.png");

    private static final String BUILTIN_PACK_NAMESPACE = "lce_skinpacks";
    private static final ResourceLocation BUILTIN_PACK_MANIFEST = ResourceLocation.fromNamespaceAndPath(BUILTIN_PACK_NAMESPACE, "skinpacks/manifest.json");

    private static final String SKINPACKS_PREFIX = "skinpacks/";
    private static final String PACK_JSON_SUFFIX = "/pack.json";

    private SkinPackLoader() {
    }

    public static Map<String, SkinPack> getPacks() {
        ensureLoaded();
        return PACKS;
    }

    public static SkinEntry getSkin(String id) {
        ensureLoaded();
        return SKINS_BY_ID.get(id);
    }

    public static String getSourcePackId(String skinId) {
        ensureLoaded();
        return PACK_BY_SKIN.get(skinId);
    }

    public static String getLastUsedCustomPackId() {
        return LAST_USED_CUSTOM_PACK_ID;
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
        Map<String, SkinPack> cur = PACKS;
        LinkedHashMap<String, SkinPack> base = new LinkedHashMap<>(cur);
        LinkedHashMap<String, SkinPack> ordered = withFavourites(base, SKINS_BY_ID);
        synchronized (LOCK) {
            PACKS = Collections.unmodifiableMap(ordered);
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
        LinkedHashMap<String, SkinEntry> skinsById = new LinkedHashMap<>();
        LinkedHashMap<String, String> packBySkin = new LinkedHashMap<>();

        try {
            List<String> namespaces = new ArrayList<>(rm.getNamespaces());
            namespaces.sort(String::compareTo);

            boolean anyManifest = false;
            for (String ns : namespaces) {
                if (loadFromManifest(rm, ns, packs, skinsById, packBySkin)) anyManifest = true;
            }

            try {
                Map<ResourceLocation, Resource> jsons = rm.listResources("skinpacks", rl -> rl.getPath().endsWith(".json"));
                ArrayList<ResourceLocation> keys = new ArrayList<>(jsons.keySet());
                keys.sort(Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath));

                for (ResourceLocation rl : keys) {
                    String ns = rl.getNamespace();
                    String path = rl.getPath();

                    String packId = parsePackId(path);
                    if (packId == null || packId.isEmpty()) continue;
                    if (packs.containsKey(packId)) continue;
                    if (PackExclusions.isExcluded(packId)) continue;

                    if (anyManifest && packId.startsWith("_")) continue;

                    JsonObject json = readObj(jsons.get(rl));
                    if (json != null) loadSinglePack(ns, packId, packId, json, rm, packs, skinsById, packBySkin);
                }
            } catch (Throwable ex) {
                DebugLog.warn("ResourceManager skinpack scan failed: {}", ex.toString());
            }

            loadLegacySkinPacksIndex(rm, packs, skinsById, packBySkin);
        } finally {
            SkinPoseRegistry.endReload();
        }

        LinkedHashMap<String, SkinPack> ordered = withFavourites(packs, skinsById);
        applyBuiltinAutoSelection(rm, ordered);
        synchronized (LOCK) {
            PACKS = Collections.unmodifiableMap(ordered);
            SKINS_BY_ID = Collections.unmodifiableMap(skinsById);
            PACK_BY_SKIN = Collections.unmodifiableMap(packBySkin);
            loaded = true;
        }
    }

    private static boolean loadFromManifest(ResourceManager rm,
                                            String namespace,
                                            Map<String, SkinPack> packsOut,
                                            Map<String, SkinEntry> skinsByIdOut,
                                            Map<String, String> packBySkinOut) {
        if (rm == null || namespace == null || namespace.isBlank()) return false;

        ResourceLocation manifestRl = ResourceLocation.fromNamespaceAndPath(namespace, SKINPACKS_PREFIX + "manifest.json");
        Resource manifestRes = rm.getResource(manifestRl).orElse(null);
        if (manifestRes == null) return false;

        JsonObject manifest = readObj(manifestRes);
        if (manifest == null) return false;

        JsonArray packsArr = manifest.has("packs") && manifest.get("packs").isJsonArray() ? manifest.getAsJsonArray("packs") : null;
        if (packsArr == null || packsArr.isEmpty()) return true;

        String mode = manifest.has("order_mode") ? safeString(manifest.get("order_mode")) : "manifest_order";
        ArrayList<ManifestPack> list = new ArrayList<>(packsArr.size());

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
            list.add(new ManifestPack(id, name, author, type, path, sort, i));
        }

        if (list.isEmpty()) return true;

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

        for (ManifestPack mp : list) {
            String packId = mp.id();
            if (packsOut.containsKey(packId)) continue;

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

            String folder = inferPackFolderFromPackPath(resolved, packId);
            loadSinglePack(namespace, packId, folder, packObj, rm, packsOut, skinsByIdOut, packBySkinOut);
        }

        return true;
    }

    private static String resolvePackJsonPath(String manifestPath, String packId) {
        if (manifestPath != null && !manifestPath.isBlank()) {
            String p = manifestPath.trim();
            if (p.startsWith("/")) p = p.substring(1);
            if (p.startsWith(SKINPACKS_PREFIX)) return p;
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
            if (p.startsWith(SKINPACKS_PREFIX) && p.endsWith(PACK_JSON_SUFFIX)) {
                String mid = p.substring(SKINPACKS_PREFIX.length(), p.length() - PACK_JSON_SUFFIX.length());
                if (!mid.isBlank()) return mid;
            }
        }
        String id = packId;
        int colon = id != null ? id.indexOf(':') : -1;
        if (colon > 0) id = id.substring(colon + 1);
        return id;
    }

    private static String parsePackId(String path) {
        if (path == null) return null;

        if ((SKINPACKS_PREFIX + "manifest.json").equals(path)) return null;

        if (path.startsWith(SKINPACKS_PREFIX) && path.endsWith(PACK_JSON_SUFFIX)) {
            String p = path.substring(SKINPACKS_PREFIX.length(), path.length() - PACK_JSON_SUFFIX.length());
            if (p.indexOf('/') >= 0) return null;
            if (p.startsWith("_")) return null;
            return p;
        }

        if (path.startsWith(SKINPACKS_PREFIX) && path.endsWith(".json")) {
            String file = path.substring(SKINPACKS_PREFIX.length());
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

        SkinPack favPack = new SkinPack(SkinIds.PACK_FAVOURITES, "Favourites", "", "", favIcon, fav);

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

    private static void loadLegacySkinPacksIndex(ResourceManager rm,
                                                 Map<String, SkinPack> packsOut,
                                                 Map<String, SkinEntry> skinsByIdOut,
                                                 Map<String, String> packBySkinOut) {
        for (String ns : rm.getNamespaces()) {

            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(ns, "skin_packs.json");
            Resource res = rm.getResource(rl).orElse(null);
            if (res == null) continue;

            try {
                JsonObject root = readObj(res);
                if (root == null) continue;

                for (Map.Entry<String, JsonElement> pe : root.entrySet()) {
                    if (!pe.getValue().isJsonObject()) continue;
                    String rawPackId = pe.getKey();

                    ResourceLocation packLoc;
                    try {
                        packLoc = rawPackId != null && rawPackId.indexOf(':') > 0
                                ? ResourceLocation.parse(rawPackId)
                                : ResourceLocation.fromNamespaceAndPath(ns, rawPackId);
                    } catch (Throwable ignored) {
                        continue;
                    }

                    String packId = packLoc.getNamespace() + ":" + packLoc.getPath();
                    if (packsOut.containsKey(packId)) continue;
                    if (PackExclusions.isExcluded(packId) || SkinIdUtil.containsMinecon(packLoc.getPath())) continue;

                    JsonObject obj = pe.getValue().getAsJsonObject();

                    collectPoseTagsFromPackJson(ns, obj);
                    String name = obj.has("name") ? obj.get("name").getAsString() : tr("skin_pack." + packLoc.getNamespace() + "." + packLoc.getPath(), packLoc.getPath());
                    String author = obj.has("author") ? obj.get("author").getAsString() : "";
                    String type = obj.has("type") ? obj.get("type").getAsString() : "";

                    ResourceLocation icon = DEFAULT_PACK_ICON;
                    if (obj.has("icon")) {
                        try {
                            icon = ResourceLocation.parse(obj.get("icon").getAsString());
                        } catch (Throwable ignored) {
                            icon = DEFAULT_PACK_ICON;
                        }
                    }
                    if (rm.getResource(icon).isEmpty()) icon = DEFAULT_PACK_ICON;

                    JsonArray skinsArr = obj.has("skins") && obj.get("skins").isJsonArray() ? obj.getAsJsonArray("skins") : null;
                    if (skinsArr == null) continue;

                    String packFolder = packLoc.getPath();
                    ArrayList<SkinEntry> entries = new ArrayList<>();
                    for (int i = 0; i < skinsArr.size(); i++) {
                        JsonElement se = skinsArr.get(i);
                        if (!se.isJsonObject()) continue;
                        JsonObject so = se.getAsJsonObject();

                        if (so.has("id") && so.has("texture")) {
                            String skinId = so.get("id").getAsString();
                            String skinName = so.has("name") ? so.get("name").getAsString() : tr("skin_pack." + packLoc.getNamespace() + "." + packLoc.getPath() + "." + i, skinId);
                            int order = so.has("order") ? so.get("order").getAsInt() : (i + 1);

                            String texPath = so.get("texture").getAsString();
                            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(ns, texPath);
                            if (!tex.getPath().startsWith(SKINPACKS_PREFIX)) {
                                tex = ResourceLocation.fromNamespaceAndPath(ns, SKINPACKS_PREFIX + packFolder + "/" + texPath);
                            }
                            SkinEntry entry = new SkinEntry(skinId, skinName, tex, null, false, order);
                            entries.add(entry);
                            collectPoseTagsFromSkinJson(so, skinId);
                            skinsByIdOut.putIfAbsent(entry.id(), entry);
                            packBySkinOut.putIfAbsent(entry.id(), packId);
                        }
                    }

                    if (!entries.isEmpty()) packsOut.put(packId, new SkinPack(packId, name, author, type, icon, entries));

                }
            } catch (Throwable ex) {
                DebugLog.warn("Failed to read legacy skin_packs.json from {}: {}", rl, ex.toString());
            }
        }
    }

    private static void loadSinglePack(String namespace, String packId, String packFolder, JsonObject json, ResourceManager rm,
                                       Map<String, SkinPack> packsOut,
                                       Map<String, SkinEntry> skinsByIdOut,
                                       Map<String, String> packBySkinOut) {

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
                ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(namespace, SKINPACKS_PREFIX + packFolder + "/" + texPath);
                ResourceLocation cape = null;
if (capePath != null && !capePath.isBlank()) {
                    cape = ResourceLocation.fromNamespaceAndPath(namespace, SKINPACKS_PREFIX + packFolder + "/" + capePath);
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

        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(namespace, SKINPACKS_PREFIX + packFolder + "/pack.png");
        if (rm.getResource(icon).isEmpty()) icon = DEFAULT_PACK_ICON;

        packsOut.put(packId, new SkinPack(packId, name, author, type, icon, entries));

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
    if (name == null || name.isBlank()) return fallbackId;
    if (name.startsWith("key:")) return tr(name.substring(4), fallbackId);
    return name;
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
