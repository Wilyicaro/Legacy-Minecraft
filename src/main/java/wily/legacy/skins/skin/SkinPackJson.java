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
}
