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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class SkinPackFamilyRules {

    static final SkinPackFamilyRules EMPTY = new SkinPackFamilyRules(Map.of(), Map.of(), Map.of());

    private final Map<String, FamilyRule> familiesById;
    private final Map<String, String> familyByMemberKey;
    private final Map<String, String> familyByAliasKey;

    private SkinPackFamilyRules(Map<String, FamilyRule> familiesById,
                                Map<String, String> familyByMemberKey,
                                Map<String, String> familyByAliasKey) {
        this.familiesById = familiesById;
        this.familyByMemberKey = familyByMemberKey;
        this.familyByAliasKey = familyByAliasKey;
    }

    static SkinPackFamilyRules load(ResourceManager resourceManager, ResourceLocation rulesFile) {
        if (resourceManager == null || rulesFile == null) return EMPTY;
        Resource resource = resourceManager.getResource(rulesFile).orElse(null);
        if (resource == null) return EMPTY;

        JsonObject root = readObj(resource);
        if (root == null) return EMPTY;
        Set<String> declaredBoxModelPackIds = loadDeclaredBoxModelPackIds(resourceManager, rulesFile);

        LinkedHashMap<String, FamilyRule> familiesById = new LinkedHashMap<>();
        HashMap<String, String> familyByMemberKey = new HashMap<>();
        HashMap<String, String> familyByAliasKey = new HashMap<>();
        LoadCounts counts = new LoadCounts();

        if (root.has("families") && root.get("families").isJsonArray()) {
            loadLegacyFamiliesArray(root.getAsJsonArray("families"), declaredBoxModelPackIds, familiesById, familyByMemberKey, familyByAliasKey, counts);
        } else {
            loadCompactFamiliesObject(root, declaredBoxModelPackIds, familiesById, familyByMemberKey, familyByAliasKey, counts);
        }

        if (counts.invalidFamilies > 0) {
            DebugLog.warn("Ignored {} invalid skin pack family rules from {}", counts.invalidFamilies, rulesFile);
        }
        if (counts.invalidMembers > 0) {
            DebugLog.warn("Ignored {} invalid skin pack family members from {}", counts.invalidMembers, rulesFile);
        }

        if (familiesById.isEmpty()) return EMPTY;
        return new SkinPackFamilyRules(
                Map.copyOf(familiesById),
                Map.copyOf(familyByMemberKey),
                Map.copyOf(familyByAliasKey)
        );
    }

    private static void loadLegacyFamiliesArray(JsonArray families,
                                                Set<String> declaredBoxModelPackIds,
                                                Map<String, FamilyRule> familiesById,
                                                Map<String, String> familyByMemberKey,
                                                Map<String, String> familyByAliasKey,
                                                LoadCounts counts) {
        if (families == null || families.isEmpty()) return;
        for (JsonElement element : families) {
            if (element == null || !element.isJsonObject()) {
                counts.invalidFamilies++;
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String familyId = safeString(object.get("family_id"));
            if (familyId == null || familyId.isBlank()) {
                counts.invalidFamilies++;
                continue;
            }
            registerFamilyRule(familyId, object, declaredBoxModelPackIds, familiesById, familyByMemberKey, familyByAliasKey, counts);
        }
    }

    private static void loadCompactFamiliesObject(JsonObject root,
                                                  Set<String> declaredBoxModelPackIds,
                                                  Map<String, FamilyRule> familiesById,
                                                  Map<String, String> familyByMemberKey,
                                                  Map<String, String> familyByAliasKey,
                                                  LoadCounts counts) {
        if (root == null || root.entrySet().isEmpty()) return;
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String familyId = entry.getKey();
            JsonElement element = entry.getValue();
            if (familyId == null || familyId.isBlank() || element == null || !element.isJsonObject()) {
                counts.invalidFamilies++;
                continue;
            }
            registerFamilyRule(familyId, element.getAsJsonObject(), declaredBoxModelPackIds, familiesById, familyByMemberKey, familyByAliasKey, counts);
        }
    }

    private static void registerFamilyRule(String familyId,
                                           JsonObject object,
                                           Set<String> declaredBoxModelPackIds,
                                           Map<String, FamilyRule> familiesById,
                                           Map<String, String> familyByMemberKey,
                                           Map<String, String> familyByAliasKey,
                                           LoadCounts counts) {
        if (familyId == null || familyId.isBlank() || object == null) {
            counts.invalidFamilies++;
            return;
        }
        if (familiesById.containsKey(familyId)) {
            DebugLog.warn("Duplicate skin pack family rule for {}: keeping the first definition", familyId);
            return;
        }

        SkinPackSourceKind preferredSource = parseSourceKind(safeString(object.get("preferred_source")));
        if (preferredSource == null) preferredSource = SkinPackSourceKind.BOX_MODEL;

        ArrayList<MemberRule> members = new ArrayList<>();
        collectLegacyMembers(object, members, counts);
        collectCompactMembers(object, "box_model", SkinPackSourceKind.BOX_MODEL, members, counts);
        collectCompactMembers(object, "legacy_skins", SkinPackSourceKind.LEGACY_SKINS, members, counts);
        collectCompactMembers(object, "bedrock_skins", SkinPackSourceKind.BEDROCK_SKINS, members, counts);

        ArrayList<String> aliases = new ArrayList<>();
        collectAliases(object, aliases);

        if (members.isEmpty() && aliases.isEmpty()) {
            counts.invalidFamilies++;
            return;
        }

        if (declaredBoxModelPackIds.contains(familyId) && members.stream().noneMatch(member -> member.sourceKind() == SkinPackSourceKind.BOX_MODEL)) {
            members.add(0, new MemberRule(SkinPackSourceKind.BOX_MODEL, familyId));
        }

        FamilyRule rule = new FamilyRule(familyId, preferredSource, List.copyOf(members), List.copyOf(aliases));
        familiesById.put(familyId, rule);

        for (MemberRule member : members) {
            registerMemberRule(familyByMemberKey, member, familyId);
            registerAliasKeys(familyByAliasKey, member.packId(), familyId);
        }
        for (String alias : aliases) {
            registerAliasKeys(familyByAliasKey, alias, familyId);
        }
        registerAliasKeys(familyByAliasKey, familyId, familyId);
    }

    private static void collectLegacyMembers(JsonObject object, List<MemberRule> members, LoadCounts counts) {
        JsonArray membersArray = object.has("members") && object.get("members").isJsonArray()
                ? object.getAsJsonArray("members")
                : null;
        if (membersArray == null) return;
        for (JsonElement memberElement : membersArray) {
            if (memberElement == null || !memberElement.isJsonObject()) {
                counts.invalidMembers++;
                continue;
            }
            JsonObject memberObject = memberElement.getAsJsonObject();
            SkinPackSourceKind sourceKind = parseSourceKind(safeString(memberObject.get("source")));
            String packId = safeString(memberObject.get("pack_id"));
            if (sourceKind == null || packId == null || packId.isBlank()) {
                counts.invalidMembers++;
                continue;
            }
            members.add(new MemberRule(sourceKind, packId));
        }
    }

    private static void collectCompactMembers(JsonObject object,
                                              String fieldName,
                                              SkinPackSourceKind sourceKind,
                                              List<MemberRule> members,
                                              LoadCounts counts) {
        if (object == null || fieldName == null || fieldName.isBlank() || sourceKind == null) return;
        JsonElement field = object.get(fieldName);
        if (field == null || field.isJsonNull()) return;
        if (field.isJsonArray()) {
            for (JsonElement element : field.getAsJsonArray()) {
                String packId = safeString(element);
                if (packId == null || packId.isBlank()) {
                    counts.invalidMembers++;
                    continue;
                }
                members.add(new MemberRule(sourceKind, packId));
            }
            return;
        }
        String packId = safeString(field);
        if (packId == null || packId.isBlank()) {
            counts.invalidMembers++;
            return;
        }
        members.add(new MemberRule(sourceKind, packId));
    }

    private static void collectAliases(JsonObject object, List<String> aliases) {
        JsonArray aliasesArray = object.has("aliases") && object.get("aliases").isJsonArray()
                ? object.getAsJsonArray("aliases")
                : null;
        if (aliasesArray == null) return;
        for (JsonElement aliasElement : aliasesArray) {
            String alias = safeString(aliasElement);
            if (alias != null && !alias.isBlank()) aliases.add(alias);
        }
    }

    private static Set<String> loadDeclaredBoxModelPackIds(ResourceManager resourceManager, ResourceLocation rulesFile) {
        ResourceLocation manifestFile = siblingPath(rulesFile, "manifest.json");
        if (resourceManager == null || manifestFile == null) return Set.of();
        Resource resource = resourceManager.getResource(manifestFile).orElse(null);
        if (resource == null) return Set.of();

        JsonObject root = readObj(resource);
        if (root == null) return Set.of();
        JsonArray packs = root.has("packs") && root.get("packs").isJsonArray() ? root.getAsJsonArray("packs") : null;
        if (packs == null || packs.isEmpty()) return Set.of();

        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (JsonElement element : packs) {
            if (element == null || !element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            String id = safeString(object.get("id"));
            if (id != null && !id.isBlank()) ids.add(id);
        }
        return ids.isEmpty() ? Set.of() : Set.copyOf(ids);
    }

    private static ResourceLocation siblingPath(ResourceLocation path, String fileName) {
        if (path == null || fileName == null || fileName.isBlank()) return null;
        String rawPath = path.getPath();
        int slash = rawPath.lastIndexOf('/');
        String sibling = slash >= 0 ? rawPath.substring(0, slash + 1) + fileName : fileName;
        return ResourceLocation.fromNamespaceAndPath(path.getNamespace(), sibling);
    }

    boolean hasEntries() {
        return !familiesById.isEmpty();
    }

    String findFamilyId(SkinPackSourceKind sourceKind, String packId, String packName) {
        if (sourceKind == null || packId == null || packId.isBlank()) return null;

        String direct = findFamilyIdByMemberKey(sourceKind, packId);
        if (direct != null) return direct;

        String byPackIdAlias = findFamilyIdByAlias(packId);
        if (byPackIdAlias != null) return byPackIdAlias;

        return findFamilyIdByAlias(packName);
    }

    String familyIdForMember(SkinPackSourceKind sourceKind, String packId) {
        if (sourceKind == null || packId == null || packId.isBlank()) return null;
        return findFamilyIdByMemberKey(sourceKind, packId);
    }

    SkinPackSourceKind preferredSource(String familyId) {
        FamilyRule rule = familyId == null ? null : familiesById.get(familyId);
        return rule == null ? SkinPackSourceKind.BOX_MODEL : rule.preferredSource();
    }

    Integer findManagedFamilyOrder(String familyId, SkinPackOrderRules orderRules) {
        if (familyId == null || familyId.isBlank() || orderRules == null || !orderRules.hasEntries()) return null;
        FamilyRule rule = familiesById.get(familyId);
        if (rule == null) return null;

        Integer best = null;
        for (MemberRule member : rule.members()) {
            Integer order = orderRules.findManagedOrder(member.sourceKind(), member.packId());
            if (order == null) continue;
            if (best == null || order < best) best = order;
        }
        return best;
    }

    private String findFamilyIdByAlias(String value) {
        if (value == null || value.isBlank()) return null;
        for (String aliasKey : SkinPackDuplicateResolver.buildDuplicatePackAliasKeys(value)) {
            String familyId = familyByAliasKey.get(aliasKey);
            if (familyId != null) return familyId;
        }
        String normalizedName = SkinPackDuplicateResolver.normalizeDuplicatePackNameKey(value);
        return normalizedName == null ? null : familyByAliasKey.get("name:" + normalizedName);
    }

    private static void registerMemberRule(Map<String, String> familyByMemberKey, MemberRule member, String familyId) {
        boolean registered = false;
        for (String memberKey : memberKeys(member.sourceKind(), member.packId())) {
            String previous = familyByMemberKey.putIfAbsent(memberKey, familyId);
            if (previous != null && !previous.equals(familyId)) {
                DebugLog.warn("Skin pack family member {} is claimed by both {} and {}: keeping {}", member.packId(), previous, familyId, previous);
                return;
            }
            registered = true;
        }
        if (!registered) return;
    }

    private static void registerAliasKeys(Map<String, String> familyByAliasKey, String alias, String familyId) {
        if (familyByAliasKey == null || familyId == null || familyId.isBlank() || alias == null || alias.isBlank()) return;
        for (String aliasKey : SkinPackDuplicateResolver.buildDuplicatePackAliasKeys(alias)) {
            registerAliasKey(familyByAliasKey, aliasKey, familyId, alias);
        }
        if (!alias.regionMatches(true, 0, "skinpack.", 0, "skinpack.".length()) && alias.indexOf(':') < 0) {
            for (String aliasKey : SkinPackDuplicateResolver.buildDuplicatePackAliasKeys("skinpack." + alias)) {
                registerAliasKey(familyByAliasKey, aliasKey, familyId, alias);
            }
        }
        String normalizedName = SkinPackDuplicateResolver.normalizeDuplicatePackNameKey(alias);
        if (normalizedName != null) {
            registerAliasKey(familyByAliasKey, "name:" + normalizedName, familyId, alias);
        }
    }

    private static void registerAliasKey(Map<String, String> familyByAliasKey, String aliasKey, String familyId, String aliasText) {
        if (aliasKey == null || aliasKey.isBlank()) return;
        String previous = familyByAliasKey.putIfAbsent(aliasKey, familyId);
        if (previous != null && !previous.equals(familyId)) {
            DebugLog.warn("Skin pack family alias '{}' is claimed by both {} and {}: keeping {}", aliasText, previous, familyId, previous);
        }
    }

    private String findFamilyIdByMemberKey(SkinPackSourceKind sourceKind, String packId) {
        for (String memberKey : memberKeys(sourceKind, packId)) {
            String familyId = familyByMemberKey.get(memberKey);
            if (familyId != null) return familyId;
        }
        return null;
    }

    private static List<String> memberKeys(SkinPackSourceKind sourceKind, String packId) {
        String normalized = SkinPackDuplicateResolver.normalizeDuplicatePackKey(packId);
        if (sourceKind == null || normalized == null) return List.of();

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add(sourceKind.name() + "|" + normalized);

        String strippedNamespace = SkinPackDuplicateResolver.stripNamespace(normalized);
        if (strippedNamespace != null && !strippedNamespace.isBlank() && !strippedNamespace.equals(normalized)) {
            keys.add(sourceKind.name() + "|" + strippedNamespace);
        }

        if (sourceKind == SkinPackSourceKind.BEDROCK_SKINS) {
            String bedrockStripped = SkinPackDuplicateResolver.stripBedrockPackPrefix(normalized);
            if (bedrockStripped != null && !bedrockStripped.isBlank() && !bedrockStripped.equals(normalized)) {
                keys.add(sourceKind.name() + "|" + bedrockStripped);
            }
        }

        return List.copyOf(keys);
    }

    private static SkinPackSourceKind parseSourceKind(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "box_model" -> SkinPackSourceKind.BOX_MODEL;
            case "legacy_skins" -> SkinPackSourceKind.LEGACY_SKINS;
            case "bedrock_skins" -> SkinPackSourceKind.BEDROCK_SKINS;
            default -> null;
        };
    }

    private static JsonObject readObj(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonElement parsed = JsonParser.parseReader(jsonReader);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Throwable throwable) {
            DebugLog.warn("Failed to read skin pack family rules: {}", throwable.toString());
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

    private record FamilyRule(String id,
                              SkinPackSourceKind preferredSource,
                              List<MemberRule> members,
                              List<String> aliases) {
    }

    private record MemberRule(SkinPackSourceKind sourceKind, String packId) {
    }

    private static final class LoadCounts {
        private int invalidFamilies;
        private int invalidMembers;
    }
}
