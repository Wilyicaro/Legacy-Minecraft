package wily.legacy.skins.client.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SkinPackLang {
    private static volatile Map<String, String> EXTRA = Map.of();

    private SkinPackLang() {
    }

    public static String get(String key) {
        Map<String, String> m = EXTRA;
        return m.get(key);
    }

    public static String translate(String key, String fallback) {
        String value = translateOrNull(key);
        return value != null ? value : fallback;
    }

    public static String translateMaybeKey(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String key = value.startsWith("key:") ? value.substring(4) : value;
        String translated = translateOrNull(key);
        if (translated != null) return translated;
        int split = key.lastIndexOf('_');
        if (split > 0) {
            boolean digits = true;
            for (int i = split + 1; i < key.length(); i++) {
                char c = key.charAt(i);
                if (c < '0' || c > '9') {
                    digits = false;
                    break;
                }
            }
            if (digits) {
                translated = translateOrNull(key.substring(0, split));
                if (translated != null) return translated;
            }
        }
        return value.startsWith("key:") ? fallback : value;
    }

    public static void reload(ResourceManager rm) {
        if (rm == null) {
            EXTRA = Map.of();
            return;
        }

        String selected = selectedLanguageCode();
        LinkedHashMap<String, String> out = new LinkedHashMap<>();

        loadLocale(rm, "en_us", out);
        if (selected != null && !selected.isBlank()) {
            String sel = selected.toLowerCase(Locale.ROOT);
            if (!"en_us".equals(sel)) loadLocale(rm, sel, out);
        }

        EXTRA = Collections.unmodifiableMap(out);
    }

    private static String selectedLanguageCode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return "en_us";
        String selected = mc.getLanguageManager().getSelected();
        return selected == null || selected.isBlank() ? "en_us" : selected;
    }

    private static void loadLocale(ResourceManager rm, String locale, Map<String, String> out) {
        Map<ResourceLocation, Resource> packLang = new HashMap<>();
        packLang.putAll(listResources(rm, "skinpacks", rl -> {
            String path = rl.getPath();
            return path.contains("/lang/") && path.endsWith(".json");
        }));
        packLang.putAll(listResources(rm, "default_skinpacks", rl -> {
            String path = rl.getPath();
            return path.contains("/lang/") && path.endsWith(".json");
        }));

        Map<ResourceLocation, Resource> vanilla = listResources(rm, "lang", rl -> {
            String path = rl.getPath();
            return path.equals("lang/" + locale + ".json") || (path.startsWith("lang/" + locale + ".") && path.endsWith(".json"));
        });
        if (packLang.isEmpty() && vanilla.isEmpty()) return;

        ArrayList<ResourceLocation> keys = new ArrayList<>();
        keys.addAll(packLang.keySet());
        keys.addAll(vanilla.keySet());
        keys.sort(Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath));

        for (ResourceLocation rl : keys) {
            Resource resource = packLang.get(rl);
            if (resource == null) resource = vanilla.get(rl);
            if (resource == null) continue;
            JsonObject obj = readObj(resource);
            if (obj == null) continue;
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isBlank()) continue;
                String value = stringValue(entry.getValue());
                if (value != null && !value.isEmpty()) {
                    out.put(key, value);
                }
            }
        }
    }

    private static JsonObject readObj(Resource res) {
        try (Reader r = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
            JsonReader jr = new JsonReader(r);
            jr.setLenient(true);
            JsonElement e = JsonParser.parseReader(jr);
            return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static Map<ResourceLocation, Resource> listResources(ResourceManager rm, String root, java.util.function.Predicate<ResourceLocation> filter) {
        try {
            return rm.listResources(root, filter);
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private static String stringValue(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return element.getAsString();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String translateOrNull(String key) {
        if (key == null || key.isBlank()) return null;
        String extra = get(key);
        if (extra != null && !extra.isEmpty() && !extra.equals(key)) return extra;
        String value = I18n.get(key);
        return value == null || value.isEmpty() || value.equals(key) ? null : value;
    }
}
