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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class DownloadedPackMetadata {
    private static final String FILE_NAME = ".legacy4j_content.json";
    private static final Entry EMPTY = new Entry(null, null);
    private static final Map<String, Entry> CACHE = new HashMap<>();

    private DownloadedPackMetadata() {
    }

    public record Entry(String name, String description) {
        public Component title(Component fallback) {
            return name == null || name.isBlank() ? fallback : Component.literal(name);
        }

        public Component description(Component fallback) {
            return description == null || description.isBlank() ? fallback : Component.literal(description);
        }
    }

    public static void write(Path packDir, ContentManager.Pack pack) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("name", pack.name());
        if (!pack.description().isBlank()) root.addProperty("description", pack.description());
        Files.writeString(packDir.resolve(FILE_NAME), root.toString(), StandardCharsets.UTF_8);
        CACHE.put(normalize(pack.id()), new Entry(pack.name(), pack.description()));
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
            return new Entry(name, description);
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
}
