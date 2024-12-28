package wily.legacy.init;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.RegisterListing;
import wily.legacy.Legacy4J;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.LegacyMerchantMenu;

import java.util.Optional;
import java.util.Set;

public class LegacyRegistries {
    private static final RegisterListing<BlockEntityType<?>> BLOCK_ENTITIES_REGISTER = FactoryAPIPlatform.createRegister(Legacy4J.MOD_ID, BuiltInRegistries.BLOCK_ENTITY_TYPE);
    private static final RegisterListing<Block> BLOCK_ITEMS_REGISTER = FactoryAPIPlatform.createRegister(Legacy4J.MOD_ID, BuiltInRegistries.BLOCK);
    private static final RegisterListing<Block> BLOCK_REGISTER = FactoryAPIPlatform.createRegister(Legacy4J.MOD_ID, BuiltInRegistries.BLOCK);
    private static final RegisterListing<Item> ITEM_REGISTER = FactoryAPIPlatform.createRegister(Legacy4J.MOD_ID, BuiltInRegistries.ITEM);
    private static final RegisterListing<MenuType<?>> MENU_REGISTER = FactoryAPIPlatform.createRegister(Legacy4J.MOD_ID, BuiltInRegistries.MENU);
    static final RegisterListing<SoundEvent> SOUND_EVENT_REGISTER = FactoryAPIPlatform.createRegister(Legacy4J.MOD_ID, BuiltInRegistries.SOUND_EVENT);

    public static final RegisterListing.Holder<MenuType<LegacyMerchantMenu>> MERCHANT_MENU = MENU_REGISTER.add("merchant_menu", ()->new MenuType<>(LegacyMerchantMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegisterListing.Holder<MenuType<LegacyCraftingMenu>> STONECUTTER_PANEL_MENU = MENU_REGISTER.add("stonecutter_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::stoneCutterMenu, FeatureFlags.VANILLA_SET));
    public static final RegisterListing.Holder<MenuType<LegacyCraftingMenu>> LOOM_PANEL_MENU = MENU_REGISTER.add("loom_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::loomMenu, FeatureFlags.VANILLA_SET));
    public static final RegisterListing.Holder<MenuType<LegacyCraftingMenu>> PLAYER_CRAFTING_PANEL_MENU = MENU_REGISTER.add("player_crafting_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::playerCraftingMenu, FeatureFlags.VANILLA_SET));
    public static final RegisterListing.Holder<MenuType<LegacyCraftingMenu>> CRAFTING_PANEL_MENU = MENU_REGISTER.add("crafting_panel_menu", ()->new MenuType<>(LegacyCraftingMenu::craftingMenu, FeatureFlags.VANILLA_SET));

    public static final RegisterListing.Holder<Item> WATER = ITEM_REGISTER.add("water",()-> new BlockItem(Blocks.WATER,new Item.Properties()/*? if >=1.21.2 {*/.setId(ResourceKey.create(Registries.ITEM,LegacyRegistries.WATER.getId())).overrideDescription(Blocks.WATER.getDescriptionId())/*?}*/));
    public static final RegisterListing.Holder<Item> LAVA = ITEM_REGISTER.add("lava",()-> new BlockItem(Blocks.LAVA,new Item.Properties()/*? if >=1.21.2 {*/.setId(ResourceKey.create(Registries.ITEM,LegacyRegistries.LAVA.getId())).overrideDescription(Blocks.LAVA.getDescriptionId())/*?}*/));

    public static final RegisterListing.Holder<Block> SHRUB = BLOCK_ITEMS_REGISTER.add("shrub",()-> new TallGrassBlock(BlockBehaviour.Properties.of()./*? if >=1.21.2 {*/setId(ResourceKey.create(Registries.BLOCK,LegacyRegistries.SHRUB.getId()))./*?}*/mapColor(MapColor.PLANT).replaceable().noCollission().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).ignitedByLava().pushReaction(PushReaction.DESTROY)));

    public static final RegisterListing.Holder<BlockEntityType<WaterCauldronBlockEntity>> WATER_CAULDRON_BLOCK_ENTITY = BLOCK_ENTITIES_REGISTER.add("water_cauldron",()-> /*? if <1.21.2 {*//*BlockEntityType.Builder.of*//*?} else {*/new BlockEntityType/*?}*/(WaterCauldronBlockEntity::new, /*? if >=1.21.2 {*/Set.of(Blocks.WATER_CAULDRON))/*?} else {*//*Blocks.WATER_CAULDRON).build(null)*//*?}*/);

    public static final RegisterListing.Holder<SoundEvent> SCROLL = SOUND_EVENT_REGISTER.add("random.scroll",()->SoundEvent.createVariableRangeEvent(Legacy4J.createModLocation("random.scroll")));
    public static final RegisterListing.Holder<SoundEvent> CRAFT_FAIL = SOUND_EVENT_REGISTER.add("random.craft_fail",()->SoundEvent.createVariableRangeEvent(Legacy4J.createModLocation("random.craft_fail")));
    public static final RegisterListing.Holder<SoundEvent> BACK = SOUND_EVENT_REGISTER.add("random.back",()->SoundEvent.createVariableRangeEvent(Legacy4J.createModLocation("random.back")));
    public static final RegisterListing.Holder<SoundEvent> FOCUS = SOUND_EVENT_REGISTER.add("random.focus",()->SoundEvent.createVariableRangeEvent(Legacy4J.createModLocation("random.focus")));
    public static final RegisterListing.Holder<SoundEvent> ACTION = SOUND_EVENT_REGISTER.add("random.action",()->SoundEvent.createVariableRangeEvent(Legacy4J.createModLocation("random.action")));
    public static final RegisterListing.Holder<SoundEvent> SHIFT_LOCK = SOUND_EVENT_REGISTER.add("random.shift_lock",()->SoundEvent.createVariableRangeEvent(Legacy4J.createModLocation("random.shift_lock")));
    public static final RegisterListing.Holder<SoundEvent> SHIFT_UNLOCK = SOUND_EVENT_REGISTER.add("random.shift_unlock",()->SoundEvent.createVariableRangeEvent(Legacy4J.createModLocation("random.shift_unlock")));
    public static final RegisterListing.Holder<SoundEvent> SPACE = SOUND_EVENT_REGISTER.add("random.space",()->SoundEvent.createVariableRangeEvent(Legacy4J.createModLocation("random.space")));
    public static final RegisterListing.Holder<SoundEvent> BACKSPACE = SOUND_EVENT_REGISTER.add("random.backspace",()->SoundEvent.createVariableRangeEvent(Legacy4J.createModLocation("random.backspace")));


    public static boolean isInvalidCauldron(BlockState blockState, Level level, BlockPos blockPos){
        Optional<WaterCauldronBlockEntity> opt;
        return blockState.is(Blocks.WATER_CAULDRON) && (opt = level.getBlockEntity(blockPos, LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get())).isPresent() && (!opt.get().hasWater() || opt.get().waterColor != null);
    }
    public static void register(){
        BLOCK_REGISTER.register();
        BLOCK_ENTITIES_REGISTER.register();
        BLOCK_ITEMS_REGISTER.register();
        BLOCK_ITEMS_REGISTER.forEach(b-> ITEM_REGISTER.add(b.getId().getPath(),()-> new BlockItem(b.get(), new Item.Properties()/*? if >=1.21.2 {*/.setId(ResourceKey.create(Registries.ITEM,b.getId())).useBlockDescriptionPrefix()/*?}*/)));
        ITEM_REGISTER.register();
        MENU_REGISTER.register();
        SOUND_EVENT_REGISTER.register();
    }
}
