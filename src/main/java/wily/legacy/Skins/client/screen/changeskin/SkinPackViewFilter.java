package wily.legacy.Skins.client.screen.changeskin;

import wily.legacy.Skins.client.compat.ExternalSkinProviders;
import wily.legacy.Skins.skin.SkinPackSourceKind;

public enum SkinPackViewFilter {
    DEFAULT("Curated", false, null, true),
    BOX_MODELS("Legacy4J", true, SkinPackSourceKind.BOX_MODEL, false),
    LEGACY_SKINS("Legacy Skins", true, SkinPackSourceKind.LEGACY_SKINS, false),
    BEDROCK_SKINS("Bedrock Skins", true, SkinPackSourceKind.BEDROCK_SKINS, false),
    ALL_PACKS("All Packs", true, null, true);

    private final String label;
    private final boolean useAllPacks;
    private final SkinPackSourceKind sourceKind;
    private final boolean includeSpecialPacks;

    SkinPackViewFilter(String label, boolean useAllPacks, SkinPackSourceKind sourceKind, boolean includeSpecialPacks) {
        this.label = label;
        this.useAllPacks = useAllPacks;
        this.sourceKind = sourceKind;
        this.includeSpecialPacks = includeSpecialPacks;
    }

    public SkinPackViewFilter next() {
        SkinPackViewFilter[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public boolean usesAllPacks() {
        return useAllPacks;
    }

    public boolean usesNativeSourceOrder() {
        return sourceKind != null;
    }

    public boolean isAvailable() {
        return switch (this) {
            case LEGACY_SKINS -> ExternalSkinProviders.isSourceAvailable(SkinPackSourceKind.LEGACY_SKINS);
            case BEDROCK_SKINS -> ExternalSkinProviders.isSourceAvailable(SkinPackSourceKind.BEDROCK_SKINS);
            case ALL_PACKS -> hasAdditionalPackProvider();
            default -> true;
        };
    }

    public static boolean hasAdditionalPackProvider() {
        return ExternalSkinProviders.hasAdditionalPackProvider();
    }

    public String label() {
        return label;
    }

    public boolean includesSource(SkinPackSourceKind sourceKind) {
        if (sourceKind == null) return this == DEFAULT || this == ALL_PACKS;
        if (sourceKind == SkinPackSourceKind.SPECIAL) return includeSpecialPacks;
        return this.sourceKind == null || this.sourceKind == sourceKind;
    }
}
