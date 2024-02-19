package wily.legacy.init;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.LegacyCraftingMenu;

public class LegacyMenuTypes {

    private static final DeferredRegister<MenuType<?>> MENU_REGISTER = DeferredRegister.create(LegacyMinecraft.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<LegacyCraftingMenu>> CRAFTING_PANEL_MENU = MENU_REGISTER.register("crafting_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::craftingMenu, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyCraftingMenu>> PLAYER_CRAFTING_PANEL_MENU = MENU_REGISTER.register("player_crafting_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::playerCraftingMenu, FeatureFlags.VANILLA_SET));

    public static void register(){
        MENU_REGISTER.register();
    }
}
