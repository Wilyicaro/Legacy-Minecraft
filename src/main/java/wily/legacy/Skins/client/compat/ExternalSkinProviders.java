package wily.legacy.Skins.client.compat;

import net.minecraft.client.gui.GuiGraphics;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.skin.SkinPackSourceKind;
import wily.legacy.Skins.util.DebugLog;

import java.util.ArrayList;
import java.util.List;

public final class ExternalSkinProviders {
    private static final LegacySkinsProviderCompat LEGACY_PROVIDER = new LegacySkinsProviderCompat();
    private static final BedrockSkinsProviderCompat BEDROCK_PROVIDER = new BedrockSkinsProviderCompat();
    private static final List<ExternalSkinProvider> PROVIDERS = List.of(LEGACY_PROVIDER, BEDROCK_PROVIDER);
    private static final List<ExternalSkinSelectionProvider> SELECTION_PROVIDERS = List.of(LEGACY_PROVIDER, BEDROCK_PROVIDER);
    private static final List<ExternalSkinPreviewProvider> PREVIEW_PROVIDERS = List.of(LEGACY_PROVIDER, BEDROCK_PROVIDER);
    private static final List<ExternalSkinPackProvider> PACK_PROVIDERS = List.of(BEDROCK_PROVIDER);
    private static volatile boolean capabilitiesLogged;
    private static volatile String activeSelectionProviderId;
    private static volatile SkinPackSourceKind preferredSelectionSourceKind;

    private ExternalSkinProviders() {
    }

    public static void logCapabilitiesOnce() {
        if (capabilitiesLogged) return;
        synchronized (ExternalSkinProviders.class) {
            if (capabilitiesLogged) return;
            capabilitiesLogged = true;
            for (ExternalSkinProvider provider : PROVIDERS) {
                ExternalSkinProviderCapabilities capabilities = safeCapabilities(provider);
                if (capabilities.isDegraded()) {
                    DebugLog.warn("Skin compat {} degraded: {}", provider.providerId(), capabilities.summary());
                } else {
                    DebugLog.debug("Skin compat {} ready: {}", provider.providerId(), capabilities.summary());
                }
            }
        }
    }

    public static boolean hasAdditionalPackProvider() {
        for (ExternalSkinSelectionProvider provider : SELECTION_PROVIDERS) {
            if (safeCapabilities(provider).coreAvailable()) return true;
        }
        return false;
    }

    public static boolean isSourceAvailable(SkinPackSourceKind sourceKind) {
        if (sourceKind == null) return false;
        for (ExternalSkinProvider provider : PROVIDERS) {
            if (provider.sourceKind() == sourceKind) return safeCapabilities(provider).coreAvailable();
        }
        return false;
    }

    public static boolean isExternalSkinId(String skinId) {
        return findSelectionProvider(skinId) != null;
    }

    public static void setPreferredSelectionSource(SkinPackSourceKind sourceKind) {
        preferredSelectionSourceKind = sourceKind;
    }

    public static boolean applySelectedSkin(String skinId) {
        ExternalSkinSelectionProvider provider = findSelectionProvider(skinId);
        if (provider == null) return false;
        if (!provider.applySelectedSkin(skinId)) return false;
        activeSelectionProviderId = provider.providerId();
        clearSelectedSkinExcept(provider);
        return true;
    }

    public static void clearAllSelectedSkins() {
        activeSelectionProviderId = null;
        for (ExternalSkinSelectionProvider provider : SELECTION_PROVIDERS) {
            try {
                provider.clearSelectedSkin();
            } catch (Throwable throwable) {
                DebugLog.warn("Skin compat {} failed to clear selection: {}", provider.providerId(), throwable.toString());
            }
        }
    }

    public static void importCurrentSelectionsIfAbsent() {
        for (ExternalSkinSelectionProvider provider : SELECTION_PROVIDERS) {
            if (!safeCapabilities(provider).coreAvailable()) continue;
            try {
                provider.importCurrentSelectionIfAbsent();
            } catch (Throwable throwable) {
                DebugLog.warn("Skin compat {} failed to import current selection: {}", provider.providerId(), throwable.toString());
            }
        }
    }

    public static String getCurrentSelectedSkinId() {
        ResolvedSelection resolved = resolveActiveSelection(preferredSelectionSourceKind);
        return resolved == null ? null : resolved.skinId();
    }

    public static boolean canRenderPreview(String skinId) {
        ExternalSkinPreviewProvider provider = findPreviewProvider(skinId);
        return provider != null && provider.canRenderPreview(skinId);
    }

    public static boolean renderPreview(PlayerSkinWidget owner,
                                        GuiGraphics guiGraphics,
                                        String skinId,
                                        float rotationX,
                                        float rotationY,
                                        boolean crouching,
                                        boolean punchLoop,
                                        float partialTick,
                                        int left,
                                        int top,
                                        int right,
                                        int bottom) {
        ExternalSkinPreviewProvider provider = findPreviewProvider(skinId);
        if (provider == null) return false;
        return provider.renderPreview(owner, guiGraphics, skinId, rotationX, rotationY, crouching, punchLoop, partialTick, left, top, right, bottom);
    }

    public static void resetPreviewCaches() {
        for (ExternalSkinPreviewProvider provider : PREVIEW_PROVIDERS) {
            if (!safeCapabilities(provider).coreAvailable()) continue;
            try {
                provider.resetPreviewCache();
            } catch (Throwable ignored) {
            }
        }
    }

    public static List<ExternalSkinPackDescriptor> loadPackDescriptors(SkinPackSourceKind sourceKind) {
        if (sourceKind == null) return List.of();
        for (ExternalSkinPackProvider provider : PACK_PROVIDERS) {
            if (provider.sourceKind() != sourceKind) continue;
            if (!safeCapabilities(provider).coreAvailable()) return List.of();
            try {
                List<ExternalSkinPackDescriptor> descriptors = provider.loadPackDescriptors();
                return descriptors == null ? List.of() : descriptors;
            } catch (Throwable ignored) {
                return List.of();
            }
        }
        return List.of();
    }

    public static List<ExternalSkinProvider> degradedProviders() {
        ArrayList<ExternalSkinProvider> degraded = new ArrayList<>();
        for (ExternalSkinProvider provider : PROVIDERS) {
            if (safeCapabilities(provider).isDegraded()) degraded.add(provider);
        }
        return List.copyOf(degraded);
    }

    private static ExternalSkinProviderCapabilities safeCapabilities(ExternalSkinProvider provider) {
        try {
            return provider.capabilities();
        } catch (Throwable throwable) {
            DebugLog.warn("Skin compat {} self-test failed: {}", provider == null ? "unknown" : provider.providerId(), throwable.toString());
            return new ExternalSkinProviderCapabilities(false, false, false, false, false, false, false, false);
        }
    }

    private static ExternalSkinSelectionProvider findSelectionProvider(String skinId) {
        if (skinId == null || skinId.isBlank()) return null;
        for (ExternalSkinSelectionProvider provider : SELECTION_PROVIDERS) {
            if (provider.ownsSkinId(skinId)) return provider;
        }
        return null;
    }

    private static ExternalSkinPreviewProvider findPreviewProvider(String skinId) {
        if (skinId == null || skinId.isBlank()) return null;
        for (ExternalSkinPreviewProvider provider : PREVIEW_PROVIDERS) {
            if (provider.ownsSkinId(skinId)) return provider;
        }
        return null;
    }

    private static void clearSelectedSkinExcept(ExternalSkinSelectionProvider selectedProvider) {
        for (ExternalSkinSelectionProvider provider : SELECTION_PROVIDERS) {
            if (provider == selectedProvider) continue;
            try {
                provider.clearSelectedSkin();
            } catch (Throwable throwable) {
                DebugLog.warn("Skin compat {} failed to clear selection after {} applied: {}", provider.providerId(), selectedProvider.providerId(), throwable.toString());
            }
        }
    }

    private static ResolvedSelection resolveActiveSelection(SkinPackSourceKind preferredSourceKind) {
        ResolvedSelection ownedSelection = resolveSelectionForProviderId(activeSelectionProviderId);
        if (ownedSelection != null) return ownedSelection;

        ArrayList<ResolvedSelection> candidates = new ArrayList<>();
        for (ExternalSkinSelectionProvider provider : SELECTION_PROVIDERS) {
            ResolvedSelection candidate = readSelection(provider);
            if (candidate != null) candidates.add(candidate);
        }
        if (candidates.isEmpty()) {
            activeSelectionProviderId = null;
            return null;
        }

        if (preferredSourceKind == SkinPackSourceKind.LEGACY_SKINS || preferredSourceKind == SkinPackSourceKind.BEDROCK_SKINS) {
            for (ResolvedSelection candidate : candidates) {
                if (candidate.provider().sourceKind() == preferredSourceKind) {
                    activeSelectionProviderId = candidate.provider().providerId();
                    return candidate;
                }
            }
        }

        ResolvedSelection winner = candidates.getFirst();
        activeSelectionProviderId = winner.provider().providerId();
        return winner;
    }

    private static ResolvedSelection resolveSelectionForProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) return null;
        for (ExternalSkinSelectionProvider provider : SELECTION_PROVIDERS) {
            if (!providerId.equals(provider.providerId())) continue;
            ResolvedSelection selection = readSelection(provider);
            if (selection == null) {
                activeSelectionProviderId = null;
                return null;
            }
            activeSelectionProviderId = selection.provider().providerId();
            return selection;
        }
        activeSelectionProviderId = null;
        return null;
    }

    private static ResolvedSelection readSelection(ExternalSkinSelectionProvider provider) {
        if (provider == null || !safeCapabilities(provider).coreAvailable()) return null;
        try {
            String skinId = provider.getCurrentSelectedSkinId();
            if (skinId == null || skinId.isBlank()) return null;
            if (!provider.canResolveSelection(skinId)) return null;
            return new ResolvedSelection(provider, skinId);
        } catch (Throwable throwable) {
            DebugLog.warn("Skin compat {} failed to read current selection: {}", provider.providerId(), throwable.toString());
            return null;
        }
    }

    private record ResolvedSelection(ExternalSkinSelectionProvider provider, String skinId) {
    }
}