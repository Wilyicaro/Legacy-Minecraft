package wily.legacy.Skins.client.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SkinPackLang {

    private static volatile Map<String, String> EXTRA = Map.of();

    private SkinPackLang() {
    }

    public static String get(String key) {
        Map<String, String> m = EXTRA;
        return m.get(key);
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
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return "en_us";
            Object lm;
            try {
                lm = mc.getClass().getMethod("getLanguageManager").invoke(mc);
            } catch (Throwable t) {
                lm = null;
            }
            if (lm == null) return "en_us";

            Object selected;
            try {
                selected = lm.getClass().getMethod("getSelected").invoke(lm);
            } catch (Throwable t) {
                try {
                    selected = lm.getClass().getMethod("getSelectedLanguage").invoke(lm);
                } catch (Throwable t2) {
                    selected = null;
                }
            }
            if (selected == null) return "en_us";

            try {
                Object code = selected.getClass().getMethod("getCode").invoke(selected);
                if (code instanceof String s && !s.isBlank()) return s;
            } catch (Throwable ignored) {
            }
            if (selected instanceof String s && !s.isBlank()) return s;
        } catch (Throwable ignored) {
        }
        return "en_us";
    }

    private static void loadLocale(ResourceManager rm, String locale, Map<String, String> out) {
        try {
            Map<ResourceLocation, Resource> res = new java.util.HashMap<>();
            try {
                res.putAll(rm.listResources("skinpacks", rl -> {
                    String p = rl.getPath();
                    return p.contains("/lang/") && p.endsWith(".json");
                }));
            } catch (Throwable ignored) {
            }
            try {
                res.putAll(rm.listResources("default_skinpacks", rl -> {
                    String p = rl.getPath();
                    return p.contains("/lang/") && p.endsWith(".json");
                }));
            } catch (Throwable ignored) {
            }

            Map<ResourceLocation, Resource> vanilla = rm.listResources("lang", rl -> {
                String p = rl.getPath();
                return p.equals("lang/" + locale + ".json") || (p.startsWith("lang/" + locale + ".") && p.endsWith(".json"));
            });

            if (res.isEmpty() && vanilla.isEmpty()) return;

            ArrayList<ResourceLocation> keys = new ArrayList<>();
            keys.addAll(res.keySet());
            keys.addAll(vanilla.keySet());
            keys.sort(Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath));

            for (ResourceLocation rl : keys) {
                Resource r = res.get(rl);
                if (r == null) r = vanilla.get(rl);
                if (r == null) continue;
                JsonObject obj = readObj(r);
                if (obj == null) continue;
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    String k = e.getKey();
                    if (k == null || k.isBlank()) continue;
                    JsonElement v = e.getValue();
                    if (v == null || !v.isJsonPrimitive()) continue;
                    try {
                        String s = v.getAsString();
                        if (s != null && !s.isEmpty()) out.put(k, s);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static JsonObject readObj(Resource res) {
        try (Reader r = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
            JsonReader jr = new JsonReader(r);
            jr.setLenient(true);
            JsonElement e = JsonParser.parseReader(jr);
            return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
