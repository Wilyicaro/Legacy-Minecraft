package wily.legacy.Skins.skin;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
public final class CustomSkinPackStore {
    private static final String RESOURCE_PACK_DIR = "Legacy Custom Skinpacks";
    private static final String RESOURCE_PACK_ID = "file/" + RESOURCE_PACK_DIR;
    private static final String PACK_DESCRIPTION = "Legacy4J custom skin packs";
    private static final String ASSET_NAMESPACE = "lce_skinpacks";
    private static final String PACKS_DIR = "assets/" + ASSET_NAMESPACE + "/skinpacks";
    private static final String IMPORT_TEMPLATE_RESOURCE = "/assets/legacy/skin_templates/import_skin.png";
    private static final String RESOURCE_PACK_ICON = "/assets/legacy/skin_templates/custom_skinpacks_pack.png";
    private static final String IMPORT_NAME_KEY = "legacy.menu.import_skin";
    private static final String SLIM = "slim";
    private CustomSkinPackStore() {
    }
    public static String createPack(Minecraft minecraft, String name, Path iconPath) throws IOException {
        if (minecraft == null) throw new IOException("Minecraft is not available");
        Path resourcePackDir = resourcePackDir(minecraft);
        if (resourcePackDir == null) throw new IOException("Game directory is not available");
        SkinPackFiles.ensureResourcePackShell(resourcePackDir, PACK_DESCRIPTION, RESOURCE_PACK_ICON, "Missing bundled custom skin pack resource icon");
        String displayName = SkinIdUtil.cleanName(name, "Custom Skin Pack");
        String packId = nextPackId(minecraft, displayName);
        String importSkinId = importSkinId(packId);
        Path packDir = packDir(minecraft, packId);
        int sortIndex = SkinPackLoader.nextCustomPackSortIndex();
        Files.createDirectories(packDir.resolve("lang"));
        Files.createDirectories(packDir.resolve("skins"));
        writePackJson(packDir.resolve("pack.json"), packId, sortIndex, importSkinId);
        writePackName(packDir.resolve("lang/en_us.json"), packId, displayName);
        SkinPackFiles.copyBundledResource(CustomSkinPackStore.class, IMPORT_TEMPLATE_RESOURCE, packDir.resolve("skins").resolve(importSkinId + ".png"), "Missing bundled import skin template");
        if (iconPath != null) {
            SkinPackFiles.validateSquarePng(iconPath, "Pack icon", 0, 0);
            Files.copy(iconPath, packDir.resolve("pack.png"), StandardCopyOption.REPLACE_EXISTING);
        } else {
            SkinPackFiles.copyBundledResource(CustomSkinPackStore.class, RESOURCE_PACK_ICON, packDir.resolve("pack.png"), "Missing bundled custom skin pack resource icon");
        }
        return packId;
    }
    public static String importSkin(Minecraft minecraft, String packId, String name, String theme, List<String> poses, Path skinPath) throws IOException {
        Path packDir = requirePackDir(minecraft, packId);
        String displayName = SkinIdUtil.cleanName(name, "Custom Skin");
        String themeText = SkinIdUtil.trimToNull(theme);
        List<String> poseKeys = normalizePoses(poses);
        String skinId = nextSkinId(packDir, packId, displayName);
        int order = SkinPackJson.nextSkinOrder(packDir.resolve("pack.json"));
        String nameKey = "custom." + packId + "." + skinId + ".name";
        String themeKey = "custom." + packId + "." + skinId + ".theme";
        SkinPackFiles.copySkinPng(skinPath, packDir.resolve("skins").resolve(skinId + ".png"));
        appendSkin(packDir.resolve("pack.json"), createSkinEntry(skinId, "key:" + nameKey, "skins/" + skinId + ".png", order));
        writeSkinLang(packDir.resolve("lang/en_us.json"), packId, skinId, displayName, themeText);
        writeSkinModel(packDir.resolve("box_models").resolve(skinId + ".json"), themeText == null ? null : themeKey, poseKeys);
        return skinId;
    }
    public static boolean isCustomPack(Minecraft minecraft, String packId) {
        return SkinPackFiles.isPackInResourcePack(minecraft, RESOURCE_PACK_DIR, PACKS_DIR, packId);
    }
    public static boolean isImportSkin(String packId, String skinId) {
        return packId != null && skinId != null && importSkinId(packId).equals(skinId);
    }
    public static boolean isEditableSkin(Minecraft minecraft, String packId, String skinId) throws IOException { return isCustomPack(minecraft, packId) && skinId != null && !skinId.isBlank() && !isImportSkin(packId, skinId); }

    public static String importSkinId(String packId) {
        return packId + "_import_skin";
    }
    public static void updatePack(Minecraft minecraft, String packId, String name, Path iconPath) throws IOException {
        Path packDir = requirePackDir(minecraft, packId);
        writePackName(packDir.resolve("lang/en_us.json"), packId, SkinIdUtil.cleanName(name, "Custom Skin Pack"));
        if (iconPath != null) {
            SkinPackFiles.validateSquarePng(iconPath, "Pack icon", 0, 0);
            Path target = packDir.resolve("pack.png");
            if (!iconPath.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
                Files.copy(iconPath, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    public static void updateSkin(Minecraft minecraft, String packId, String skinId, String name, String theme, List<String> poses, Path skinPath) throws IOException {
        Path dir = requirePackDir(minecraft, packId);
        requireEditableSkin(minecraft, packId, skinId, "edited");
        String displayName = SkinIdUtil.cleanName(name, "Custom Skin");
        String themeText = SkinIdUtil.trimToNull(theme);
        List<String> poseKeys = normalizePoses(poses);
        if (skinPath != null) {
            SkinPackFiles.copySkinPng(skinPath, dir.resolve("skins").resolve(skinId + ".png"));
        }
        writeSkinLang(dir.resolve("lang/en_us.json"), packId, skinId, displayName, themeText);
        writeSkinModel(dir.resolve("box_models").resolve(skinId + ".json"), themeText == null ? null : "custom." + packId + "." + skinId + ".theme", poseKeys);
    }
    public static void moveSkin(Minecraft minecraft, String packId, String skinId, int delta) throws IOException {
        Path packJson = requirePackDir(minecraft, packId).resolve("pack.json");
        if (delta == 0 || !isEditableSkin(minecraft, packId, skinId)) return;
        ArrayList<JsonObject> skins = SkinPackJson.readOrderedSkins(packJson);
        int index = SkinPackJson.indexOfSkin(skins, skinId);
        if (index < 0) return;
        int minIndex = !skins.isEmpty() && isImportSkin(packId, SkinPackJson.string(skins.getFirst().get("id"))) ? 1 : 0;
        int target = Math.max(minIndex, Math.min(skins.size() - 1, index + delta));
        if (target == index) return;
        JsonObject skin = skins.remove(index);
        skins.add(target, skin);
        SkinPackJson.writeOrderedSkins(packJson, skins);
    }
    public static void deleteSkin(Minecraft minecraft, String packId, String skinId) throws IOException {
        Path dir = requirePackDir(minecraft, packId);
        requireEditableSkin(minecraft, packId, skinId, "removed");
        Path packJson = dir.resolve("pack.json");
        ArrayList<JsonObject> skins = SkinPackJson.readOrderedSkins(packJson);
        int index = SkinPackJson.indexOfSkin(skins, skinId);
        if (index < 0) return;
        skins.remove(index);
        SkinPackJson.writeOrderedSkins(packJson, skins);
        removeSkinLang(dir.resolve("lang/en_us.json"), packId, skinId);
        SkinPackFiles.deleteSkinFiles(dir, skinId);
    }
    public static void deletePack(Minecraft minecraft, String packId) throws IOException {
        SkinPackFiles.deleteTree(requirePackDir(minecraft, packId));
    }
    public static boolean enableResourcePack(Minecraft minecraft) throws IOException {
        return SkinPackFiles.enableResourcePack(minecraft, RESOURCE_PACK_DIR, PACK_DESCRIPTION, RESOURCE_PACK_ICON, "Missing bundled custom skin pack resource icon");
    }
    private static Path resourcePackDir(Minecraft minecraft) {
        return SkinPackFiles.resourcePackDir(minecraft, RESOURCE_PACK_DIR);
    }
    private static Path packDir(Minecraft minecraft, String packId) {
        Path dir = resourcePackDir(minecraft);
        if (dir == null) throw new IllegalStateException("Game directory is not available");
        return dir.resolve(PACKS_DIR).resolve(packId);
    }
    static Path packJsonPath(Minecraft minecraft, String packId) {
        return packDir(minecraft, packId).resolve("pack.json");
    }
    private static Path requirePackDir(Minecraft minecraft, String packId) throws IOException {
        if (!isCustomPack(minecraft, packId)) throw new IOException("Custom skin pack was not found");
        return packDir(minecraft, packId);
    }
    private static void requireEditableSkin(Minecraft minecraft, String packId, String skinId, String action) throws IOException {
        if (skinId == null || skinId.isBlank()) throw new IOException("Custom skin was not found");
        if (!isEditableSkin(minecraft, packId, skinId)) throw new IOException("This skin cannot be " + action);
    }
    private static void writePackJson(Path path, String packId, int sortIndex, String importSkinId) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("name", "key:custom." + packId + ".pack.name");
        root.addProperty("author", "");
        root.addProperty("type", "skin");
        root.addProperty("editable", true);
        root.addProperty("allow_empty", true);
        root.addProperty("sort_index", sortIndex);
        root.addProperty("sort_sub_index", 0);
        JsonArray skins = new JsonArray();
        skins.add(createSkinEntry(importSkinId, "key:" + IMPORT_NAME_KEY, "skins/" + importSkinId + ".png", 1));
        root.add("skins", skins);
        SkinPackFiles.writeJson(path, root);
    }
    private static JsonObject createSkinEntry(String skinId, String name, String texture, int order) {
        JsonObject skin = new JsonObject();
        skin.addProperty("id", skinId);
        skin.addProperty("name", name);
        skin.addProperty("texture", texture);
        skin.addProperty("order", order);
        return skin;
    }
    private static void appendSkin(Path path, JsonObject skin) throws IOException {
        JsonObject root = SkinPackFiles.readJson(path);
        JsonArray skins = root.has("skins") && root.get("skins").isJsonArray() ? root.getAsJsonArray("skins") : new JsonArray();
        skins.add(skin);
        root.add("skins", skins);
        SkinPackFiles.writeJson(path, root);
    }
    private static void writePackName(Path path, String packId, String displayName) throws IOException {
        updateLang(path, root -> root.addProperty("custom." + packId + ".pack.name", displayName));
    }
    private static void writeSkinLang(Path path, String packId, String skinId, String displayName, String themeText) throws IOException {
        updateLang(path, root -> {
            root.addProperty("custom." + packId + "." + skinId + ".name", displayName);
            if (themeText != null) root.addProperty("custom." + packId + "." + skinId + ".theme", themeText);
            else root.remove("custom." + packId + "." + skinId + ".theme");
        });
    }
    private static void removeSkinLang(Path path, String packId, String skinId) throws IOException {
        updateLang(path, root -> {
            root.remove("custom." + packId + "." + skinId + ".name");
            root.remove("custom." + packId + "." + skinId + ".theme");
        });
    }
    private static void updateLang(Path path, LangUpdate update) throws IOException {
        JsonObject root = SkinPackFiles.readJson(path);
        update.apply(root);
        SkinPackFiles.writeJson(path, root);
    }
    private static void writeSkinModel(Path path, String themeKey, List<String> poseKeys) throws IOException {
        JsonObject root = SkinPackFiles.readJson(path);
        if (themeKey != null && !themeKey.isBlank()) {
            JsonObject meta = root.has("meta") && root.get("meta").isJsonObject() ? root.getAsJsonObject("meta") : new JsonObject();
            meta.addProperty("themeNameId", themeKey);
            root.add("meta", meta);
        } else if (root.has("meta") && root.get("meta").isJsonObject()) {
            JsonObject meta = root.getAsJsonObject("meta");
            meta.remove("themeNameId");
            if (meta.entrySet().isEmpty()) root.remove("meta");
        }
        if (!poseKeys.isEmpty()) {
            JsonArray poses = new JsonArray();
            for (String poseKey : poseKeys) poses.add(poseKey);
            root.add("poses", poses);
        } else {
            root.remove("poses");
        }
        if (root.entrySet().isEmpty()) {
            Files.deleteIfExists(path);
            return;
        }
        SkinPackFiles.writeJson(path, root);
    }
    private static String nextPackId(Minecraft minecraft, String displayName) throws IOException {
        String base = SkinIdUtil.slug(displayName, "custom_skin");
        LinkedHashSet<String> used = new LinkedHashSet<>(SkinPackLoader.getPacks().keySet());
        Path packsDir = resourcePackDir(minecraft).resolve(PACKS_DIR);
        if (Files.isDirectory(packsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(packsDir)) {
                for (Path path : stream) {
                    if (Files.isDirectory(path)) used.add(path.getFileName().toString());
                }
            }
        }
        return uniqueId(used, base);
    }
    private static String nextSkinId(Path packDir, String packId, String displayName) throws IOException {
        return uniqueId(SkinPackJson.readSkinIds(packDir.resolve("pack.json")), packId + "_" + SkinIdUtil.slug(displayName, "custom_skin"));
    }
    private static String normalizePose(String pose) {
        if (pose == null || pose.isBlank()) return null;
        String key = pose.trim().toLowerCase(Locale.ROOT);
        if (key.equals(SLIM) || key.equals("alex")) return SLIM;
        SkinPoseRegistry.PoseTag tag = SkinPoseRegistry.PoseTag.fromKey(key);
        return tag == null ? null : tag.name().toLowerCase(Locale.ROOT);
    }
    private static List<String> normalizePoses(List<String> poses) {
        if (poses == null || poses.isEmpty()) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String pose : poses) {
            String key = normalizePose(pose);
            if (key != null) out.add(key);
        }
        return List.copyOf(out);
    }
    private static String uniqueId(Set<String> used, String base) {
        if (!used.contains(base)) return base;
        for (int i = 2; ; i++) {
            String candidate = base + "_" + i;
            if (!used.contains(candidate)) return candidate;
        }
    }
    @FunctionalInterface
    private interface LangUpdate { void apply(JsonObject root); }
}
