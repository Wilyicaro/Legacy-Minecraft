package wily.legacy.CustomModelSkins.cpm.shared;

public enum PlatformFeature {
    EDITOR_HELD_ITEM, EDITOR_SUPPORTED,
    ;

    public boolean isSupported() {
        return MinecraftCommonAccess.get().getSupportedFeatures().contains(this);
    }
}
