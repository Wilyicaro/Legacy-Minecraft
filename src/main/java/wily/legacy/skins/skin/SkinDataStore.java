package wily.legacy.Skins.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import wily.legacy.Skins.util.DebugLog;
import wily.legacy.Skins.util.SkinPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public final class SkinDataStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();
    private static final Path STATE_PATH = SkinPaths.resolve("state.json");
    private static final Path LEGACY_SELECTION_PATH = SkinPaths.resolve("selected_skin.txt");
    private static final Path LEGACY_FAVORITES_PATH = SkinPaths.resolve("favourites.json");
    private static final Path LEGACY_EXCLUSIONS_PATH = SkinPaths.resolve("pack_exclusions.json");
    private static State state;

    private SkinDataStore() {
    }

    public static String getSelectedSkin(UUID userId) {
        if (userId == null) return "";
        synchronized (LOCK) {
            State data = state();
            return userId.equals(data.selectionUserId) ? data.selectionSkinId : "";
        }
    }

    public static void setSelectedSkin(UUID userId, String skinId) {
        if (userId == null) return;
        synchronized (LOCK) {
            State data = state();
            data.selectionUserId = userId;
            data.selectionSkinId = SkinIdUtil.normalize(skinId);
            saveState(data);
        }
    }

    public static boolean isFavorite(String skinId) {
        String id = SkinIdUtil.normalize(skinId);
        if (!SkinIdUtil.hasSkin(id)) return false;
        synchronized (LOCK) {
            return state().favorites.contains(id);
        }
    }

    public static List<String> getFavorites() {
        synchronized (LOCK) {
            return List.copyOf(state().favorites);
        }
    }

    public static void toggleFavorite(String skinId) {
        String id = SkinIdUtil.normalize(skinId);
        if (!SkinIdUtil.hasSkin(id)) return;
        synchronized (LOCK) {
            State data = state();
            if (!data.favorites.remove(id)) data.favorites.add(id);
            saveState(data);
        }
    }

    public static boolean isExcludedPack(String packId) {
        if (packId == null || packId.isBlank()) return false;
        synchronized (LOCK) {
            return state().excludedPacks.contains(packId);
        }
    }

    public static void setPackExcluded(String packId, boolean excluded) {
        if (packId == null || packId.isBlank()) return;
        synchronized (LOCK) {
            State data = state();
            if (excluded) data.excludedPacks.add(packId);
            else data.excludedPacks.remove(packId);
            saveState(data);
        }
    }

    private static State state() {
        if (state != null) return state;
        boolean hasState = Files.exists(STATE_PATH);
        state = hasState ? readState(STATE_PATH) : importLegacyState();
        if (!hasState) saveState(state);
        return state;
    }

    private static State readState(Path path) {
        State data = new State();
        JsonObject root = readObject(path, "read");
        if (root == null) return data;
        JsonObject selection = object(root, "selection");
        data.selectionUserId = uuid(selection, "userId");
        data.selectionSkinId = SkinIdUtil.normalize(string(selection, "skinId"));
        readArray(root, "favorites", data.favorites, true);
        readArray(root, "excludedPacks", data.excludedPacks, false);
        return data;
    }

    private static State importLegacyState() {
        State data = new State();
        if (Files.exists(LEGACY_SELECTION_PATH)) {
            try {
                String[] lines = Files.readString(LEGACY_SELECTION_PATH, StandardCharsets.UTF_8).split("\\R", 3);
                if (lines.length >= 2) {
                    data.selectionUserId = UUID.fromString(lines[0].trim());
                    data.selectionSkinId = SkinIdUtil.normalize(lines[1].trim());
                }
            } catch (IOException | IllegalArgumentException ex) {
                DebugLog.debug("Failed to import {}", LEGACY_SELECTION_PATH);
            }
        }
        readArray(readObject(LEGACY_FAVORITES_PATH, "import"), "favourites", data.favorites, true);
        readArray(readObject(LEGACY_EXCLUSIONS_PATH, "import"), "excluded", data.excludedPacks, false);
        return data;
    }

    private static JsonObject readObject(Path path, String action) {
        if (!Files.exists(path)) return null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (IOException | RuntimeException ex) {
            DebugLog.debug("Failed to " + action + " " + path);
            return null;
        }
    }

    private static void readArray(JsonObject root, String key, LinkedHashSet<String> out, boolean skinIds) {
        JsonArray array = root == null ? null : root.getAsJsonArray(key);
        if (array == null) return;
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) continue;
            String value = value(element);
            if (value == null) continue;
            if (skinIds) value = SkinIdUtil.normalize(value);
            if ((skinIds ? SkinIdUtil.hasSkin(value) : !value.isBlank())) out.add(value);
        }
    }

    private static String value(JsonElement element) {
        try {
            return element.getAsString();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static JsonObject object(JsonObject root, String key) {
        return root != null && root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : null;
    }

    private static UUID uuid(JsonObject root, String key) {
        String value = string(root, key);
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String string(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonPrimitive()) return null;
        String value = root.get(key).getAsString();
        return value == null || value.isBlank() ? null : value;
    }

    private static void saveState(State data) {
        try {
            Files.createDirectories(STATE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(STATE_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(toJson(data), writer);
            }
        } catch (IOException | RuntimeException ex) {
            DebugLog.debug("Failed to save {}", STATE_PATH);
        }
    }

    private static JsonObject toJson(State data) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject selection = new JsonObject();
        if (data.selectionUserId != null) selection.addProperty("userId", data.selectionUserId.toString());
        else selection.add("userId", null);
        selection.addProperty("skinId", data.selectionSkinId);
        root.add("selection", selection);
        root.add("favorites", toJsonArray(data.favorites));
        root.add("excludedPacks", toJsonArray(data.excludedPacks));
        return root;
    }

    private static JsonArray toJsonArray(Iterable<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(value);
        return array;
    }

    private static final class State {
        private UUID selectionUserId;
        private String selectionSkinId = "";
        private final LinkedHashSet<String> favorites = new LinkedHashSet<>();
        private final LinkedHashSet<String> excludedPacks = new LinkedHashSet<>();
    }
}
