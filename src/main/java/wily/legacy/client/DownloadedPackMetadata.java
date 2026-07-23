package wily.legacy.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import wily.legacy.Legacy4J;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DownloadedPackMetadata {
    private static final String FILE_NAME = ".legacy4j_content.json";
    private static final Entry EMPTY = new Entry(null, null, null, false, false);
    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();

    private DownloadedPackMetadata() {
    }

    public record Entry(String name, String description, String worldTemplateFolderName, boolean hasWorldTemplate, boolean useResourceAlbum) {
        public Component title(Component fallback) {
            return name == null || name.isBlank() ? fallback : Component.literal(name);
        }

        public Component description(Component fallback) {
            return description == null || description.isBlank() ? fallback : Component.literal(description);
        }

    }

    public static void write(Path packDir, ContentManager.Pack pack, ContentManager.Category category) throws IOException {
        Entry entry = from(pack, category);
        JsonObject root = new JsonObject();
        root.addProperty("name", entry.name());
        root.addProperty("useResourceAlbum", entry.useResourceAlbum());
        if (entry.description() != null) root.addProperty("description", entry.description());
        if (entry.worldTemplateFolderName() != null) root.addProperty("worldTemplateFolderName", entry.worldTemplateFolderName());
        root.addProperty("hasWorldTemplate", entry.hasWorldTemplate());
        Path metadataPath = packDir.resolve(FILE_NAME);
        Path tempPath = Files.createTempFile(packDir, FILE_NAME, ".tmp");
        try {
            Files.writeString(tempPath, root.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(tempPath, metadataPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, metadataPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempPath);
        }
        CACHE.put(normalize(pack.id()), entry);
    }

    public static boolean sync(Path packDir, ContentManager.Pack pack, ContentManager.Category category) throws IOException {
        Entry entry = from(pack, category);
        if (Files.isRegularFile(packDir.resolve(FILE_NAME)) && entry.equals(get(pack.id()))) return false;
        write(packDir, pack, category);
        return true;
    }

    public static void clear(String packId) {
        CACHE.remove(normalize(packId));
    }

    public static boolean exists(String packId) {
        Path path = path(normalize(packId));
        return path != null && Files.isRegularFile(path);
    }

    public static Entry entry(String packId) {
        return get(packId);
    }

    public static Component getTitle(Pack pack) {
        return getTitle(pack.getId(), pack.getTitle());
    }

    public static Component getTitle(String packId, Component fallback) {
        return get(packId).title(fallback);
    }

    public static Component getDescription(Pack pack) {
        return getDescription(pack.getId(), pack.getDescription());
    }

    public static Component getDescription(String packId, Component fallback) {
        return get(packId).description(fallback);
    }

    private static Entry get(String packId) {
        String id = normalize(packId);
        Entry entry = CACHE.get(id);
        if (entry != null) return entry;
        entry = read(id);
        CACHE.put(id, entry);
        return entry;
    }

    private static Entry read(String packId) {
        Path path = path(packId);
        if (path == null || !Files.isRegularFile(path)) return EMPTY;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String name = root.has("name") ? root.get("name").getAsString() : null;
            String description = root.has("description") ? root.get("description").getAsString() : null;
            String worldTemplateFolderName = root.has("worldTemplateFolderName") ? root.get("worldTemplateFolderName").getAsString() : null;
            boolean hasWorldTemplate = root.has("hasWorldTemplate")
                ? root.get("hasWorldTemplate").getAsBoolean()
                : worldTemplateFolderName != null && !worldTemplateFolderName.isBlank();
            boolean useResourceAlbum = !root.has("useResourceAlbum") || root.get("useResourceAlbum").getAsBoolean();
            return new Entry(name, description, worldTemplateFolderName, hasWorldTemplate, useResourceAlbum);
        } catch (Exception e) {
            Legacy4J.LOGGER.warn("Failed to read downloaded pack metadata for {}", packId, e);
            return EMPTY;
        }
    }

    private static Path path(String packId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return null;
        String normalized = normalize(packId);
        if (normalized.indexOf(':') >= 0 || normalized.indexOf('/') >= 0 || normalized.indexOf('\\') >= 0) return null;
        return minecraft.getResourcePackDirectory().resolve(normalized).resolve(FILE_NAME);
    }

    private static String normalize(String packId) {
        return packId.startsWith("file/") ? packId.substring(5) : packId;
    }

    private static Entry from(ContentManager.Pack pack, ContentManager.Category category) {
        String description = pack.description().isBlank() ? null : pack.description();
        String worldTemplateFolderName = pack.worldTemplateFolderName().filter(s -> !s.isBlank()).orElse(null);
        return new Entry(pack.name(), description, worldTemplateFolderName, pack.hasWorldTemplate(), category.useResourceAlbum());
    }
}
