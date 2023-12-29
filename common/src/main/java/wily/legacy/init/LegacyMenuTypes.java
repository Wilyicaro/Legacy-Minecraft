package wily.legacy.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import wily.legacy.inventory.ClassicCraftingMenu;
import wily.legacy.inventory.LegacyChestMenu;
import wily.legacy.inventory.LegacyFurnaceMenu;
import wily.legacy.inventory.LegacyInventoryMenu;

import static wily.legacy.LegacyMinecraft.MOD_ID;

public class LegacyMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPE_REGISTER = DeferredRegister.create(MOD_ID, Registries.MENU);
    public static final RegistrySupplier<MenuType<LegacyChestMenu>> STORAGE_5X1 = MENU_TYPE_REGISTER.register("storage_5x1", ()->new MenuType<>(LegacyChestMenu::fiveSlots, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyChestMenu>> STORAGE_3X3 = MENU_TYPE_REGISTER.register("storage_3x3", ()->new MenuType<>(LegacyChestMenu::threeRowsColumns, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyChestMenu>> BAG_MENU = MENU_TYPE_REGISTER.register("bag", ()->new MenuType<>(LegacyChestMenu::threeRowsBag, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyChestMenu>> CHEST_MENU = MENU_TYPE_REGISTER.register("chest", ()->new MenuType<>(LegacyChestMenu::threeRows, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyChestMenu>> LARGE_CHEST_MENU = MENU_TYPE_REGISTER.register("large_chest", ()->new MenuType<>(LegacyChestMenu::sixRows, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyFurnaceMenu>> LEGACY_FURNACE_MENU = MENU_TYPE_REGISTER.register("furnace", ()->new MenuType<>(LegacyFurnaceMenu::furnace, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyFurnaceMenu>> LEGACY_SMOKER_MENU = MENU_TYPE_REGISTER.register("smoker", ()->new MenuType<>(LegacyFurnaceMenu::smoker, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyFurnaceMenu>> LEGACY_BLAST_FURNACE_MENU = MENU_TYPE_REGISTER.register("blast_furnace", ()->new MenuType<>(LegacyFurnaceMenu::blastFurnace, FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyInventoryMenu>> LEGACY_INVENTORY_MENU = MENU_TYPE_REGISTER.register("inventory", ()->new MenuType<>(((id, inventory) -> new LegacyInventoryMenu(id,inventory.player,false)), FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<LegacyInventoryMenu>> LEGACY_INVENTORY_MENU_CRAFTING = MENU_TYPE_REGISTER.register("inventory_crafting", ()->new MenuType<>((id, inventory) -> new LegacyInventoryMenu(id,inventory.player,true), FeatureFlags.VANILLA_SET));
    public static final RegistrySupplier<MenuType<ClassicCraftingMenu>> CLASSIC_CRAFTING_MENU = MENU_TYPE_REGISTER.register("classic_crafting", ()->new MenuType<>(((id, inventory) -> new ClassicCraftingMenu(id,inventory.player)), FeatureFlags.VANILLA_SET));
    public static void init(){
        MENU_TYPE_REGISTER.register();
    }
}
