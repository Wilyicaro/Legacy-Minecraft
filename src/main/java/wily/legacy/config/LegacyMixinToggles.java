package wily.legacy.config;

import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.factoryapi.base.config.FactoryMixinToggle;
import wily.legacy.util.LegacyComponents;

import static wily.legacy.util.LegacyComponents.optionName;

public class LegacyMixinToggles {
    public static final FactoryMixinToggle.Storage COMMON_STORAGE = new FactoryMixinToggle.Storage("legacy/common_mixin.json");
    public static final FactoryMixinToggle legacyCauldrons = createAndRegisterMixinOption("legacy.mixin.base.cauldron", "legacyCauldrons");
    public static final FactoryMixinToggle legacyPistons = createAndRegisterMixinOption("legacy.mixin.base.piston", "legacyPistons");


    private static FactoryMixinToggle createAndRegisterMixinOption(String key, String translationKey) {
        return COMMON_STORAGE.register(createMixinOption(key, translationKey, true));
    }

    public static FactoryMixinToggle createMixinOption(String key, String translationKey, boolean defaultValue) {
        return new FactoryMixinToggle(key, defaultValue, () -> new FactoryConfigDisplay.Instance<>(optionName(translationKey), b -> LegacyComponents.NEEDS_RESTART, (c, v) -> c));
    }
}
