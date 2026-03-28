package wily.legacy.Skins.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import wily.legacy.Skins.util.DebugLog;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class SkinPackOrderRules {

    static final SkinPackOrderRules EMPTY = new SkinPackOrderRules(Map.of(), Map.of(), Map.of(), Map.of());

    private final Map<String, Integer> boxModelOrders;
    private final Map<String, Integer> legacyOrders;
    private final Map<String, Integer> bedrockOrders;
    private final Map<String, String> boxModelNames;

    private SkinPackOrderRules(Map<String, Integer> boxModelOrders,
                               Map<String, Integer> legacyOrders,
                               Map<String, Integer> bedrockOrders,
                               Map<String, String> boxModelNames) {
        this.boxModelOrders = boxModelOrders;
        this.legacyOrders = legacyOrders;
        this.bedrockOrders = bedrockOrders;
        this.boxModelNames = boxModelNames;
    }

    static SkinPackOrderRules load(ResourceManager resourceManager, ResourceLocation rulesFile) {
        if (resourceManager == null || rulesFile == null) return EMPTY;
        Resource resource = resourceManager.getResource(rulesFile).orElse(null);
        if (resource == null) return EMPTY;

        JsonObject root = readObj(resource);
        if (root == null) return EMPTY;

        JsonArray entries = root.has("entries") && root.get("entries").isJsonArray()
                ? root.getAsJsonArray("entries")
                : null;
        if (entries == null || entries.isEmpty()) return EMPTY;

        HashMap<String, Integer> boxModelOrders = new HashMap<>();
        HashMap<String, Integer> legacyOrders = new HashMap<>();
        HashMap<String, Integer> bedrockOrders = new HashMap<>();
        HashMap<String, String> boxModelNames = new HashMap<>();
        int invalidEntries = 0;

        for (JsonElement element : entries) {
            if (element == null || !element.isJsonObject()) {
                invalidEntries++;
                continue;
            }
            JsonObject entry = element.getAsJsonObject();

            String source = safeString(entry.get("source"));
            String packId = safeString(entry.get("pack_id"));
            String name = safeString(entry.get("name"));
            int order = safeInt(entry.get("order"), -1);
            if (source == null || source.isBlank() || packId == null || packId.isBlank() || order < 0) {
                invalidEntries++;
                continue;
            }

            switch (source.trim().toLowerCase(Locale.ROOT)) {
                case "box_model" -> {
                    putOrderRule(boxModelOrders, "box_model", packId, order);
                    putBoxModelName(boxModelNames, packId, name);
                }
                case "legacy_skins" -> putOrderRule(legacyOrders, "legacy_skins", packId, order);
                case "bedrock_skins" -> putOrderRule(bedrockOrders, "bedrock_skins", packId, order);
                default -> invalidEntries++;
            }
        }

        if (invalidEntries > 0) {
            DebugLog.warn("Ignored {} invalid skin pack order rule entries from {}", invalidEntries, rulesFile);
        }

        if (boxModelOrders.isEmpty() && legacyOrders.isEmpty() && bedrockOrders.isEmpty()) {
            return EMPTY;
        }
        return new SkinPackOrderRules(
                Map.copyOf(boxModelOrders),
                Map.copyOf(legacyOrders),
                Map.copyOf(bedrockOrders),
                Map.copyOf(boxModelNames)
        );
    }

    boolean hasEntries() {
        return !boxModelOrders.isEmpty() || !legacyOrders.isEmpty() || !bedrockOrders.isEmpty();
    }

    String managedBoxModelName(String packId) {
        if (packId == null || packId.isBlank()) return null;
        return boxModelNames.get(packId);
    }

    boolean hasManagedBoxModelOrder(String packId) {
        return packId != null && boxModelOrders.containsKey(packId);
    }

   
    Integer findManagedOrder(SkinPackSourceKind sourceKind, String packId) {
        if (!hasEntries() || sourceKind == null || packId == null || packId.isBlank()) return null;
        return switch (sourceKind) {
            case BOX_MODEL -> boxModelOrders.get(packId);
            case LEGACY_SKINS -> legacyOrders.get(packId);
            case BEDROCK_SKINS -> findBedrockPackOrder(packId);
            case SPECIAL -> null;
        };
    }

  
    int maxManagedOrder() {
        int max = 0;
        for (Integer value : boxModelOrders.values()) {
            if (value != null && value > max) max = value;
        }
        for (Integer value : legacyOrders.values()) {
            if (value != null && value > max) max = value;
        }
        for (Integer value : bedrockOrders.values()) {
            if (value != null && value > max) max = value;
        }
        return max;
    }

    Integer findBoxModelPackOrder(String namespace, String packId, String builtinPackNamespace) {
        if (SkinIds.PACK_DEFAULT.equals(packId)) return 0;
        if (!hasEntries()) return null;
        if (packId == null || packId.isBlank()) return null;
        if (builtinPackNamespace == null || !builtinPackNamespace.equals(namespace)) return null;
        return boxModelOrders.get(packId);
    }

    Integer findLegacyPackOrder(String packId) {
        if (!hasEntries() || packId == null || packId.isBlank()) return null;
        return legacyOrders.get(packId);
    }

    Integer findBedrockPackOrder(String packId) {
        if (!hasEntries() || packId == null || packId.isBlank()) return null;

        Integer direct = bedrockOrders.get(packId);
        if (direct != null) return direct;

        if (packId.startsWith("skinpack.") && packId.length() > "skinpack.".length()) {
            Integer stripped = bedrockOrders.get(packId.substring("skinpack.".length()));
            if (stripped != null) return stripped;
        }

        for (Map.Entry<String, Integer> entry : bedrockOrders.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(packId)) return entry.getValue();
            if (packId.startsWith("skinpack.")
                    && entry.getKey().equalsIgnoreCase(packId.substring("skinpack.".length()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static void putOrderRule(Map<String, Integer> target, String source, String packId, int order) {
        Integer previous = target.put(packId, order);
        if (previous != null && previous != order) {
            DebugLog.warn("Duplicate skin pack order rule for {} {}: keeping last order {} over {}", source, packId, order, previous);
        }
    }

    private static void putBoxModelName(Map<String, String> target, String packId, String name) {
        if (name == null || name.isBlank()) return;
        String previous = target.put(packId, name);
        if (previous != null && !previous.equals(name)) {
            DebugLog.warn("Duplicate box-model display name rule for {}: keeping last name '{}' over '{}'", packId, name, previous);
        }
    }

    private static JsonObject readObj(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonElement parsed = JsonParser.parseReader(jsonReader);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Throwable throwable) {
            DebugLog.warn("Failed to read skin pack order rules: {}", throwable.toString());
            return null;
        }
    }

    private static String safeString(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        try {
            return element.getAsString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int safeInt(JsonElement element, int defaultValue) {
        if (element == null || element.isJsonNull()) return defaultValue;
        try {
            return element.getAsInt();
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }
}