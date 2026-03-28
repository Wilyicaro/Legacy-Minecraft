package wily.legacy.Skins.skin;

import wily.legacy.Skins.util.DebugLog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class SkinPackDefaultOrganizer {
    private static final int UNKNOWN_BOX_MODEL_ORDER_OFFSET = 1000;
    private static final int UNKNOWN_FAMILY_ORDER_OFFSET = 1000;
    private static final int DEFAULT_PACK_SOURCE_ORDER = -100000;

    private SkinPackDefaultOrganizer() {
    }

    static OrganizationResult organize(LinkedHashMap<String, SkinPack> loadedPacks,
                                       Map<String, SkinPackSourceKind> packSourceKinds,
                                       Map<String, Integer> packSourceOrder,
                                       Map<String, Integer> packInsertOrder,
                                       SkinPackOrderRules orderRules,
                                       SkinPackFamilyRules familyRules,
                                       boolean hideDuplicatePacks) {
        LinkedHashMap<String, Integer> organizedSourceOrder = new LinkedHashMap<>();
        LinkedHashMap<String, SkinPack> organizedPacks = new LinkedHashMap<>();
        LinkedHashSet<String> hiddenDuplicatePackIds = new LinkedHashSet<>();
        if (loadedPacks == null || loadedPacks.isEmpty()) {
            return new OrganizationResult(organizedPacks, hiddenDuplicatePackIds, organizedSourceOrder);
        }

        ArrayList<PackCandidate> candidates = buildCandidates(loadedPacks, packSourceKinds, packSourceOrder, packInsertOrder, orderRules, familyRules);
        boolean hasExternalSource = hasExternalSources(candidates);
        organizedSourceOrder.putAll(buildSourceOrders(candidates, packSourceOrder, orderRules, hasExternalSource));
        for (PackCandidate candidate : candidates) {
            candidate.nativeSourceOrder = organizedSourceOrder.getOrDefault(candidate.packId, candidate.loaderIndex);
        }
        if (!hasExternalSource) {
            ArrayList<PackCandidate> nativeOrdered = new ArrayList<>(candidates);
            nativeOrdered.sort(Comparator.comparingInt((PackCandidate candidate) -> candidate.nativeSourceOrder)
                    .thenComparingInt(candidate -> candidate.loaderIndex)
                    .thenComparing(candidate -> candidate.packId, String::compareToIgnoreCase));
            for (PackCandidate candidate : nativeOrdered) {
                organizedPacks.put(candidate.packId, candidate.pack);
            }
            return new OrganizationResult(organizedPacks, hiddenDuplicatePackIds, organizedSourceOrder);
        }

        ArrayList<FamilyGroup> families = resolveFamilies(candidates, orderRules, familyRules);
        Map<String, Long> familySortKeys = computeFamilySortKeys(families);
        families.sort(familyComparator(familySortKeys));
        for (FamilyGroup family : families) {
            family.members.sort(memberComparator(family.preferredSource, family.winner.packId));
            for (PackCandidate member : family.members) {
                organizedPacks.put(member.packId, member.pack);
            }
            if (hideDuplicatePacks && family.members.size() > 1) {
                for (PackCandidate member : family.members) {
                    if (!Objects.equals(member.packId, family.winner.packId)) {
                        hiddenDuplicatePackIds.add(member.packId);
                    }
                }
            }
            DebugLog.debug(
                    "Skin pack family {} order={} winner={} members={} hidden={}",
                    family.familyId,
                    familySortKeys.getOrDefault(family.familyId, Long.MAX_VALUE),
                    family.winner.packId,
                    family.memberIds(),
                    hideDuplicatePacks ? hiddenDuplicatePackIds.stream().filter(family.memberIds()::contains).toList() : List.of()
            );
        }

        return new OrganizationResult(organizedPacks, hiddenDuplicatePackIds, organizedSourceOrder);
    }

    private static ArrayList<PackCandidate> buildCandidates(LinkedHashMap<String, SkinPack> loadedPacks,
                                                            Map<String, SkinPackSourceKind> packSourceKinds,
                                                            Map<String, Integer> packSourceOrder,
                                                            Map<String, Integer> packInsertOrder,
                                                            SkinPackOrderRules orderRules,
                                                            SkinPackFamilyRules familyRules) {
        ArrayList<PackCandidate> candidates = new ArrayList<>(loadedPacks.size());
        int loaderIndex = 0;
        for (Map.Entry<String, SkinPack> entry : loadedPacks.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) continue;

            String packId = entry.getKey();
            SkinPack pack = entry.getValue();
            SkinPackSourceKind sourceKind = resolveSourceKind(packId, packSourceKinds);
            String displayName = SkinPackLoader.nameString(pack.name(), packId);
            String managedBoxName = sourceKind == SkinPackSourceKind.BOX_MODEL && orderRules != null
                    ? orderRules.managedBoxModelName(packId)
                    : null;
            LinkedHashSet<String> aliasScopeKeys = new LinkedHashSet<>();
            aliasScopeKeys.addAll(SkinPackDuplicateResolver.buildDuplicatePackAliasKeys(packId));
            aliasScopeKeys.addAll(SkinPackDuplicateResolver.buildDuplicatePackAliasKeys(SkinPackDuplicateResolver.stripNamespace(packId)));
            aliasScopeKeys.addAll(SkinPackDuplicateResolver.buildDuplicatePackAliasKeys(displayName));
            if (managedBoxName != null && !managedBoxName.isBlank()) {
                aliasScopeKeys.addAll(SkinPackDuplicateResolver.buildDuplicatePackAliasKeys(managedBoxName));
            }

            String nameKey = SkinPackDuplicateResolver.normalizeDuplicatePackNameKey(displayName);
            if (nameKey != null) aliasScopeKeys.add("name:" + nameKey);

            List<String> fingerprints = SkinPackDuplicateResolver.buildDuplicatePackFingerprints(displayName, pack.skins());
            List<String> tokens = SkinPackDuplicateResolver.buildDuplicatePackTokens(pack.skins());
            Integer managedDefaultOrder = orderRules == null ? null : orderRules.findManagedOrder(sourceKind, packId);
            Integer existingSourceOrder = packSourceOrder == null ? null : packSourceOrder.get(packId);
            Integer existingInsertOrder = packInsertOrder == null ? null : packInsertOrder.get(packId);
            String explicitFamilyId = familyRules == null ? null : familyRules.findFamilyId(sourceKind, packId, displayName);

            candidates.add(new PackCandidate(
                    packId,
                    pack,
                    sourceKind,
                    displayName,
                    managedBoxName,
                    loaderIndex,
                    existingInsertOrder == null ? loaderIndex : existingInsertOrder,
                    existingSourceOrder == null ? loaderIndex : existingSourceOrder,
                    managedDefaultOrder,
                    explicitFamilyId,
                    nameKey,
                    List.copyOf(aliasScopeKeys),
                    fingerprints,
                    tokens,
                    SkinIds.PACK_DEFAULT.equals(packId)
            ));
            loaderIndex++;
        }
        return candidates;
    }

    private static boolean hasExternalSources(List<PackCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return false;
        for (PackCandidate candidate : candidates) {
            if (candidate.sourceKind == SkinPackSourceKind.LEGACY_SKINS || candidate.sourceKind == SkinPackSourceKind.BEDROCK_SKINS) {
                return true;
            }
        }
        return false;
    }

    private static LinkedHashMap<String, Integer> buildSourceOrders(List<PackCandidate> candidates,
                                                                    Map<String, Integer> originalSourceOrder,
                                                                    SkinPackOrderRules orderRules,
                                                                    boolean hasExternalSource) {
        LinkedHashMap<String, Integer> sourceOrders = new LinkedHashMap<>();
        int manifestBoxOrder = 0;
        int unknownBoxOrder = Math.max(orderRules == null ? 0 : orderRules.maxManagedOrder(), 0) + UNKNOWN_BOX_MODEL_ORDER_OFFSET;

        for (PackCandidate candidate : candidates) {
            if (candidate == null) continue;

            int sourceOrder;
            if (candidate.isDefaultPack) {
                sourceOrder = DEFAULT_PACK_SOURCE_ORDER;
            } else if (candidate.sourceKind == SkinPackSourceKind.BOX_MODEL) {
                manifestBoxOrder++;
                if (!hasExternalSource) {
                    sourceOrder = manifestBoxOrder;
                } else {
                    Integer managedOrder = orderRules == null ? null : orderRules.findManagedOrder(SkinPackSourceKind.BOX_MODEL, candidate.packId);
                    sourceOrder = managedOrder != null ? managedOrder : unknownBoxOrder++;
                }
            } else {
                Integer nativeOrder = originalSourceOrder == null ? null : originalSourceOrder.get(candidate.packId);
                sourceOrder = nativeOrder == null ? candidate.loaderIndex : nativeOrder;
            }
            sourceOrders.put(candidate.packId, sourceOrder);
        }
        return sourceOrders;
    }

    private static ArrayList<FamilyGroup> resolveFamilies(List<PackCandidate> candidates,
                                                          SkinPackOrderRules orderRules,
                                                          SkinPackFamilyRules familyRules) {
        HashMap<String, PackCandidate> byPackId = new HashMap<>();
        HashMap<String, String> parent = new HashMap<>();
        for (PackCandidate candidate : candidates) {
            byPackId.put(candidate.packId, candidate);
            parent.put(candidate.packId, candidate.packId);
        }

        unionExplicitFamilies(candidates, parent);
        unionPreferredBoxModelFamilies(candidates, parent);
        unionExactFingerprintFamilies(candidates, parent);
        unionScopedFuzzyFamilies(candidates, parent);

        LinkedHashMap<String, ArrayList<PackCandidate>> candidatesByRoot = new LinkedHashMap<>();
        for (PackCandidate candidate : candidates) {
            String root = findRoot(parent, candidate.packId);
            candidatesByRoot.computeIfAbsent(root, key -> new ArrayList<>()).add(candidate);
        }

        ArrayList<FamilyGroup> families = new ArrayList<>(candidatesByRoot.size());
        for (ArrayList<PackCandidate> members : candidatesByRoot.values()) {
            if (members == null || members.isEmpty()) continue;

            String explicitFamilyId = selectExplicitFamilyId(members, familyRules, orderRules);
            String familyId = explicitFamilyId != null ? explicitFamilyId : deriveAutomaticFamilyId(members);
            SkinPackSourceKind preferredSource = explicitFamilyId != null && familyRules != null
                    ? familyRules.preferredSource(explicitFamilyId)
                    : SkinPackSourceKind.BOX_MODEL;
            Integer defaultFamilyOrder = explicitFamilyId != null && familyRules != null
                    ? familyRules.findManagedFamilyOrder(explicitFamilyId, orderRules)
                    : null;
            if (defaultFamilyOrder == null) {
                defaultFamilyOrder = findSmallestManagedOrder(members);
            }

            PackCandidate winner = selectWinner(members, preferredSource);
            if (winner != null) winner.duplicateWinner = true;
            families.add(new FamilyGroup(familyId, preferredSource, defaultFamilyOrder, winner, members));
        }
        return families;
    }

    private static void unionExplicitFamilies(List<PackCandidate> candidates, Map<String, String> parent) {
        HashMap<String, String> firstPackByFamily = new HashMap<>();
        for (PackCandidate candidate : candidates) {
            if (candidate.explicitFamilyId == null || candidate.explicitFamilyId.isBlank()) continue;
            String firstPackId = firstPackByFamily.putIfAbsent(candidate.explicitFamilyId, candidate.packId);
            if (firstPackId != null) union(parent, firstPackId, candidate.packId);
        }
    }

    private static void unionPreferredBoxModelFamilies(List<PackCandidate> candidates, Map<String, String> parent) {
        HashMap<String, String> boxAliases = new HashMap<>();
        HashMap<String, String> boxFingerprints = new HashMap<>();
        LinkedHashMap<String, List<String>> boxTokens = new LinkedHashMap<>();
        HashMap<String, PackCandidate> boxCandidates = new HashMap<>();

        for (PackCandidate candidate : candidates) {
            if (candidate.sourceKind != SkinPackSourceKind.BOX_MODEL) continue;
            boxCandidates.put(candidate.packId, candidate);
            registerCandidateAlias(boxAliases, candidate.packId, candidate.packId);
            registerCandidateAlias(boxAliases, SkinPackDuplicateResolver.stripNamespace(candidate.packId), candidate.packId);
            registerCandidateAlias(boxAliases, candidate.displayName, candidate.packId);
            registerCandidateAlias(boxAliases, candidate.managedBoxName, candidate.packId);
            for (String fingerprint : candidate.fingerprints) {
                boxFingerprints.putIfAbsent(fingerprint, candidate.packId);
            }
            if (candidate.tokens != null && !candidate.tokens.isEmpty()) {
                boxTokens.putIfAbsent(candidate.packId, candidate.tokens);
            }
        }

        for (PackCandidate candidate : candidates) {
            if (candidate.sourceKind == SkinPackSourceKind.BOX_MODEL) continue;
            String preferredPackId = SkinPackDuplicateResolver.findNewFormatDuplicatePackId(
                    null,
                    candidate.packId,
                    candidate.displayName,
                    candidate.pack.skins(),
                    boxAliases,
                    boxFingerprints,
                    boxTokens
            );
            if (preferredPackId == null || !boxCandidates.containsKey(preferredPackId)) continue;
            union(parent, preferredPackId, candidate.packId);
        }
    }

    private static void unionExactFingerprintFamilies(List<PackCandidate> candidates, Map<String, String> parent) {
        HashMap<String, String> firstPackByFingerprint = new HashMap<>();
        for (PackCandidate candidate : candidates) {
            for (String fingerprint : candidate.fingerprints) {
                if (fingerprint == null || fingerprint.isBlank()) continue;
                String firstPackId = firstPackByFingerprint.putIfAbsent(fingerprint, candidate.packId);
                if (firstPackId != null) {
                    union(parent, firstPackId, candidate.packId);
                }
            }
        }
    }

    private static void unionScopedFuzzyFamilies(List<PackCandidate> candidates, Map<String, String> parent) {
        HashMap<String, ArrayList<PackCandidate>> buckets = new HashMap<>();
        for (PackCandidate candidate : candidates) {
            for (String aliasKey : candidate.aliasScopeKeys) {
                if (aliasKey == null || aliasKey.isBlank()) continue;
                buckets.computeIfAbsent(aliasKey, key -> new ArrayList<>()).add(candidate);
            }
        }

        for (ArrayList<PackCandidate> bucket : buckets.values()) {
            int size = bucket.size();
            if (size < 2) continue;
            for (int left = 0; left < size - 1; left++) {
                PackCandidate a = bucket.get(left);
                for (int right = left + 1; right < size; right++) {
                    PackCandidate b = bucket.get(right);
                    if (Objects.equals(findRoot(parent, a.packId), findRoot(parent, b.packId))) continue;
                    if (shouldFuzzyMatch(a, b)) {
                        union(parent, a.packId, b.packId);
                    }
                }
            }
        }
    }

    private static boolean shouldFuzzyMatch(PackCandidate left, PackCandidate right) {
        if (left == null || right == null) return false;
        if (left.tokens == null || right.tokens == null) return false;
        if (left.tokens.size() < 4 || right.tokens.size() < 4) return false;

        int common = SkinPackDuplicateResolver.longestCommonTokenSubsequence(left.tokens, right.tokens);
        if (common <= 0) return false;

        int longest = Math.max(left.tokens.size(), right.tokens.size());
        int shortest = Math.min(left.tokens.size(), right.tokens.size());
        double score = (double) common / (double) longest;
        double coverage = (double) common / (double) shortest;
        return coverage >= 0.82D && score >= 0.68D;
    }

    private static String selectExplicitFamilyId(List<PackCandidate> members,
                                                 SkinPackFamilyRules familyRules,
                                                 SkinPackOrderRules orderRules) {
        if (members == null || members.isEmpty()) return null;
        return members.stream()
                .map(candidate -> candidate.explicitFamilyId)
                .filter(Objects::nonNull)
                .distinct()
                .min((left, right) -> {
                    Integer leftOrder = familyRules == null ? null : familyRules.findManagedFamilyOrder(left, orderRules);
                    Integer rightOrder = familyRules == null ? null : familyRules.findManagedFamilyOrder(right, orderRules);
                    if (leftOrder != null || rightOrder != null) {
                        if (leftOrder == null) return 1;
                        if (rightOrder == null) return -1;
                        if (!leftOrder.equals(rightOrder)) return Integer.compare(leftOrder, rightOrder);
                    }
                    return left.compareToIgnoreCase(right);
                })
                .orElse(null);
    }

    private static String deriveAutomaticFamilyId(List<PackCandidate> members) {
        if (members == null || members.isEmpty()) return "pack:unknown";
        PackCandidate preferred = members.stream()
                .min(Comparator.comparingInt((PackCandidate candidate) -> globalSourcePriority(candidate.sourceKind))
                        .thenComparingInt(candidate -> candidate.managedDefaultOrder == null ? Integer.MAX_VALUE : candidate.managedDefaultOrder)
                        .thenComparingInt(candidate -> candidate.nativeSourceOrder)
                        .thenComparing(candidate -> candidate.packId, String::compareToIgnoreCase))
                .orElse(members.get(0));
        return "pack:" + preferred.packId;
    }

    private static Integer findSmallestManagedOrder(List<PackCandidate> members) {
        Integer best = null;
        if (members == null || members.isEmpty()) return null;
        for (PackCandidate candidate : members) {
            if (candidate.managedDefaultOrder == null) continue;
            if (best == null || candidate.managedDefaultOrder < best) {
                best = candidate.managedDefaultOrder;
            }
        }
        return best;
    }

    private static Map<String, Long> computeFamilySortKeys(List<FamilyGroup> families) {
        final long scale = 1000L;
        HashMap<String, Long> sortKeys = new HashMap<>();
        if (families == null || families.isEmpty()) return sortKeys;

        long maxKnownSortKey = 0L;
        ArrayList<FamilyGroup> knownBoxFamilies = new ArrayList<>();
        ArrayList<FamilyGroup> unknownBoxFamilies = new ArrayList<>();
        ArrayList<FamilyGroup> deferredFamilies = new ArrayList<>();

        for (FamilyGroup family : families) {
            if (family == null) continue;
            if (family.defaultFamilyOrder != null) {
                long sortKey = family.defaultFamilyOrder.longValue() * scale;
                sortKeys.put(family.familyId, sortKey);
                if (sortKey > maxKnownSortKey) maxKnownSortKey = sortKey;
                if (family.smallestBoxNativeOrder() != null) knownBoxFamilies.add(family);
                continue;
            }

            if (family.smallestBoxNativeOrder() != null) unknownBoxFamilies.add(family);
            else deferredFamilies.add(family);
        }

        knownBoxFamilies.sort(Comparator.comparingInt(family -> family.smallestBoxNativeOrder() == null ? Integer.MAX_VALUE : family.smallestBoxNativeOrder()));
        unknownBoxFamilies.sort(Comparator.comparingInt(family -> family.smallestBoxNativeOrder() == null ? Integer.MAX_VALUE : family.smallestBoxNativeOrder()));
        interpolateUnknownBoxFamilies(sortKeys, knownBoxFamilies, unknownBoxFamilies, scale, maxKnownSortKey);

        long deferredBase = Math.max(maxKnownSortKey, sortKeys.values().stream().mapToLong(Long::longValue).max().orElse(0L)) + scale * 10L;
        deferredFamilies.sort(Comparator.comparingInt((FamilyGroup family) -> globalSourcePriority(family.winner.sourceKind))
                .thenComparingInt(family -> family.winner.nativeSourceOrder)
                .thenComparing(family -> family.winner.displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(family -> family.familyId, String.CASE_INSENSITIVE_ORDER));
        for (int index = 0; index < deferredFamilies.size(); index++) {
            FamilyGroup family = deferredFamilies.get(index);
            long offset = (long) index * scale + (long) globalSourcePriority(family.winner.sourceKind) * 100L + Math.min(99, Math.max(0, family.winner.nativeSourceOrder));
            sortKeys.putIfAbsent(family.familyId, deferredBase + offset);
        }
        return sortKeys;
    }

    private static void interpolateUnknownBoxFamilies(Map<String, Long> sortKeys,
                                                      List<FamilyGroup> knownBoxFamilies,
                                                      List<FamilyGroup> unknownBoxFamilies,
                                                      long scale,
                                                      long maxKnownSortKey) {
        if (sortKeys == null || unknownBoxFamilies == null || unknownBoxFamilies.isEmpty()) return;
        if (knownBoxFamilies == null || knownBoxFamilies.isEmpty()) {
            long base = maxKnownSortKey + scale;
            for (int index = 0; index < unknownBoxFamilies.size(); index++) {
                FamilyGroup family = unknownBoxFamilies.get(index);
                sortKeys.putIfAbsent(family.familyId, base + index * scale);
            }
            return;
        }

        ArrayList<IntervalGroup> intervals = new ArrayList<>();
        for (FamilyGroup family : unknownBoxFamilies) {
            Integer nativeOrder = family.smallestBoxNativeOrder();
            if (nativeOrder == null) continue;

            int rightIndex = -1;
            for (int index = 0; index < knownBoxFamilies.size(); index++) {
                Integer knownOrder = knownBoxFamilies.get(index).smallestBoxNativeOrder();
                if (knownOrder != null && nativeOrder < knownOrder) {
                    rightIndex = index;
                    break;
                }
            }
            int leftIndex = rightIndex < 0 ? knownBoxFamilies.size() - 1 : rightIndex - 1;
            IntervalKey intervalKey = new IntervalKey(leftIndex, rightIndex);
            IntervalGroup interval = intervals.stream()
                    .filter(candidate -> candidate.key.equals(intervalKey))
                    .findFirst()
                    .orElseGet(() -> {
                        IntervalGroup created = new IntervalGroup(intervalKey, new ArrayList<>());
                        intervals.add(created);
                        return created;
                    });
            interval.families.add(family);
        }

        for (IntervalGroup interval : intervals) {
            interval.families.sort(Comparator.comparingInt(family -> family.smallestBoxNativeOrder() == null ? Integer.MAX_VALUE : family.smallestBoxNativeOrder()));
            int count = interval.families.size();
            if (count <= 0) continue;

            Long leftKey = interval.key.leftIndex >= 0
                    ? sortKeys.get(knownBoxFamilies.get(interval.key.leftIndex).familyId)
                    : null;
            Long rightKey = interval.key.rightIndex >= 0 && interval.key.rightIndex < knownBoxFamilies.size()
                    ? sortKeys.get(knownBoxFamilies.get(interval.key.rightIndex).familyId)
                    : null;

            if (leftKey != null && rightKey != null && rightKey > leftKey) {
                long gap = rightKey - leftKey;
                for (int step = 1; step <= count; step++) {
                    long offset = Math.max(1L, gap * step / (count + 1L));
                    sortKeys.putIfAbsent(interval.families.get(step - 1).familyId, leftKey + offset);
                }
                continue;
            }

            if (leftKey != null) {
                for (int step = 1; step <= count; step++) {
                    sortKeys.putIfAbsent(interval.families.get(step - 1).familyId, leftKey + step);
                }
                continue;
            }

            if (rightKey != null) {
                for (int step = count; step >= 1; step--) {
                    sortKeys.putIfAbsent(interval.families.get(count - step).familyId, rightKey - step);
                }
                continue;
            }

            long base = maxKnownSortKey + scale;
            for (int index = 0; index < count; index++) {
                sortKeys.putIfAbsent(interval.families.get(index).familyId, base + index);
            }
        }
    }

    private static PackCandidate selectWinner(List<PackCandidate> members, SkinPackSourceKind preferredSource) {
        if (members == null || members.isEmpty()) return null;
        return members.stream()
                .min(Comparator.comparingInt((PackCandidate candidate) -> sourcePreferencePriority(candidate.sourceKind, preferredSource))
                        .thenComparingInt(candidate -> candidate.managedDefaultOrder == null ? Integer.MAX_VALUE : candidate.managedDefaultOrder)
                        .thenComparingInt(candidate -> candidate.nativeSourceOrder)
                        .thenComparing(candidate -> candidate.displayName == null ? "" : candidate.displayName.toLowerCase(Locale.ROOT))
                        .thenComparing(candidate -> candidate.packId, String::compareToIgnoreCase))
                .orElse(members.get(0));
    }

    private static Comparator<FamilyGroup> familyComparator(Map<String, Long> familySortKeys) {
        return (left, right) -> {
            if (left == right) return 0;
            if (left == null) return 1;
            if (right == null) return -1;

            if (left.containsDefaultPack() != right.containsDefaultPack()) {
                return left.containsDefaultPack() ? -1 : 1;
            }

            long leftSortKey = familySortKeys == null ? Long.MAX_VALUE : familySortKeys.getOrDefault(left.familyId, Long.MAX_VALUE);
            long rightSortKey = familySortKeys == null ? Long.MAX_VALUE : familySortKeys.getOrDefault(right.familyId, Long.MAX_VALUE);
            if (leftSortKey != rightSortKey) {
                return Long.compare(leftSortKey, rightSortKey);
            }

            int winnerNameCompare = String.CASE_INSENSITIVE_ORDER.compare(left.winner.displayName, right.winner.displayName);
            if (winnerNameCompare != 0) return winnerNameCompare;
            return String.CASE_INSENSITIVE_ORDER.compare(left.winner.packId, right.winner.packId);
        };
    }

    private static Comparator<PackCandidate> memberComparator(SkinPackSourceKind preferredSource, String winnerPackId) {
        return (left, right) -> {
            if (left == right) return 0;
            if (left == null) return 1;
            if (right == null) return -1;

            boolean leftWinner = Objects.equals(left.packId, winnerPackId);
            boolean rightWinner = Objects.equals(right.packId, winnerPackId);
            if (leftWinner != rightWinner) return leftWinner ? -1 : 1;

            int sourceCompare = Integer.compare(sourcePreferencePriority(left.sourceKind, preferredSource), sourcePreferencePriority(right.sourceKind, preferredSource));
            if (sourceCompare != 0) return sourceCompare;

            int nativeCompare = Integer.compare(left.nativeSourceOrder, right.nativeSourceOrder);
            if (nativeCompare != 0) return nativeCompare;

            int managedCompare = Integer.compare(left.managedDefaultOrder == null ? Integer.MAX_VALUE : left.managedDefaultOrder,
                    right.managedDefaultOrder == null ? Integer.MAX_VALUE : right.managedDefaultOrder);
            if (managedCompare != 0) return managedCompare;

            int nameCompare = String.CASE_INSENSITIVE_ORDER.compare(left.displayName, right.displayName);
            if (nameCompare != 0) return nameCompare;
            return String.CASE_INSENSITIVE_ORDER.compare(left.packId, right.packId);
        };
    }

    private static int sourcePreferencePriority(SkinPackSourceKind sourceKind, SkinPackSourceKind preferredSource) {
        if (sourceKind == null) return Integer.MAX_VALUE;
        if (preferredSource == sourceKind) return 0;
        if (sourceKind == SkinPackSourceKind.SPECIAL) return Integer.MAX_VALUE - 1;

        return switch (preferredSource == null ? SkinPackSourceKind.BOX_MODEL : preferredSource) {
            case LEGACY_SKINS -> switch (sourceKind) {
                case BOX_MODEL -> 1;
                case BEDROCK_SKINS -> 2;
                case LEGACY_SKINS -> 0;
                case SPECIAL -> Integer.MAX_VALUE - 1;
            };
            case BEDROCK_SKINS -> switch (sourceKind) {
                case BOX_MODEL -> 1;
                case LEGACY_SKINS -> 2;
                case BEDROCK_SKINS -> 0;
                case SPECIAL -> Integer.MAX_VALUE - 1;
            };
            case BOX_MODEL, SPECIAL -> globalSourcePriority(sourceKind);
        };
    }

    private static int globalSourcePriority(SkinPackSourceKind sourceKind) {
        if (sourceKind == null) return Integer.MAX_VALUE;
        return switch (sourceKind) {
            case BOX_MODEL -> 0;
            case LEGACY_SKINS -> 1;
            case BEDROCK_SKINS -> 2;
            case SPECIAL -> 3;
        };
    }

    private static SkinPackSourceKind resolveSourceKind(String packId, Map<String, SkinPackSourceKind> packSourceKinds) {
        if (SkinIds.PACK_DEFAULT.equals(packId)) return SkinPackSourceKind.BOX_MODEL;
        if (packSourceKinds == null || packId == null || packId.isBlank()) return SkinPackSourceKind.SPECIAL;
        return packSourceKinds.getOrDefault(packId, SkinPackSourceKind.SPECIAL);
    }

    private static void registerCandidateAlias(Map<String, String> aliases, String alias, String packId) {
        if (aliases == null || packId == null || packId.isBlank() || alias == null || alias.isBlank()) return;
        for (String aliasKey : SkinPackDuplicateResolver.buildDuplicatePackAliasKeys(alias)) {
            aliases.putIfAbsent(aliasKey, packId);
        }
        String nameKey = SkinPackDuplicateResolver.normalizeDuplicatePackNameKey(alias);
        if (nameKey != null) aliases.putIfAbsent("name:" + nameKey, packId);
    }

    private static String findRoot(Map<String, String> parent, String packId) {
        String current = parent.get(packId);
        if (current == null || current.equals(packId)) return packId;
        String root = findRoot(parent, current);
        parent.put(packId, root);
        return root;
    }

    private static void union(Map<String, String> parent, String leftPackId, String rightPackId) {
        if (parent == null || leftPackId == null || rightPackId == null) return;
        String leftRoot = findRoot(parent, leftPackId);
        String rightRoot = findRoot(parent, rightPackId);
        if (Objects.equals(leftRoot, rightRoot)) return;
        if (leftRoot.compareToIgnoreCase(rightRoot) <= 0) {
            parent.put(rightRoot, leftRoot);
        } else {
            parent.put(leftRoot, rightRoot);
        }
    }

    record OrganizationResult(LinkedHashMap<String, SkinPack> orderedBasePacks,
                              Set<String> hiddenDuplicatePackIds,
                              Map<String, Integer> sourceOrder) {
    }

    private static final class PackCandidate {
        private final String packId;
        private final SkinPack pack;
        private final SkinPackSourceKind sourceKind;
        private final String displayName;
        private final String managedBoxName;
        private final int loaderIndex;
        private final int baseInsertOrder;
        private int nativeSourceOrder;
        private final Integer managedDefaultOrder;
        private final String explicitFamilyId;
        private final String nameKey;
        private final List<String> aliasScopeKeys;
        private final List<String> fingerprints;
        private final List<String> tokens;
        private final boolean isDefaultPack;
        private boolean duplicateWinner;

        private PackCandidate(String packId,
                              SkinPack pack,
                              SkinPackSourceKind sourceKind,
                              String displayName,
                              String managedBoxName,
                              int loaderIndex,
                              int baseInsertOrder,
                              int nativeSourceOrder,
                              Integer managedDefaultOrder,
                              String explicitFamilyId,
                              String nameKey,
                              List<String> aliasScopeKeys,
                              List<String> fingerprints,
                              List<String> tokens,
                              boolean isDefaultPack) {
            this.packId = packId;
            this.pack = pack;
            this.sourceKind = sourceKind;
            this.displayName = displayName == null ? packId : displayName;
            this.managedBoxName = managedBoxName;
            this.loaderIndex = loaderIndex;
            this.baseInsertOrder = baseInsertOrder;
            this.nativeSourceOrder = nativeSourceOrder;
            this.managedDefaultOrder = managedDefaultOrder;
            this.explicitFamilyId = explicitFamilyId;
            this.nameKey = nameKey;
            this.aliasScopeKeys = aliasScopeKeys;
            this.fingerprints = fingerprints;
            this.tokens = tokens;
            this.isDefaultPack = isDefaultPack;
        }
    }

    private record FamilyGroup(String familyId,
                               SkinPackSourceKind preferredSource,
                               Integer defaultFamilyOrder,
                               PackCandidate winner,
                               ArrayList<PackCandidate> members) {

        private boolean containsDefaultPack() {
            if (members == null || members.isEmpty()) return false;
            for (PackCandidate member : members) {
                if (member != null && member.isDefaultPack) return true;
            }
            return false;
        }

        private List<String> memberIds() {
            if (members == null || members.isEmpty()) return List.of();
            ArrayList<String> ids = new ArrayList<>(members.size());
            for (PackCandidate member : members) {
                if (member != null && member.packId != null) ids.add(member.packId);
            }
            return List.copyOf(ids);
        }

        private Integer smallestBoxNativeOrder() {
            Integer best = null;
            if (members == null || members.isEmpty()) return null;
            for (PackCandidate member : members) {
                if (member == null || member.sourceKind != SkinPackSourceKind.BOX_MODEL) continue;
                if (best == null || member.baseInsertOrder < best) {
                    best = member.baseInsertOrder;
                }
            }
            return best;
        }
    }

    private record IntervalKey(int leftIndex, int rightIndex) {
    }

    private static final class IntervalGroup {
        private final IntervalKey key;
        private final ArrayList<FamilyGroup> families;

        private IntervalGroup(IntervalKey key, ArrayList<FamilyGroup> families) {
            this.key = key;
            this.families = families;
        }
    }
}
