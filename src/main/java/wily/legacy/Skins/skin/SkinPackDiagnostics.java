package wily.legacy.Skins.skin;

import wily.legacy.Skins.util.DebugLog;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class SkinPackDiagnostics {
    private static final int MAX_LIST_ITEMS = 12;

    private SkinPackDiagnostics() {
    }

    static void logLoadDiagnostics(Map<String, SkinPack> basePacks,
                                   Map<String, String> packBySkin,
                                   Map<String, SkinPackSourceKind> packSourceKinds,
                                   Set<String> hiddenDuplicatePackIds,
                                   SkinPackOrderRules orderRules,
                                   SkinPackFamilyRules familyRules) {
        if (basePacks == null || basePacks.isEmpty()) return;

        logInternalConsistencyWarnings(basePacks, packBySkin, packSourceKinds, hiddenDuplicatePackIds);
        logMissingManagedBoxPacks(basePacks, packSourceKinds, orderRules);
        logUnmanagedExternalPacks(basePacks, packSourceKinds, orderRules, familyRules);
        logSummary(basePacks, packSourceKinds, hiddenDuplicatePackIds);
    }

   
    private static void logInternalConsistencyWarnings(Map<String, SkinPack> basePacks,
                                                       Map<String, String> packBySkin,
                                                       Map<String, SkinPackSourceKind> packSourceKinds,
                                                       Set<String> hiddenDuplicatePackIds) {
        ArrayList<String> missingHiddenDuplicateIds = new ArrayList<>();
        if (hiddenDuplicatePackIds != null) {
            for (String packId : hiddenDuplicatePackIds) {
                if (packId != null && !basePacks.containsKey(packId)) {
                    missingHiddenDuplicateIds.add(packId);
                }
            }
        }
        if (!missingHiddenDuplicateIds.isEmpty()) {
            DebugLog.warn("Skin pack organizer hid packs that were never published: {}", abbreviateList(missingHiddenDuplicateIds));
        }

        ArrayList<String> packsMissingSourceKind = new ArrayList<>();
        for (String packId : basePacks.keySet()) {
            if (packId == null || SkinIds.PACK_DEFAULT.equals(packId) || SkinIds.PACK_FAVOURITES.equals(packId)) continue;
            if (packSourceKinds == null || !packSourceKinds.containsKey(packId)) packsMissingSourceKind.add(packId);
        }
        if (!packsMissingSourceKind.isEmpty()) {
            DebugLog.warn("Skin pack loader published packs without a source kind: {}", abbreviateList(packsMissingSourceKind));
        }

        LinkedHashSet<String> orphanedSkinPackIds = new LinkedHashSet<>();
        if (packBySkin != null) {
            for (String packId : packBySkin.values()) {
                if (packId == null || basePacks.containsKey(packId)) continue;
                orphanedSkinPackIds.add(packId);
            }
        }
        if (!orphanedSkinPackIds.isEmpty()) {
            DebugLog.warn("Skin pack loader mapped skins to missing packs: {}", abbreviateList(orphanedSkinPackIds));
        }
    }

    private static void logMissingManagedBoxPacks(Map<String, SkinPack> basePacks,
                                                  Map<String, SkinPackSourceKind> packSourceKinds,
                                                  SkinPackOrderRules orderRules) {
        if (orderRules == null || !orderRules.hasEntries()) return;

        ArrayList<String> missingManagedBoxPacks = new ArrayList<>();
        for (Map.Entry<String, SkinPack> entry : basePacks.entrySet()) {
            String packId = entry.getKey();
            if (packId == null || SkinIds.PACK_DEFAULT.equals(packId) || SkinIds.PACK_FAVOURITES.equals(packId)) continue;
            if (resolveSourceKind(packId, packSourceKinds) != SkinPackSourceKind.BOX_MODEL) continue;
            if (orderRules.findManagedOrder(SkinPackSourceKind.BOX_MODEL, packId) != null) continue;
            missingManagedBoxPacks.add(packId);
        }

        if (!missingManagedBoxPacks.isEmpty()) {
            DebugLog.warn("Curated skin pack order is missing {} built-in box packs: {}", missingManagedBoxPacks.size(), abbreviateList(missingManagedBoxPacks));
        }
    }

   
    private static void logUnmanagedExternalPacks(Map<String, SkinPack> basePacks,
                                                  Map<String, SkinPackSourceKind> packSourceKinds,
                                                  SkinPackOrderRules orderRules,
                                                  SkinPackFamilyRules familyRules) {
        ArrayList<String> unmanagedExternalPacks = new ArrayList<>();
        for (Map.Entry<String, SkinPack> entry : basePacks.entrySet()) {
            String packId = entry.getKey();
            SkinPack pack = entry.getValue();
            if (packId == null || pack == null) continue;

            SkinPackSourceKind sourceKind = resolveSourceKind(packId, packSourceKinds);
            if (sourceKind != SkinPackSourceKind.LEGACY_SKINS && sourceKind != SkinPackSourceKind.BEDROCK_SKINS) continue;
            if (orderRules != null && orderRules.findManagedOrder(sourceKind, packId) != null) continue;

            String displayName = SkinPackLoader.nameString(pack.name(), packId);
            String familyId = familyRules == null ? null : familyRules.findFamilyId(sourceKind, packId, displayName);
            if (familyId != null && !familyId.isBlank()) continue;

            unmanagedExternalPacks.add(packId + " [" + sourceKind.label() + "]");
        }

        if (!unmanagedExternalPacks.isEmpty()) {
            DebugLog.debug("Skin pack organizer loaded unmanaged external packs: {}", abbreviateList(unmanagedExternalPacks));
        }
    }

   
    private static void logSummary(Map<String, SkinPack> basePacks,
                                   Map<String, SkinPackSourceKind> packSourceKinds,
                                   Set<String> hiddenDuplicatePackIds) {
        EnumMap<SkinPackSourceKind, Integer> totalBySource = new EnumMap<>(SkinPackSourceKind.class);
        EnumMap<SkinPackSourceKind, Integer> visibleBySource = new EnumMap<>(SkinPackSourceKind.class);
        EnumMap<SkinPackSourceKind, Integer> hiddenBySource = new EnumMap<>(SkinPackSourceKind.class);

        int visibleCount = 0;
        for (String packId : basePacks.keySet()) {
            if (packId == null || SkinIds.PACK_FAVOURITES.equals(packId)) continue;

            SkinPackSourceKind sourceKind = resolveSourceKind(packId, packSourceKinds);
            increment(totalBySource, sourceKind);
            if (hiddenDuplicatePackIds != null && hiddenDuplicatePackIds.contains(packId)) {
                increment(hiddenBySource, sourceKind);
            } else {
                increment(visibleBySource, sourceKind);
                visibleCount++;
            }
        }

        DebugLog.debug(
                "Skin pack load summary total={} visible={} hiddenDuplicates={} totalBySource={} visibleBySource={} hiddenBySource={}",
                basePacks.size(),
                visibleCount,
                hiddenDuplicatePackIds == null ? 0 : hiddenDuplicatePackIds.size(),
                formatCounts(totalBySource),
                formatCounts(visibleBySource),
                formatCounts(hiddenBySource)
        );
    }

    private static SkinPackSourceKind resolveSourceKind(String packId, Map<String, SkinPackSourceKind> packSourceKinds) {
        if (SkinIds.PACK_DEFAULT.equals(packId)) return SkinPackSourceKind.BOX_MODEL;
        if (SkinIds.PACK_FAVOURITES.equals(packId)) return SkinPackSourceKind.SPECIAL;
        if (packSourceKinds == null) return SkinPackSourceKind.SPECIAL;
        return packSourceKinds.getOrDefault(packId, SkinPackSourceKind.SPECIAL);
    }

    private static void increment(EnumMap<SkinPackSourceKind, Integer> counts, SkinPackSourceKind sourceKind) {
        if (counts == null || sourceKind == null) return;
        counts.put(sourceKind, counts.getOrDefault(sourceKind, 0) + 1);
    }

    private static String formatCounts(EnumMap<SkinPackSourceKind, Integer> counts) {
        if (counts == null || counts.isEmpty()) return "none";

        ArrayList<String> parts = new ArrayList<>();
        for (SkinPackSourceKind sourceKind : SkinPackSourceKind.values()) {
            Integer count = counts.get(sourceKind);
            if (count == null || count <= 0) continue;
            parts.add(sourceKind.name().toLowerCase() + "=" + count);
        }
        return parts.isEmpty() ? "none" : String.join(", ", parts);
    }

    private static String abbreviateList(Iterable<String> values) {
        if (values == null) return "none";

        ArrayList<String> items = new ArrayList<>();
        int total = 0;
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            total++;
            if (items.size() < MAX_LIST_ITEMS) items.add(value);
        }
        if (total == 0) return "none";
        if (total <= MAX_LIST_ITEMS) return String.join(", ", items);
        return String.join(", ", items) + " ... +" + (total - MAX_LIST_ITEMS) + " more";
    }
}
