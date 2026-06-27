package wily.legacy.config;

import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.legacy.Legacy4JClient;

import static wily.legacy.util.LegacyComponents.optionName;

public class LegacyCommonOptions {
    public static final FactoryConfig.StorageHandler COMMON_STORAGE = new FactoryConfig.StorageHandler(true).withFile("legacy/common.json");
    public static final FactoryConfig<Boolean> legacyCombat = COMMON_STORAGE.register(FactoryConfig.createBoolean("legacyCombat", new FactoryConfigDisplay.Instance<>(optionName("legacyCombat")), true, b-> {}, COMMON_STORAGE));
    public static final FactoryConfig<Boolean> legacySwordBlocking = COMMON_STORAGE.register(FactoryConfig.createBoolean("legacySwordBlocking", new FactoryConfigDisplay.Instance<>(optionName("legacySwordBlocking")), false, b-> {}, COMMON_STORAGE));
    public static final FactoryConfig<Boolean> legacyBlockProtection = COMMON_STORAGE.register(FactoryConfig.createBoolean("legacyBlockProtection", new FactoryConfigDisplay.Instance<>(optionName("legacyBlockProtection")), true, b-> {}, COMMON_STORAGE));
    public static final FactoryConfig<Boolean> legacyAudio = COMMON_STORAGE.register(FactoryConfig.createBoolean("legacyAudio", new FactoryConfigDisplay.Instance<>(optionName("legacyAudio")), true, b-> {}, COMMON_STORAGE));
    public static final FactoryConfig<Boolean> legacyLootTables = COMMON_STORAGE.register(FactoryConfig.createBoolean("legacyLootTables", new FactoryConfigDisplay.Instance<>(optionName("legacyLootTables")), true, b-> {}, COMMON_STORAGE));
    public static final FactoryConfig<Boolean> legacyMobInteractions = COMMON_STORAGE.register(FactoryConfig.createBoolean("legacyMobInteractions", new FactoryConfigDisplay.Instance<>(optionName("legacyMobInteractions")), true, b-> {}, COMMON_STORAGE));
    public static final FactoryConfig<Boolean> legacyWorldInteractions = COMMON_STORAGE.register(FactoryConfig.createBoolean("legacyWorldInteractions", new FactoryConfigDisplay.Instance<>(optionName("legacyWorldInteractions")), true, b-> {}, COMMON_STORAGE));
    public static final FactoryConfig<Boolean> squaredViewDistance = COMMON_STORAGE.register(FactoryConfig.createBoolean("squaredViewDistance", new FactoryConfigDisplay.Instance<>(optionName("squaredViewDistance")), false, b-> {
        if (FactoryAPI.isClient()) Legacy4JClient.updateChunks();
    }, COMMON_STORAGE));

}
