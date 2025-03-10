package wily.legacy.client;

import wily.factoryapi.base.config.FactoryMixinToggle;
import wily.legacy.config.LegacyMixinToggles;

public class LegacyMixinOptions {
    public static final FactoryMixinToggle.Storage CLIENT_MIXIN_STORAGE = new FactoryMixinToggle.Storage("legacy/client_mixin.json");

    public static final FactoryMixinToggle legacyInventoryScreen = createAndRegisterMixin("legacy.mixin.base.client.inventory", "legacyInventoryScreen");
    public static final FactoryMixinToggle legacyClassicCraftingScreen = createAndRegisterMixin("legacy.mixin.base.client.crafting", "legacyClassicCraftingScreen");
    public static final FactoryMixinToggle legacyClassicStonecutterScreen = createAndRegisterMixin("legacy.mixin.base.client.stonecutter", "legacyClassicStonecutterScreen");
    public static final FactoryMixinToggle legacyClassicLoomScreen = createAndRegisterMixin("legacy.mixin.base.client.loom", "legacyClassicLoomScreen");
    public static final FactoryMixinToggle legacyClassicMerchantScreen = createAndRegisterMixin("legacy.mixin.base.client.merchant", "legacyClassicMerchantScreen");
    public static final FactoryMixinToggle legacyContainerLikeScreen = createAndRegisterMixin("legacy.mixin.base.client.container", "legacyContainerLikeScreen");
    //? if >=1.20.2 {
    public static final FactoryMixinToggle legacyCrafterScreen = createAndRegisterMixin("legacy.mixin.base.client.crafter", "legacyCrafterScreen");
    //?}
    public static final FactoryMixinToggle legacyFurnaceScreen = createAndRegisterMixin("legacy.mixin.base.client.furnace", "legacyFurnaceScreen");
    public static final FactoryMixinToggle legacyAnvilScreen = createAndRegisterMixin("legacy.mixin.base.client.anvil", "legacyAnvilScreen");
    public static final FactoryMixinToggle legacySmithingScreen = createAndRegisterMixin("legacy.mixin.base.client.smithing", "legacySmithingScreen");
    public static final FactoryMixinToggle legacyGrindstoneScreen = createAndRegisterMixin("legacy.mixin.base.client.grindstone", "legacyGrindstoneScreen");
    public static final FactoryMixinToggle legacyCartographyScreen = createAndRegisterMixin("legacy.mixin.base.client.cartography", "legacyCartographyScreen");
    public static final FactoryMixinToggle legacyEnchantmentScreen = createAndRegisterMixin("legacy.mixin.base.client.enchantment", "legacyEnchantmentScreen");
    public static final FactoryMixinToggle legacyBeaconScreen = createAndRegisterMixin("legacy.mixin.base.client.beacon", "legacyBeaconScreen");
    public static final FactoryMixinToggle legacyBrewingStandScreen = createAndRegisterMixin("legacy.mixin.base.client.brewing", "legacyBrewingStandScreen");
    public static final FactoryMixinToggle legacyBookScreen = createAndRegisterMixin("legacy.mixin.base.client.book", "legacyBookScreen");
    public static final FactoryMixinToggle legacySignScreen = createAndRegisterMixin("legacy.mixin.base.client.sign", "legacySignScreen");
    public static final FactoryMixinToggle legacyCreateWorldScreen = createAndRegisterMixin("legacy.mixin.base.client.create_world", "legacyCreateWorldScreen");
    public static final FactoryMixinToggle legacyTitleScreen = createAndRegisterMixin("legacy.mixin.base.client.title", "legacyTitleScreen");
    public static final FactoryMixinToggle legacyPauseScreen = createAndRegisterMixin("legacy.mixin.base.client.pause", "legacyPauseScreen");
    public static final FactoryMixinToggle legacyGui = createAndRegisterMixin("legacy.mixin.base.client.gui", "legacyGui");
    public static final FactoryMixinToggle legacyChat = createAndRegisterMixin("legacy.mixin.base.client.chat", "legacyChat");
    public static final FactoryMixinToggle legacyBossHealth = createAndRegisterMixin("legacy.mixin.base.client.bosshealth", "legacyBossHealth");
    public static final FactoryMixinToggle legacyWitches = createAndRegisterMixin("legacy.mixin.base.client.witch", "legacyWitches");
    public static final FactoryMixinToggle legacyDrowned = createAndRegisterMixin("legacy.mixin.base.client.drowned", "legacyDrowned");



    public static FactoryMixinToggle createAndRegisterMixin(String key, String translationKey){
        return CLIENT_MIXIN_STORAGE.register(LegacyMixinToggles.createMixinOption(key, translationKey, true));
    }
}
