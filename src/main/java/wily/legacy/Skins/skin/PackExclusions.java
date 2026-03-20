package wily.legacy.Skins.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import wily.legacy.Skins.util.LegacySkinsPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class PackExclusions {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = SkinSync.MODID + "_pack_exclusions.json";

    private static final Set<String> EXCLUDED = new HashSet<>();
    private static volatile boolean loaded;
    private static final Object LOCK = new Object();

    private PackExclusions() {
    }

    public static void ensureLoaded() {
        if (loaded) return;
        synchronized (LOCK) {
            if (!loaded) loadLocked();
        }
    }

    public static void reload() {
        synchronized (LOCK) {
            loaded = false;
            loadLocked();
        }
    }

    public static boolean isExcluded(String packId) {
        if (packId == null) return false;
        ensureLoaded();
        synchronized (LOCK) {
            return EXCLUDED.contains(packId);
        }
    }

    public static Set<String> getExcluded() {
        ensureLoaded();
        synchronized (LOCK) {
            return Collections.unmodifiableSet(new HashSet<>(EXCLUDED));
        }
    }

    public static void setExcluded(String packId, boolean excluded) {
        if (packId == null || packId.isBlank()) return;
        ensureLoaded();
        synchronized (LOCK) {
            if (excluded) EXCLUDED.add(packId);
            else EXCLUDED.remove(packId);
        }
        save();
    }

    private static Path filePath() {
        return LegacySkinsPaths.resolve("pack_exclusions.json", FILE_NAME);
    }

    private static void loadLocked() {
        EXCLUDED.clear();
        Path p = filePath();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                JsonArray arr = obj == null ? null : obj.getAsJsonArray("excluded");
                if (arr != null) {
                    for (JsonElement e : arr) {
                        if (!e.isJsonPrimitive()) continue;
                        String id = e.getAsString();
                        if (id != null && !id.isBlank()) EXCLUDED.add(id);
                    }
                }
            } catch (Exception ex) {
                LOGGER.warn("Failed to read exclusions: {}", ex.toString());
            }
        }
        loaded = true;
    }

    private static void save() {
        Path p = filePath();
        try {
            Files.createDirectories(p.getParent());
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();
            synchronized (LOCK) {
                for (String id : EXCLUDED) arr.add(id);
            }
            obj.add("excluded", arr);
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, w);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to save exclusions: {}", ex.toString());
        }
    }
}
