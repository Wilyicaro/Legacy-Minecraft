package wily.legacy.Skins.skin;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.server.packs.resources.Resource;
import wily.legacy.Skins.util.DebugLog;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
final class SkinPackJson {
    private SkinPackJson() { }
    static JsonObject readObject(Resource resource) { return readObject(resource, null); }
    static JsonObject readObject(Resource resource, String failureContext) {
        if (resource == null) return null;
        try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonElement parsed = JsonParser.parseReader(jsonReader);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ex) {
            if (failureContext != null && !failureContext.isBlank()) {
                DebugLog.warn("Failed to read {}: {}", failureContext, ex.toString());
            }
            return null;
        }
    }
    static String string(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        try {
            return element.getAsString();
        } catch (RuntimeException ignored) { return null; }
    }
    static int integer(JsonElement element, int defaultValue) {
        if (element == null || element.isJsonNull()) return defaultValue;
        try {
            return element.getAsInt();
        } catch (RuntimeException ignored) { return defaultValue; }
    }
    static boolean bool(JsonElement element, boolean defaultValue) {
        if (element == null || element.isJsonNull()) return defaultValue;
        try {
            return element.getAsBoolean();
        } catch (RuntimeException ignored) { return defaultValue; }
    }
    static ArrayList<JsonObject> readOrderedSkins(Path path) throws IOException {
        ArrayList<SkinJsonEntry> ordered = new ArrayList<>();
        JsonObject root = SkinPackFiles.readJson(path);
        if (!root.has("skins") || !root.get("skins").isJsonArray()) return new ArrayList<>();
        for (int i = 0; i < root.getAsJsonArray("skins").size(); i++) {
            JsonElement element = root.getAsJsonArray("skins").get(i);
            if (!element.isJsonObject()) continue;
            JsonObject skin = element.getAsJsonObject().deepCopy();
            ordered.add(new SkinJsonEntry(skin, integer(skin.get("order"), i + 1), i));
        }
        ordered.sort(Comparator.comparingInt(SkinJsonEntry::order).thenComparingInt(SkinJsonEntry::index));
        ArrayList<JsonObject> out = new ArrayList<>(ordered.size());
        for (SkinJsonEntry skin : ordered) out.add(skin.json());
        return out;
    }
    static void writeOrderedSkins(Path path, List<JsonObject> skins) throws IOException {
        JsonObject root = SkinPackFiles.readJson(path);
        var array = new com.google.gson.JsonArray();
        for (int i = 0; i < skins.size(); i++) {
            JsonObject skin = skins.get(i).deepCopy();
            skin.addProperty("order", i + 1);
            array.add(skin);
        }
        root.add("skins", array);
        SkinPackFiles.writeJson(path, root);
    }
    static int indexOfSkin(List<JsonObject> skins, String skinId) {
        for (int i = 0; i < skins.size(); i++) {
            if (skinId.equals(string(skins.get(i).get("id")))) return i;
        }
        return -1;
    }
    static LinkedHashSet<String> readSkinIds(Path path) throws IOException {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        JsonObject root = SkinPackFiles.readJson(path);
        if (!root.has("skins") || !root.get("skins").isJsonArray()) return ids;
        for (JsonElement element : root.getAsJsonArray("skins")) {
            if (!element.isJsonObject()) continue;
            String id = string(element.getAsJsonObject().get("id"));
            if (id != null && !id.isBlank()) ids.add(id);
        }
        return ids;
    }
    static int nextSkinOrder(Path path) throws IOException {
        int maxOrder = 0;
        JsonObject root = SkinPackFiles.readJson(path);
        if (!root.has("skins") || !root.get("skins").isJsonArray()) return 1;
        for (JsonElement element : root.getAsJsonArray("skins")) {
            if (!element.isJsonObject()) continue;
            maxOrder = Math.max(maxOrder, integer(element.getAsJsonObject().get("order"), 0));
        }
        return maxOrder + 1;
    }
    private record SkinJsonEntry(JsonObject json, int order, int index) { }
}
