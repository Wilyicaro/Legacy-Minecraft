package wily.legacy.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import wily.legacy.Legacy4J;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.LegacyMerchantMenu;

public class LegacyMenuTypes {

    private static final DeferredRegister<MenuType<?>> MENU_REGISTER = DeferredRegister.create(Legacy4J.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<LegacyCraftingMenu>> CRAFTING_PANEL_MENU = MENU_REGISTER.register("crafting_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::craftingMenu, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyCraftingMenu>> PLAYER_CRAFTING_PANEL_MENU = MENU_REGISTER.register("player_crafting_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::playerCraftingMenu, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyCraftingMenu>> LOOM_PANEL_MENU = MENU_REGISTER.register("loom_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::loomMenu, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyCraftingMenu>> STONECUTTER_PANEL_MENU = MENU_REGISTER.register("stonecutter_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::stoneCutterMenu, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyMerchantMenu>> MERCHANT_MENU = MENU_REGISTER.register("merchant_menu", ()->new MenuType<>(LegacyMerchantMenu::new, FeatureFlags.VANILLA_SET));
    public static void register(){
        if (Legacy4J.serverProperties != null && !Legacy4J.serverProperties.legacyRegistries) return;
        MENU_REGISTER.register();
    }
}
