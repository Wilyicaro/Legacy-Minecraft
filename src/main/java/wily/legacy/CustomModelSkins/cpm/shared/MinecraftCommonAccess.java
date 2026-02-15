package wily.legacy.CustomModelSkins.cpm.shared;

import java.util.EnumSet;

public interface MinecraftCommonAccess {
    static MinecraftCommonAccess get() {
        return MinecraftObjectHolder.commonObject;
    }

    EnumSet<PlatformFeature> getSupportedFeatures();

    default String getPlatformVersionString() {
        return "Minecraft " + getMCVersion() + " " + getMCBrand() + " " + getModVersion();
    }

    String getMCVersion();

    String getMCBrand();

    String getModVersion();
}
