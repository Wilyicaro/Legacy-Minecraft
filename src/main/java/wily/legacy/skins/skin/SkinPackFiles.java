package wily.legacy.Skins.skin;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.ARGB;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import wily.legacy.client.PackAlbum;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
public final class SkinPackFiles {
    private SkinPackFiles() {
    }
    static Path resourcePackDir(Minecraft minecraft, String dirName) {
        if (minecraft == null || minecraft.gameDirectory == null) return null;
        return minecraft.gameDirectory.toPath().resolve("resourcepacks").resolve(dirName);
    }
    static boolean isPackInResourcePack(Minecraft minecraft, String dirName, String packsDir, String packId) {
        if (packId == null || packId.isBlank()) return false;
        Path dir = resourcePackDir(minecraft, dirName);
        return dir != null && Files.isDirectory(dir.resolve(packsDir).resolve(packId));
    }
    static boolean enableResourcePack(Minecraft minecraft, String dirName, String description, String iconResource, String iconError) throws IOException {
        if (minecraft == null) throw new IOException("Minecraft is not available");
        PackRepository repository = minecraft.getResourcePackRepository();
        if (repository == null) throw new IOException("Resource pack repository is not available");
        Path dir = resourcePackDir(minecraft, dirName);
        if (dir == null) throw new IOException("Game directory is not available");
        ensureResourcePackShell(dir, description, iconResource, iconError);
        repository.reload();
        String selectedId = resolveResourcePackId(repository, "file/" + dirName, dirName);
        if (selectedId == null) throw new IOException("Resource pack could not be found");
        ArrayList<String> selected = new ArrayList<>(repository.getSelectedIds());
        boolean changed = selected.removeIf(id -> isManagedResourcePackId(id, dirName) && !selectedId.equals(id));
        if (!selected.contains(selectedId)) {
            selected.add(selectedId);
            changed = true;
        }
        if (changed) {
            repository.setSelected(selected);
            PackAlbum.updateSavedResourcePacks();
        }
        return changed;
    }
    public static Path choosePng(Minecraft minecraft, String title) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            String startPath = minecraft == null || minecraft.gameDirectory == null ? "" : minecraft.gameDirectory.getAbsolutePath();
            String path = TinyFileDialogs.tinyfd_openFileDialog(title, startPath, filters, "PNG Files", false);
            return path == null || path.isBlank() ? null : Path.of(path);
        }
    }
    static boolean managesTargetDirectory(String folderName, String targetDir) {
        if (folderName == null || targetDir == null) return false;
        String normalized = folderName.replace('\\', '/').trim();
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return targetDir.equals(normalized);
    }
    static boolean isPackInstall(Path packDir) {
        return packDir != null && Files.isRegularFile(packDir.resolve("pack.json"));
    }
    static JsonObject readJson(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return new JsonObject();
        try {
            JsonElement element = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException ex) {
            throw new IOException("Invalid JSON in " + path.getFileName(), ex);
        }
    }
    static void writeJson(Path path, JsonObject root) throws IOException {
        Files.createDirectories(path.getParent());
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            writer.setSerializeNulls(false);
            writer.setIndent("  ");
            GsonHelper.writeValue(writer, root, null);
        }
    }
    static void copyBundledResource(Class<?> owner, String resource, Path target, String error) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream in = owner.getResourceAsStream(resource)) {
            if (in == null) throw new IOException(error);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    static void ensureResourcePackShell(Path resourcePackDir, String description, String iconResource, String iconError) throws IOException {
        Files.createDirectories(resourcePackDir.resolve("assets/lce_skinpacks/skinpacks"));
        if (!Files.exists(resourcePackDir.resolve("pack.mcmeta"))) {
            JsonObject pack = new JsonObject();
            pack.addProperty("description", description);
            pack.addProperty("pack_format", 16);
            pack.addProperty("min_format", 16);
            pack.addProperty("max_format", 999);

            var supportedFormats = new com.google.gson.JsonArray();
            supportedFormats.add(16);
            supportedFormats.add(999);
            pack.add("supported_formats", supportedFormats);

            JsonObject root = new JsonObject();
            root.add("pack", pack);
            writeJson(resourcePackDir.resolve("pack.mcmeta"), root);
        }
        if (!Files.exists(resourcePackDir.resolve("pack.png"))) {
            copyBundledResource(CustomSkinPackStore.class, iconResource, resourcePackDir.resolve("pack.png"), iconError);
        }
    }
    static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root).sorted(Comparator.reverseOrder())) {
            for (Path path : (Iterable<Path>) paths::iterator) Files.deleteIfExists(path);
        }
    }
    static void deleteSkinFiles(Path packDir, String skinId) throws IOException {
        Files.deleteIfExists(packDir.resolve("skins").resolve(skinId + ".png"));
        Files.deleteIfExists(packDir.resolve("box_models").resolve(skinId + ".json"));
        Files.deleteIfExists(packDir.resolve("box_textures").resolve(skinId + ".png"));
        Files.deleteIfExists(packDir.resolve("advancement_faces").resolve(skinId + ".png"));
    }
    static void copySkinPng(Path source, Path target) throws IOException {
        if (source == null || !Files.isRegularFile(source)) throw new IOException("Skin file was not found");
        try (InputStream in = Files.newInputStream(source); NativeImage image = NativeImage.read(in)) {
            if (image == null) throw new IOException("Skin must be a valid PNG");
            int width = image.getWidth();
            int height = image.getHeight();
            boolean legacy = width == 64 && height == 32;
            boolean modern = width == 64 && height == 64;
            boolean hd = width == 128 && height == 128;
            if (!legacy && !modern && !hd) throw new IOException("Skin must be 64x32, 64x64, or 128x128");
            Files.createDirectories(target.getParent());
            if (!legacy) {
                if (!source.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            }
            try (NativeImage converted = convertLegacySkin(image)) {
                converted.writeToFile(target);
            }
        } catch (RuntimeException ex) {
            throw new IOException("Skin must be a valid PNG", ex);
        }
    }
    static void validateSquarePng(Path path, String label, int minSize, int maxSize) throws IOException {
        if (path == null || !Files.isRegularFile(path)) throw new IOException(label + " file was not found");
        try (InputStream in = Files.newInputStream(path); NativeImage image = NativeImage.read(in)) {
            if (minSize == 0 || image == null) return;
            int width = image.getWidth();
            int height = image.getHeight();
            if ((width == minSize && height == minSize) || (width == maxSize && height == maxSize)) return;
            throw new IOException(label + " must be " + minSize + "x" + minSize + " or " + maxSize + "x" + maxSize);
        } catch (RuntimeException ex) {
            throw new IOException(label + " must be a valid PNG", ex);
        }
    }
    private static String resolveResourcePackId(PackRepository repository, String... ids) {
        for (String id : ids) {
            if (repository.getPack(id) != null) return id;
        }
        return null;
    }
    private static NativeImage convertLegacySkin(NativeImage image) {
        NativeImage converted = new NativeImage(64, 64, true);
        converted.copyFrom(image);
        converted.fillRect(0, 32, 64, 32, 0);
        converted.copyRect(4, 16, 16, 32, 4, 4, true, false);
        converted.copyRect(8, 16, 16, 32, 4, 4, true, false);
        converted.copyRect(0, 20, 24, 32, 4, 12, true, false);
        converted.copyRect(4, 20, 16, 32, 4, 12, true, false);
        converted.copyRect(8, 20, 8, 32, 4, 12, true, false);
        converted.copyRect(12, 20, 16, 32, 4, 12, true, false);
        converted.copyRect(44, 16, -8, 32, 4, 4, true, false);
        converted.copyRect(48, 16, -8, 32, 4, 4, true, false);
        converted.copyRect(40, 20, 0, 32, 4, 12, true, false);
        converted.copyRect(44, 20, -8, 32, 4, 12, true, false);
        converted.copyRect(48, 20, -16, 32, 4, 12, true, false);
        converted.copyRect(52, 20, -8, 32, 4, 12, true, false);
        setNoAlpha(converted, 0, 0, 32, 16);
        doNotchTransparencyHack(converted, 32, 0, 64, 32);
        setNoAlpha(converted, 0, 16, 64, 32);
        setNoAlpha(converted, 16, 48, 48, 64);
        return converted;
    }
    private static void doNotchTransparencyHack(NativeImage image, int startX, int startY, int endX, int endY) {
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                if (ARGB.alpha(image.getPixel(x, y)) < 128) return;
            }
        }
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                image.setPixel(x, y, image.getPixel(x, y) & 16777215);
            }
        }
    }
    private static void setNoAlpha(NativeImage image, int startX, int startY, int endX, int endY) {
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                image.setPixel(x, y, ARGB.opaque(image.getPixel(x, y)));
            }
        }
    }
    private static boolean isManagedResourcePackId(String id, String dirName) {
        return ("file/" + dirName).equals(id) || dirName.equals(id);
    }
}
