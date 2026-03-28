package wily.legacy.Skins.skin;

import net.minecraft.resources.ResourceLocation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SkinPackDuplicateResolver {
    private static final List<KnownExternalPackMapping> KNOWN_EXTERNAL_PACK_MAPPINGS = createKnownExternalPackMappings();
    private static final Map<String, String> DUPLICATE_PACK_ALIAS_OVERRIDES = createDuplicatePackAliasOverrides();
    private static final Map<String, Integer> LEGACY_PACK_SORT_INDEX_OVERRIDES = createLegacyPackSortIndexOverrides();

    private SkinPackDuplicateResolver() {
    }

    static Integer findLegacyPackSortIndexOverride(ResourceLocation packLoc, String packId, String packName) {
        return findExternalPackSortIndexOverride(packLoc, packId, packName);
    }

    private static KnownExternalPackMapping packMapping(String preferredPackId,
                                                        int sortIndex,
                                                        String... aliases) {
        ArrayList<String> values = new ArrayList<>();
        values.add(preferredPackId);
        if (aliases != null) {
            Collections.addAll(values, aliases);
        }
        return new KnownExternalPackMapping(preferredPackId, sortIndex, List.copyOf(values));
    }

    private record KnownExternalPackMapping(String preferredPackId,
                                            int sortIndex,
                                            List<String> aliases) {
    }

    static String findNewFormatDuplicatePackId(ResourceLocation packLoc,
                                                       String packId,
                                                       String packName,
                                                       List<SkinEntry> entries,
                                                       Map<String, String> preferredNewFormatPackAliases,
                                                       Map<String, String> preferredNewFormatPackFingerprints,
                                                       Map<String, List<String>> preferredNewFormatPackTokens) {
        if ((preferredNewFormatPackAliases == null || preferredNewFormatPackAliases.isEmpty())
                && (preferredNewFormatPackFingerprints == null || preferredNewFormatPackFingerprints.isEmpty())
                && (preferredNewFormatPackTokens == null || preferredNewFormatPackTokens.isEmpty())) {
            return null;
        }
        if (packLoc == null && (packId == null || packId.isBlank())) return null;

        String knownPreferredPackId = findKnownExternalPreferredPackId(packLoc, packId, packName, preferredNewFormatPackAliases);
        if (knownPreferredPackId != null) return knownPreferredPackId;

        if (packId != null && !packId.isBlank()) {
            String direct = findNewFormatDuplicateAlias(preferredNewFormatPackAliases, packId);
            if (direct != null) return direct;
            String stripped = findNewFormatDuplicateAlias(preferredNewFormatPackAliases, stripNamespace(packId));
            if (stripped != null) return stripped;
        }

        if (packLoc != null) {
            String namespacedPath = packLoc.getNamespace() + ":" + packLoc.getPath();
            String byNamespacedPath = findNewFormatDuplicateAlias(preferredNewFormatPackAliases, namespacedPath);
            if (byNamespacedPath != null) return byNamespacedPath;
            String byPath = findNewFormatDuplicateAlias(preferredNewFormatPackAliases, packLoc.getPath());
            if (byPath != null) return byPath;
        }

        String byName = findNewFormatDuplicateNameAlias(preferredNewFormatPackAliases, packName);
        if (byName != null) return byName;

        String byFingerprint = findNewFormatDuplicateFingerprint(preferredNewFormatPackFingerprints, packName, entries);
        if (byFingerprint != null) return byFingerprint;

        return findNewFormatApproximateDuplicatePackId(preferredNewFormatPackTokens, entries);
    }

    static String findKnownExternalPreferredPackId(ResourceLocation packLoc,
                                                           String packId,
                                                           String packName,
                                                           Map<String, String> newFormatPackAliases) {
        if (newFormatPackAliases == null || newFormatPackAliases.isEmpty()) return null;

        KnownExternalPackMapping mapping = findKnownExternalPackMapping(packLoc, packId, packName);
        if (mapping == null) return null;

        String preferredPackId = mapping.preferredPackId();
        String aliasMatch = findNewFormatDuplicateAlias(newFormatPackAliases, preferredPackId);
        if (aliasMatch != null) return aliasMatch;
        return newFormatPackAliases.containsValue(preferredPackId) ? preferredPackId : null;
    }

    static Integer findExternalPackSortIndexOverride(ResourceLocation packLoc,
                                                             String packId,
                                                             String packName) {
        String compactPackId = compactDuplicatePackAlias(stripNamespace(packId));
        if (compactPackId != null) {
            Integer override = LEGACY_PACK_SORT_INDEX_OVERRIDES.get(compactPackId);
            if (override != null) return override;
        }

        if (packLoc != null) {
            String compactPath = compactDuplicatePackAlias(packLoc.getPath());
            if (compactPath != null) {
                Integer override = LEGACY_PACK_SORT_INDEX_OVERRIDES.get(compactPath);
                if (override != null) return override;
            }
        }

        String normalizedName = normalizeDuplicatePackNameKey(packName);
        if (normalizedName != null) {
            Integer override = LEGACY_PACK_SORT_INDEX_OVERRIDES.get(normalizedName);
            if (override != null) return override;
        }

        KnownExternalPackMapping mapping = findKnownExternalPackMapping(packLoc, packId, packName);
        return mapping == null ? null : mapping.sortIndex();
    }

    static KnownExternalPackMapping findKnownExternalPackMapping(ResourceLocation packLoc,
                                                                        String packId,
                                                                        String packName) {
        for (KnownExternalPackMapping mapping : KNOWN_EXTERNAL_PACK_MAPPINGS) {
            if (matchesKnownExternalPackMapping(mapping, packId)) return mapping;
            if (matchesKnownExternalPackMapping(mapping, stripNamespace(packId))) return mapping;
            if (packLoc != null && matchesKnownExternalPackMapping(mapping, packLoc.getPath())) return mapping;
            if (matchesKnownExternalPackMapping(mapping, packName)) return mapping;
        }
        return null;
    }

    static boolean matchesKnownExternalPackMapping(KnownExternalPackMapping mapping,
                                                           String value) {
        if (mapping == null || value == null || value.isBlank()) return false;

        String normalizedValue = normalizeDuplicatePackKey(value);
        String normalizedName = normalizeDuplicatePackNameKey(value);
        String compactValue = compactDuplicatePackAlias(stripNamespace(value));

        for (String alias : mapping.aliases()) {
            String normalizedAlias = normalizeDuplicatePackKey(alias);
            if (normalizedAlias != null && normalizedAlias.equals(normalizedValue)) return true;

            String normalizedAliasName = normalizeDuplicatePackNameKey(alias);
            if (normalizedAliasName != null && normalizedAliasName.equals(normalizedName)) return true;

            String compactAlias = compactDuplicatePackAlias(stripNamespace(alias));
            if (compactAlias != null && compactAlias.equals(compactValue)) return true;
        }
        return false;
    }

    static String findNewFormatDuplicateNameAlias(Map<String, String> newFormatPackAliases,
                                                          String packName) {
        if (newFormatPackAliases == null || newFormatPackAliases.isEmpty()) return null;
        String normalizedPackName = normalizeDuplicatePackNameKey(packName);
        return normalizedPackName == null ? null : newFormatPackAliases.get("name:" + normalizedPackName);
    }

    static String findNewFormatDuplicateFingerprint(Map<String, String> preferredNewFormatPackFingerprints,
                                                            String packName,
                                                            List<SkinEntry> entries) {
        if (preferredNewFormatPackFingerprints == null || preferredNewFormatPackFingerprints.isEmpty()) return null;
        for (String fingerprint : buildDuplicatePackFingerprints(packName, entries)) {
            String match = preferredNewFormatPackFingerprints.get(fingerprint);
            if (match != null) return match;
        }
        return null;
    }

    static String findNewFormatApproximateDuplicatePackId(Map<String, List<String>> preferredNewFormatPackTokens,
                                                                  List<SkinEntry> entries) {
        if (preferredNewFormatPackTokens == null || preferredNewFormatPackTokens.isEmpty()) return null;
        List<String> importedTokens = buildDuplicatePackTokens(entries);
        if (importedTokens.size() < 4) return null;

        String bestPackId = null;
        double bestScore = 0.0D;
        double bestCoverage = 0.0D;
        for (Map.Entry<String, List<String>> candidate : preferredNewFormatPackTokens.entrySet()) {
            List<String> preferredTokens = candidate.getValue();
            if (preferredTokens == null || preferredTokens.size() < 4) continue;

            int common = longestCommonTokenSubsequence(importedTokens, preferredTokens);
            if (common <= 0) continue;

            int longest = Math.max(importedTokens.size(), preferredTokens.size());
            int shortest = Math.min(importedTokens.size(), preferredTokens.size());
            double score = (double) common / (double) longest;
            double coverage = (double) common / (double) shortest;
            if (coverage < 0.82D || score < 0.68D) continue;

            if (score > bestScore || (score == bestScore && coverage > bestCoverage)) {
                bestScore = score;
                bestCoverage = coverage;
                bestPackId = candidate.getKey();
            }
        }
        return bestPackId;
    }

    static List<String> buildDuplicatePackFingerprints(String packName,
                                                               List<SkinEntry> entries) {
        List<String> tokens = buildDuplicatePackTokens(entries);
        String normalizedPackName = normalizeDuplicatePackNameKey(packName);
        if (tokens.isEmpty()) {
            return normalizedPackName == null ? List.of() : List.of("name:" + normalizedPackName);
        }

        LinkedHashSet<String> fingerprints = new LinkedHashSet<>();
        String tokenFingerprint = String.join("|", tokens);
        fingerprints.add("tokens:" + tokenFingerprint);

        if (normalizedPackName != null) {
            fingerprints.add("name+tokens:" + normalizedPackName + "|" + tokenFingerprint);
        }

        int headLength = Math.min(tokens.size(), 24);
        if (headLength > 0) {
            String headFingerprint = String.join("|", tokens.subList(0, headLength));
            fingerprints.add("head:" + headFingerprint);
            if (normalizedPackName != null) {
                fingerprints.add("name+head:" + normalizedPackName + "|" + headFingerprint);
            }
        }

        return List.copyOf(fingerprints);
    }

    static List<String> buildDuplicatePackTokens(List<SkinEntry> entries) {
        if (entries == null || entries.isEmpty()) return List.of();

        ArrayList<String> tokens = new ArrayList<>();
        for (SkinEntry entry : entries) {
            if (entry == null) continue;

            int before = tokens.size();
            appendDuplicatePackTokens(tokens, entry.name());
            if (tokens.size() > before) continue;

            String fallbackId = stripNamespace(entry.id());
            if (fallbackId == null || fallbackId.isBlank()) continue;
            int slash = fallbackId.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < fallbackId.length()) {
                fallbackId = fallbackId.substring(slash + 1);
            }
            appendDuplicatePackTokens(tokens, fallbackId);
        }

        return tokens.isEmpty() ? List.of() : List.copyOf(tokens);
    }

    static void appendDuplicatePackTokens(List<String> tokens,
                                                  String value) {
        if (tokens == null || value == null || value.isBlank()) return;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.replace('&', ' ');
        normalized = normalized.replaceAll("[^a-z0-9]+", " ").trim();
        if (normalized.isBlank()) return;

        for (String token : normalized.split("\\s+")) {
            if (!token.isBlank()) tokens.add(token);
        }
    }

    static int longestCommonTokenSubsequence(List<String> left,
                                                     List<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) return 0;

        int[] previous = new int[right.size() + 1];
        for (int i = 1; i <= left.size(); i++) {
            int[] current = new int[right.size() + 1];
            String leftToken = left.get(i - 1);
            for (int j = 1; j <= right.size(); j++) {
                if (leftToken.equals(right.get(j - 1))) {
                    current[j] = previous[j - 1] + 1;
                } else {
                    current[j] = Math.max(previous[j], current[j - 1]);
                }
            }
            previous = current;
        }
        return previous[right.size()];
    }

    static String normalizeDuplicatePackKey(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.replace('\\', '/');
        normalized = normalized.replace(' ', '_');
        normalized = normalized.replace('-', '_');
        normalized = normalized.replaceAll("[^a-z0-9_:/]+", "");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("/+", "/");
        normalized = normalized.replaceAll(":+", ":");
        normalized = normalized.replaceAll("^[:/_]+|[:/_]+$", "");
        return normalized.isBlank() ? null : normalized;
    }

    static String findNewFormatDuplicateAlias(Map<String, String> newFormatPackAliases,
                                                      String alias) {
        if (newFormatPackAliases == null || newFormatPackAliases.isEmpty()) return null;
        for (String aliasKey : buildDuplicatePackAliasKeys(alias)) {
            String match = newFormatPackAliases.get(aliasKey);
            if (match != null) return match;
        }
        return null;
    }

    static List<String> buildDuplicatePackAliasKeys(String value) {
        String normalized = normalizeDuplicatePackKey(value);
        if (normalized == null) return List.of();

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add(normalized);

        String bedrockStripped = stripBedrockPackPrefix(normalized);
        if (bedrockStripped != null && !bedrockStripped.isBlank() && !bedrockStripped.equals(normalized)) {
            keys.add(bedrockStripped);
        }

        String stripped = stripNamespace(normalized);
        if (stripped != null && !stripped.isBlank()) {
            keys.add(stripped);
            String compact = compactDuplicatePackAlias(stripped);
            if (compact != null) {
                keys.add("compact:" + compact);
                String overridden = DUPLICATE_PACK_ALIAS_OVERRIDES.get(compact);
                if (overridden != null && !overridden.isBlank()) {
                    keys.add("compact:" + overridden);
                }
            }
        }

        return List.copyOf(keys);
    }

    static String stripBedrockPackPrefix(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = normalizeDuplicatePackKey(value);
        if (normalized == null || normalized.isBlank()) return normalized;
        if (normalized.startsWith("skinpack.")) return normalizeDuplicatePackKey(normalized.substring("skinpack.".length()));
        if (normalized.startsWith("skinpack_")) return normalizeDuplicatePackKey(normalized.substring("skinpack_".length()));
        if (normalized.startsWith("skinpack/")) return normalizeDuplicatePackKey(normalized.substring("skinpack/".length()));
        return normalized;
    }

    static String compactDuplicatePackAlias(String value) {
        if (value == null || value.isBlank()) return null;
        String compact = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        return compact.isBlank() ? null : compact;
    }

    static Map<String, String> createDuplicatePackAliasOverrides() {
        HashMap<String, String> aliases = new HashMap<>();
        for (KnownExternalPackMapping mapping : KNOWN_EXTERNAL_PACK_MAPPINGS) {
            String preferredAlias = compactDuplicatePackAlias(mapping.preferredPackId());
            if (preferredAlias == null) continue;

            for (String alias : mapping.aliases()) {
                String compactAlias = compactDuplicatePackAlias(alias);
                if (compactAlias != null && !compactAlias.equals(preferredAlias)) {
                    aliases.putIfAbsent(compactAlias, preferredAlias);
                }
            }
        }
        return Map.copyOf(aliases);
    }

    static Map<String, Integer> createLegacyPackSortIndexOverrides() {
        HashMap<String, Integer> sorts = new HashMap<>();
        for (KnownExternalPackMapping mapping : KNOWN_EXTERNAL_PACK_MAPPINGS) {
            registerPackSortIndexOverride(sorts, mapping.preferredPackId(), mapping.sortIndex());
            for (String alias : mapping.aliases()) {
                registerPackSortIndexOverride(sorts, alias, mapping.sortIndex());
            }
        }
        return Map.copyOf(sorts);
    }

    static List<KnownExternalPackMapping> createKnownExternalPackMappings() {
        return List.of(
                packMapping("skinpack1", 3700, "sp1", "skin_pack_1", "skin pack 1"),
                packMapping("skinpack2", 3800, "sp2", "skin_pack_2", "skin pack 2"),
                packMapping("skinpack3", 3900, "sp3", "skin_pack_3", "skin pack 3"),
                packMapping("skinpack4", 3925, "sp4", "skin_pack_4", "skin pack 4"),
                packMapping("skinpack5", 3950, "sp5", "skin_pack_5", "skin pack 5"),
                packMapping("skinpack6", 3960, "sp6", "skin_pack_6", "skin pack 6"),
                packMapping("birthday", 400, "birthday_1", "birthday1", "birthday skin pack", "1st birthday skin pack", "first birthday skin pack"),
                packMapping("birthday2", 300, "birthday_2", "birthday2", "2nd birthday skin pack", "second birthday skin pack"),
                packMapping("birthday3", 200, "birthday_3", "birthday3", "3rd birthday skin pack", "third birthday skin pack"),
                packMapping("birthday4", 150, "birthday_4", "birthday4", "4th birthday skin pack", "fourth birthday skin pack"),
                packMapping("birthday5", 100, "birthday_5", "birthday5", "5th birthday skin pack", "fifth birthday skin pack"),
                packMapping("adventuretime", 1000, "adventure_time", "adventure time"),
                packMapping("battleandbeasts", 800, "battle_beasts_1", "battlebeasts1", "battle beasts 1", "battle and the beasts"),
                packMapping("battleandthebeasts2", 900, "battle_beasts_2", "battlebeasts2", "battle beasts 2", "battle and the beasts 2"),
                packMapping("biomesettlers", 1000, "biome_settlers_1", "biomesettlers1", "biome settlers 1"),
                packMapping("biomesettlers2", 1100, "biome_settlers_2", "biomesettlers2", "biome settlers 2"),
                packMapping("campfiretales", 1200, "campfire_tales", "campfire tales"),
                packMapping("festivemashup", 2000, "festive_mashup"),
                packMapping("festive", 2100, "festive_pack", "festivepack"),
                packMapping("minecon2015", 2800, "minecon_2015", "minecon 2015"),
                packMapping("minecon2016", 2900, "minecon_2016", "minecon 2016"),
                packMapping("minecon2017", 2300, "minecon_2017", "minecon 2017"),
                packMapping("storymode", 4500, "mc_storymode", "story mode", "minecraft story mode"),
                packMapping("starwarsclassic", 4100, "star_wars_classic", "star wars classic"),
                packMapping("the_simpsons", 5200, "simpsons", "the simpsons"),
                packMapping("villians", 5300, "villains", "villians")
        );
    }

    static void registerPackSortIndexOverride(Map<String, Integer> sorts,
                                                      String alias,
                                                      int sortIndex) {
        if (sorts == null || alias == null || alias.isBlank()) return;

        String compactAlias = compactDuplicatePackAlias(alias);
        if (compactAlias != null) {
            sorts.putIfAbsent(compactAlias, sortIndex);
        }

        String normalizedName = normalizeDuplicatePackNameKey(alias);
        if (normalizedName != null) {
            sorts.putIfAbsent(normalizedName, sortIndex);
        }
    }

    static String normalizeDuplicatePackNameKey(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.replace('&', ' ');
        normalized = normalized.replaceAll("[^a-z0-9]+", "");
        return normalized.isBlank() ? null : normalized;
    }

    static String stripNamespace(String packId) {
        if (packId == null || packId.isBlank()) return packId;
        int colon = packId.indexOf(':');
        return colon >= 0 ? packId.substring(colon + 1) : packId;
    }

    static String namespaceOf(String packId) {
        if (packId == null || packId.isBlank()) return null;
        int colon = packId.indexOf(':');
        if (colon <= 0) return null;
        String namespace = packId.substring(0, colon).trim();
        return namespace.isBlank() ? null : namespace;
    }

}
