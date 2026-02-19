package wily.legacy.CustomModelSkins.cpm;

import wily.factoryapi.FactoryAPI;
import wily.legacy.CustomModelSkins.cpm.shared.MinecraftObjectHolder;
import wily.legacy.CustomModelSkins.cpm.shared.PlatformFeature;
import wily.legacy.Legacy4J;

import java.util.EnumSet;

public class CustomPlayerModels extends CommonBase {
    private static final EnumSet<PlatformFeature> FEATURES = EnumSet.noneOf(PlatformFeature.class);


    public static void initCommon() {
        MinecraftObjectHolder.setCommonObject(new CustomPlayerModels());
    }

    @Override
    public EnumSet<PlatformFeature> getSupportedFeatures() {
        return FEATURES;
    }

    @Override
    public String getMCBrand() {

        try {
            return "(legacy4j/" + FactoryAPI.getLoader().name().toLowerCase() + ")";
        } catch (Throwable t) {
            return "(legacy4j)";
        }
    }

    @Override
    public String getModVersion() {
        try {
            return Legacy4J.VERSION.get();
        } catch (Throwable t) {
            return "?UNKNOWN?";
        }
    }
}
