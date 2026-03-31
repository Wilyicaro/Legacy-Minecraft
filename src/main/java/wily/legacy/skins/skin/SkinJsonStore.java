package wily.legacy.Skins.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class SkinJsonStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private SkinJsonStore() { }
    static List<String> read(Path path, String key) {
        ArrayList<String> values = new ArrayList<>();
        if (!Files.exists(path)) return values;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            JsonArray array = obj == null ? null : obj.getAsJsonArray(key);
            if (array == null) return values;
            for (JsonElement element : array) {
                if (!element.isJsonPrimitive()) continue;
                String value = element.getAsString();
                if (value != null && !value.isBlank()) { values.add(value); }
            }
        } catch (IOException | RuntimeException ignored) { }
        return values;
    }
    static void write(Path path, String key, Collection<String> values) {
        try {
            Files.createDirectories(path.getParent());
            JsonObject obj = new JsonObject();
            JsonArray array = new JsonArray();
            for (String value : values) { array.add(value); }
            obj.add(key, array);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) { GSON.toJson(obj, writer); }
        } catch (IOException | RuntimeException ignored) { }
    }
}
