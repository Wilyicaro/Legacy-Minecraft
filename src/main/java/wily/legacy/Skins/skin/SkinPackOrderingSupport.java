package wily.legacy.Skins.skin;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SkinPackOrderingSupport {

    private SkinPackOrderingSupport() {
    }

    static LinkedHashMap<String, SkinPack> sortPacks(LinkedHashMap<String, SkinPack> packs,
                                                     Map<String, Integer> sortIndex,
                                                     Set<String> hasSort,
                                                     Map<String, Integer> insertIndex,
                                                     Map<String, Integer> sortAnchorInsertIndex,
                                                     Map<String, Integer> sortPriority) {
        if (packs == null || packs.isEmpty()) return packs;
        boolean anySortedPack = false;
        for (String packId : packs.keySet()) {
            if (hasSort.contains(packId)) {
                anySortedPack = true;
                break;
            }
        }
        if (!anySortedPack) return packs;

        ArrayList<Map.Entry<String, SkinPack>> entries = new ArrayList<>(packs.entrySet());
        entries.sort((left, right) -> {
            String leftId = left.getKey();
            String rightId = right.getKey();
            boolean leftHasSort = hasSort.contains(leftId);
            boolean rightHasSort = hasSort.contains(rightId);
            if (leftHasSort != rightHasSort) return leftHasSort ? -1 : 1;

            int leftSort = sortIndex.getOrDefault(leftId, 0);
            int rightSort = sortIndex.getOrDefault(rightId, 0);
            if (leftSort != rightSort) return Integer.compare(leftSort, rightSort);

            int leftAnchor = sortAnchorInsertIndex.getOrDefault(leftId, insertIndex.getOrDefault(leftId, 0));
            int rightAnchor = sortAnchorInsertIndex.getOrDefault(rightId, insertIndex.getOrDefault(rightId, 0));
            if (leftAnchor != rightAnchor) return Integer.compare(leftAnchor, rightAnchor);

            int leftPriority = sortPriority.getOrDefault(leftId, 0);
            int rightPriority = sortPriority.getOrDefault(rightId, 0);
            if (leftPriority != rightPriority) return Integer.compare(leftPriority, rightPriority);

            int leftInsert = insertIndex.getOrDefault(leftId, 0);
            int rightInsert = insertIndex.getOrDefault(rightId, 0);
            return Integer.compare(leftInsert, rightInsert);
        });

        LinkedHashMap<String, SkinPack> ordered = new LinkedHashMap<>(packs.size());
        for (Map.Entry<String, SkinPack> entry : entries) {
            ordered.put(entry.getKey(), entry.getValue());
        }
        return ordered;
    }

    static LinkedHashMap<String, SkinPack> filterVisiblePacks(LinkedHashMap<String, SkinPack> allPacks,
                                                              Set<String> hiddenDuplicatePackIds) {
        if (allPacks == null || allPacks.isEmpty()) return new LinkedHashMap<>();
        if (hiddenDuplicatePackIds == null || hiddenDuplicatePackIds.isEmpty()) return new LinkedHashMap<>(allPacks);

        LinkedHashMap<String, SkinPack> visible = new LinkedHashMap<>(allPacks.size());
        for (Map.Entry<String, SkinPack> entry : allPacks.entrySet()) {
            if (entry == null || entry.getKey() == null) continue;
            if (hiddenDuplicatePackIds.contains(entry.getKey())) continue;
            visible.put(entry.getKey(), entry.getValue());
        }
        return visible;
    }

    static void registerPackSource(Map<String, SkinPackSourceKind> packSourceKinds,
                                   String packId,
                                   SkinPackSourceKind sourceKind) {
        if (packSourceKinds == null || packId == null || packId.isBlank() || sourceKind == null) return;
        packSourceKinds.put(packId, sourceKind);
    }

    static void registerPackSourceOrder(Map<String, Integer> packSourceOrder,
                                        String packId,
                                        Integer explicitSourceOrder,
                                        Map<String, Integer> packSortIndex,
                                        Map<String, Integer> packInsertIndex) {
        if (packSourceOrder == null || packId == null || packId.isBlank()) return;
        if (explicitSourceOrder != null) {
            packSourceOrder.put(packId, explicitSourceOrder);
            return;
        }
        Integer nativeSortIndex = packSortIndex != null ? packSortIndex.get(packId) : null;
        if (nativeSortIndex != null) {
            packSourceOrder.put(packId, nativeSortIndex);
            return;
        }
        if (packInsertIndex != null) {
            packSourceOrder.put(packId, packInsertIndex.getOrDefault(packId, Integer.MAX_VALUE));
        }
    }

    static void applyLegacyPackSortMetadata(List<LegacyPackCandidate> candidates,
                                            Map<String, Integer> packSortIndex,
                                            Set<String> packHasSort,
                                            Map<String, Integer> packInsertIndex,
                                            Map<String, Integer> packSortAnchorInsertIndex,
                                            Map<String, Integer> packSortPriority) {
        if (candidates == null || candidates.isEmpty()) return;
        if (packSortIndex == null || packHasSort == null || packInsertIndex == null
                || packSortAnchorInsertIndex == null || packSortPriority == null) {
            return;
        }

        final int slotScale = 1000;
        int candidateCount = candidates.size();
        int[] slots = new int[candidateCount];
        int[] sorts = new int[candidateCount];
        boolean[] anchored = new boolean[candidateCount];

        for (int i = 0; i < candidateCount; i++) {
            LegacyPackCandidate candidate = candidates.get(i);
            Integer explicitSortIndex = candidate.explicitSortIndex();
            if (explicitSortIndex != null) {
                slots[i] = candidate.insertIndex() * slotScale;
                sorts[i] = explicitSortIndex;
                anchored[i] = true;
                continue;
            }

            Integer sortOverride = SkinPackDuplicateResolver.findLegacyPackSortIndexOverride(
                    candidate.packLoc(),
                    candidate.packId(),
                    candidate.name()
            );
            if (sortOverride != null) {
                slots[i] = candidate.insertIndex() * slotScale;
                sorts[i] = sortOverride;
                anchored[i] = true;
                continue;
            }

            String preferredPackId = candidate.preferredPackId();
            if (preferredPackId == null || preferredPackId.isBlank()) continue;
            if (!packHasSort.contains(preferredPackId)) continue;

            int anchorInsertIndex = packSortAnchorInsertIndex.getOrDefault(
                    preferredPackId,
                    packInsertIndex.getOrDefault(preferredPackId, candidate.insertIndex())
            );
            Integer sort = packSortIndex.get(preferredPackId);
            if (sort == null) continue;

            slots[i] = anchorInsertIndex * slotScale - 1;
            sorts[i] = sort;
            anchored[i] = true;
        }

        int firstAnchored = -1;
        for (int i = 0; i < candidateCount; i++) {
            if (anchored[i]) {
                firstAnchored = i;
                break;
            }
        }

        if (firstAnchored == -1) {
            for (int i = 0; i < candidateCount; i++) {
                LegacyPackCandidate candidate = candidates.get(i);
                slots[i] = candidate.insertIndex() * slotScale;
                sorts[i] = 10000 + i;
            }
        } else {
            for (int i = firstAnchored - 1; i >= 0; i--) {
                int offset = firstAnchored - i;
                slots[i] = slots[firstAnchored] - offset;
                sorts[i] = Math.max(0, sorts[firstAnchored] - offset);
            }

            int previousAnchored = firstAnchored;
            for (int i = firstAnchored + 1; i < candidateCount; i++) {
                if (!anchored[i]) continue;
                fillLegacySortGap(previousAnchored, i, slots, sorts);
                previousAnchored = i;
            }

            for (int i = previousAnchored + 1; i < candidateCount; i++) {
                int offset = i - previousAnchored;
                slots[i] = slots[previousAnchored] + offset;
                sorts[i] = sorts[previousAnchored] + offset;
            }
        }

        for (int i = 0; i < candidateCount; i++) {
            LegacyPackCandidate candidate = candidates.get(i);
            int slot = slots[i];
            int anchorInsertIndex = Math.floorDiv(slot, slotScale);
            int priority = slot - anchorInsertIndex * slotScale;

            packSortIndex.put(candidate.packId(), sorts[i]);
            packHasSort.add(candidate.packId());
            packSortAnchorInsertIndex.put(candidate.packId(), anchorInsertIndex);
            packSortPriority.put(candidate.packId(), priority);
        }
    }

    static void inheritPreferredPackSort(String packId,
                                         String preferredPackId,
                                         Map<String, Integer> packSortIndex,
                                         Set<String> packHasSort,
                                         Map<String, Integer> packInsertIndex,
                                         Map<String, Integer> packSortAnchorInsertIndex,
                                         Map<String, Integer> packSortPriority,
                                         int fallbackInsertIndex) {
        if (packId == null || packId.isBlank() || preferredPackId == null || preferredPackId.isBlank()) return;
        if (packSortIndex == null || packHasSort == null || packInsertIndex == null
                || packSortAnchorInsertIndex == null || packSortPriority == null) {
            return;
        }
        if (!packHasSort.contains(preferredPackId)) return;

        Integer sort = packSortIndex.get(preferredPackId);
        if (sort == null) return;

        int anchorInsertIndex = packSortAnchorInsertIndex.getOrDefault(
                preferredPackId,
                packInsertIndex.getOrDefault(preferredPackId, fallbackInsertIndex)
        );
        packSortIndex.put(packId, sort);
        packHasSort.add(packId);
        packSortAnchorInsertIndex.put(packId, anchorInsertIndex - 1);
        packSortPriority.put(packId, 999);
    }

    static void applyManagedPackFamilyOrder(String preferredPackId,
                                            Integer explicitSortIndex,
                                            Map<String, Integer> packSortIndex,
                                            Set<String> packHasSort,
                                            SkinPackOrderRules packOrderRules) {
        if (preferredPackId == null || preferredPackId.isBlank()) return;
        if (explicitSortIndex == null) return;
        if (packSortIndex == null || packHasSort == null) return;
        if (packOrderRules != null && packOrderRules.hasManagedBoxModelOrder(preferredPackId)) return;
        packSortIndex.put(preferredPackId, explicitSortIndex);
        packHasSort.add(preferredPackId);
    }

    private static void fillLegacySortGap(int leftIndex,
                                          int rightIndex,
                                          int[] slots,
                                          int[] sorts) {
        int count = rightIndex - leftIndex - 1;
        if (count <= 0) return;

        int leftSlot = slots[leftIndex];
        int rightSlot = slots[rightIndex];
        int slotGap = rightSlot - leftSlot;
        int leftSort = sorts[leftIndex];
        int rightSort = sorts[rightIndex];
        int sortGap = rightSort - leftSort;

        for (int step = 1; step <= count; step++) {
            int idx = leftIndex + step;
            if (slotGap > count) {
                slots[idx] = leftSlot + (int) ((long) slotGap * step / (count + 1));
            } else {
                slots[idx] = rightSlot - (count - step + 1);
            }

            if (sortGap > 0) {
                int interpolated = leftSort + (int) ((long) sortGap * step / (count + 1));
                sorts[idx] = Math.max(leftSort + 1, interpolated);
            } else {
                sorts[idx] = leftSort;
            }
        }
    }

    record LegacyPackCandidate(ResourceLocation packLoc,
                               String packId,
                               String name,
                               String author,
                               String type,
                               ResourceLocation icon,
                               List<SkinEntry> entries,
                               List<String> insertedIds,
                               Map<String, Integer> legacyRuntimeOrdinals,
                               String preferredPackId,
                               Integer explicitSortIndex,
                               int sourceOrder,
                               int insertIndex,
                               boolean hiddenDuplicate) {
    }
}
