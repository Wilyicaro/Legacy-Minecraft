package wily.legacy.Skins.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/* * * Favourites are stored. */
public final class FavoritesStore {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = SkinSync.MODID + "_favourites.json";

    private static final List<String> ORDERED = new ArrayList<>();
    private static final Set<String> SET = new HashSet<>();
    private static volatile boolean loaded;
    private static final Object LOCK = new Object();

    private FavoritesStore() {
    }

    public static void ensureLoaded() {
        if (loaded) return;
        synchronized (LOCK) {
            if (!loaded) loadLocked();
        }
    }

    public static boolean isFavorite(String id) {
        if (id == null) return false;
        ensureLoaded();
        synchronized (LOCK) {
            return SET.contains(id);
        }
    }

    public static List<String> getFavorites() {
        ensureLoaded();
        synchronized (LOCK) {
            return Collections.unmodifiableList(new ArrayList<>(ORDERED));
        }
    }

    public static boolean toggle(String id) {
        if (id == null || id.isBlank()) return false;
        ensureLoaded();

        boolean nowFav;
        synchronized (LOCK) {
            if (SET.contains(id)) {
                SET.remove(id);
                ORDERED.removeIf(s -> s.equals(id));
                nowFav = false;
            } else {
                SET.add(id);
                ORDERED.add(id);
                nowFav = true;
            }
        }
        save();
        return nowFav;
    }

    private static Path filePath() {
        Path base;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.gameDirectory != null) {
                base = mc.gameDirectory.toPath();
            } else {
                base = Path.of(System.getProperty("user.dir"));
            }
        } catch (Throwable t) {
            base = Path.of(System.getProperty("user.dir"));
        }
        return base.resolve("config").resolve(FILE_NAME);
    }

    private static void loadLocked() {
        ORDERED.clear();
        SET.clear();
        Path p = filePath();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                JsonArray arr = obj == null ? null : obj.getAsJsonArray("favourites");
                if (arr != null) {
                    for (JsonElement e : arr) {
                        if (!e.isJsonPrimitive()) continue;
                        String id = e.getAsString();
                        if (id == null || id.isBlank()) continue;
                        if (SET.add(id)) ORDERED.add(id);
                    }
                }
            } catch (Exception ex) {
                LOGGER.warn("Failed to read favourites: {}", ex.toString());
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
                for (String id : ORDERED) arr.add(id);
            }
            obj.add("favourites", arr);
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, w);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to save favourites: {}", ex.toString());
        }
    }
}
