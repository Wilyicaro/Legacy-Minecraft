package wily.legacy.skins.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;

public final class DownloadedSkinPackStore {
    private static final String RESOURCE_PACK_DIR = "Legacy Downloaded Skinpacks";
    private static final String PACK_DESCRIPTION = "Legacy4J skin packs";
    private static final String ASSET_NAMESPACE = "lce_skinpacks";
    private static final String PACKS_DIR = "assets/" + ASSET_NAMESPACE + "/skinpacks";
    private static final String TARGET_DIRECTORY_NAME = "resourcepacks/" + RESOURCE_PACK_DIR + "/" + PACKS_DIR;
    private static final String RESOURCE_PACK_ICON = "/assets/legacy/skin_templates/downloaded_skinpacks_pack.png";

    private DownloadedSkinPackStore() {
    }

    public static boolean managesTargetDirectory(String folderName) {
        return SkinPackFiles.managesTargetDirectory(folderName, TARGET_DIRECTORY_NAME);
    }

    public static boolean isValidPackInstall(Path packDir) {
        return SkinPackFiles.isPackInstall(packDir);
    }

    public static boolean isDownloadedPack(Minecraft minecraft, String packId) {
        return SkinPackFiles.isPackInResourcePack(minecraft, RESOURCE_PACK_DIR, PACKS_DIR, packId);
    }

    public static boolean isManagedResourcePackId(String packId) {
        return ("file/" + RESOURCE_PACK_DIR).equals(packId) || RESOURCE_PACK_DIR.equals(packId);
    }

    public static void normalizeInstalledPack(Path packDir) throws IOException {
        normalizePack(packDir, "community");
    }

    static void normalizePack(Path packDir, String fallbackType) throws IOException {
        if (!isValidPackInstall(packDir)) return;
        Path packJson = packDir.resolve("pack.json");
        JsonObject root = SkinPackFiles.readJson(packJson);
        root.remove("editable");
        root.remove("allow_empty");
        String type = SkinPackJson.string(root.get("type"));
        if (type == null || type.isBlank()) root.addProperty("type", fallbackType);
        if (root.has("skins") && root.get("skins").isJsonArray()) {
            JsonArray oldSkins = root.getAsJsonArray("skins");
            JsonArray skins = new JsonArray();
            int order = 1;
            for (JsonElement element : oldSkins) {
                if (!element.isJsonObject()) continue;
                JsonObject skin = element.getAsJsonObject();
                String skinId = SkinPackJson.string(skin.get("id"));
                String name = SkinPackJson.string(skin.get("name"));
                if (skinId != null && skinId.endsWith("_import_skin") || "key:legacy.menu.import_skin".equals(name)) {
                    if (skinId != null && !skinId.isBlank()) SkinPackFiles.deleteSkinFiles(packDir, skinId);
                    continue;
                }
                JsonObject cleanSkin = skin.deepCopy();
                cleanSkin.addProperty("order", order++);
                skins.add(cleanSkin);
            }
            root.add("skins", skins);
        }
        SkinPackFiles.writeJson(packJson, root);
    }

    public static void enableResourcePack(Minecraft minecraft) throws IOException {
        SkinPackFiles.enableResourcePack(minecraft, RESOURCE_PACK_DIR, PACK_DESCRIPTION, RESOURCE_PACK_ICON, "Missing bundled downloaded skin pack resource icon");
    }

    public static List<String> preserveSelection(PackRepository repository, List<String> selectedIds) {
        ArrayList<String> selected = new ArrayList<>(selectedIds);
        if (repository == null) return selected;
        String resolvedId = repository.getPack("file/" + RESOURCE_PACK_DIR) != null ? "file/" + RESOURCE_PACK_DIR : repository.getPack(RESOURCE_PACK_DIR) != null ? RESOURCE_PACK_DIR : null;
        if (resolvedId != null && !selected.contains(resolvedId)) selected.add(resolvedId);
        return selected;
    }

    public static void deletePack(Minecraft minecraft, String packId) throws IOException {
        if (!isDownloadedPack(minecraft, packId)) throw new IOException("Downloaded skin pack was not found");
        Path dir = SkinPackFiles.resourcePackDir(minecraft, RESOURCE_PACK_DIR);
        if (dir == null) throw new IOException("Game directory is not available");
        SkinPackFiles.deleteTree(dir.resolve(PACKS_DIR).resolve(packId));
    }
}
