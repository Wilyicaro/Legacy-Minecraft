package wily.legacy.Skins.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import wily.legacy.Skins.util.DebugLog;
import wily.legacy.Skins.client.render.SkinPoseRegistry;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

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

    private static final ResourceLocation DEFAULT_PACK_ICON = ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "skinpacks/default/pack.png");
    private static final ResourceLocation DEFAULT_CPM_PREVIEW_SKIN = ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "skinpacks/default/skins/steve.png");

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
            Map<ResourceLocation, Resource> jsons = rm.listResources("skinpacks", rl -> rl.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, Resource> e : jsons.entrySet()) {
                ResourceLocation rl = e.getKey();
                String ns = rl.getNamespace();
                String path = rl.getPath();

                String packId = parsePackId(path);
                if (packId == null || packId.isEmpty()) continue;
                if (packs.containsKey(packId)) continue;
                if (PackExclusions.isExcluded(packId)) continue;

                if (!SkinIds.PACK_DEFAULT.equals(packId) && SkinSync.MODID.equals(ns)) continue;

                JsonObject json = readObj(e.getValue());
                if (json != null) loadSinglePack(ns, packId, json, rm, packs, skinsById, packBySkin);
            }

            loadLegacySkinPacksIndex(rm, packs, skinsById, packBySkin);
        } catch (Throwable ex) {
            DebugLog.warn("ResourceManager skinpack scan failed: {}", ex.toString());
        } finally {
            SkinPoseRegistry.endReload();
        }

        LinkedHashMap<String, SkinPack> ordered = withFavourites(packs, skinsById);
        synchronized (LOCK) {
            PACKS = Collections.unmodifiableMap(ordered);
            SKINS_BY_ID = Collections.unmodifiableMap(skinsById);
            PACK_BY_SKIN = Collections.unmodifiableMap(packBySkin);
            loaded = true;
        }
    }

    private static String parsePackId(String path) {
        if (path == null) return null;
        if (path.startsWith(SKINPACKS_PREFIX) && path.endsWith(PACK_JSON_SUFFIX)) {
            String p = path.substring(SKINPACKS_PREFIX.length(), path.length() - PACK_JSON_SUFFIX.length());
            return p.indexOf('/') >= 0 ? null : p;
        }

        if (path.startsWith(SKINPACKS_PREFIX) && path.endsWith(".json")) {
            String file = path.substring(SKINPACKS_PREFIX.length());
            if (file.indexOf('/') >= 0) return null;
            if ("pack.json".equals(file)) return null;
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

        SkinPack favPack = new SkinPack(SkinIds.PACK_FAVOURITES, "Favourites", "", "", DEFAULT_PACK_ICON, fav);

        LinkedHashMap<String, SkinPack> out = new LinkedHashMap<>();
        SkinPack def = base.get(SkinIds.PACK_DEFAULT);
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
            if (SkinSync.MODID.equals(ns)) continue;

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
                            SkinEntry entry = new SkinEntry(skinId, skinName, tex, order);
                            entries.add(entry);
                            skinsByIdOut.putIfAbsent(entry.id(), entry);
                            packBySkinOut.putIfAbsent(entry.id(), packId);
                            continue;
                        }

                        if (so.has("model")) {
                            String modelStr = so.get("model").getAsString();
                            if (modelStr == null || modelStr.isEmpty()) continue;
                            ResourceLocation modelLoc;
                            try {
                                modelLoc = modelStr.indexOf(':') > 0 ? ResourceLocation.parse(modelStr) : ResourceLocation.fromNamespaceAndPath(ns, modelStr);
                            } catch (Throwable ignored) {
                                continue;
                            }
                            String modelKey = modelLoc.toString();
                            String id = SkinIds.CPM_PREFIX + modelKey;

                            
                            collectPoseTagsFromSkinJson(ns, so, id);
                            String skinName = so.has("name") ? so.get("name").getAsString() : tr("skin_pack." + packLoc.getNamespace() + "." + packLoc.getPath() + "." + i, modelKey);
                            SkinEntry entry = new SkinEntry(id, skinName, DEFAULT_CPM_PREVIEW_SKIN, i + 1);
                            entries.add(entry);
                            skinsByIdOut.putIfAbsent(entry.id(), entry);
                            packBySkinOut.putIfAbsent(entry.id(), packId);
                        }
                    }

                    packsOut.put(packId, new SkinPack(packId, name, author, type, icon, entries));
                }
            } catch (Throwable ex) {
                DebugLog.warn("Failed to read legacy skin_packs.json from {}: {}", rl, ex.toString());
            }
        }
    }

    private static void loadSinglePack(String namespace, String packId, JsonObject json, ResourceManager rm,
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
                int order = o.has("order") ? o.get("order").getAsInt() : (i + 1);
                String texPath = o.get("texture").getAsString();

                
                collectPoseTagsFromSkinJson(namespace, o, skinId);

                ResourceLocation texture;
                if (SkinIds.PACK_DEFAULT.equals(packId) && SkinSync.MODID.equals(namespace)) {
                    texture = DefaultAtlasSkins.getTexture(rm, texPath);
                } else {
                    texture = ResourceLocation.fromNamespaceAndPath(namespace, SKINPACKS_PREFIX + packId + "/" + texPath);
                    if (rm.getResource(texture).isEmpty() && SkinIdUtil.isCpm(skinId))
                        texture = DEFAULT_CPM_PREVIEW_SKIN;
                }
                tmp.add(new SkinEntryWithIndex(new SkinEntry(skinId, skinName, texture, order), i));
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

        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(namespace, SKINPACKS_PREFIX + packId + "/pack.png");
        if (rm.getResource(icon).isEmpty()) icon = DEFAULT_PACK_ICON;

        packsOut.put(packId, new SkinPack(packId, name, author, type, icon, entries));
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

    
    private static void collectPoseTagsFromSkinJson(String namespace, JsonObject skinObj, String skinId) {
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
                    if (tag != null) SkinPoseRegistry.addSelector(tag, skinId, namespace);
                }
            }
        } else if (poses.isJsonPrimitive()) {
            SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(poses.getAsString());
            if (tag != null) SkinPoseRegistry.addSelector(tag, skinId, namespace);
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
            String v = I18n.get(key);
            return v == null || v.isEmpty() || v.equals(key) ? fallback : v;
        } catch (Throwable t) {
            return fallback;
        }
    }

    private record SkinEntryWithIndex(SkinEntry entry, int index) {
    }
}
