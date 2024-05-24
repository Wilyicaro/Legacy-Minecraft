package wily.legacy.init;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import wily.legacy.Legacy4J;
import wily.legacy.block.entity.WaterCauldronBlockEntity;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.minecraft.world.level.block.Blocks.CAULDRON;

public class LegacyRegistries {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES_REGISTER = DeferredRegister.create(Legacy4J.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<Block> BLOCK_ITEMS_REGISTER = DeferredRegister.create(Legacy4J.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Block> BLOCK_REGISTER = DeferredRegister.create(Legacy4J.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEM_REGISTER = DeferredRegister.create(Legacy4J.MOD_ID, Registries.ITEM);
    public static final RegistrySupplier<Item> WATER = ITEM_REGISTER.register("water",()-> new BlockItem(Blocks.WATER,new Item.Properties()));
    public static final RegistrySupplier<Item> LAVA = ITEM_REGISTER.register("lava",()-> new BlockItem(Blocks.LAVA,new Item.Properties()));

    public static final RegistrySupplier<Block> SHRUB = BLOCK_ITEMS_REGISTER.register("shrub",()-> new TallGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().noCollission().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).ignitedByLava().pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<BlockEntityType<WaterCauldronBlockEntity>> WATER_CAULDRON_BLOCK_ENTITY = BLOCK_ENTITIES_REGISTER.register("water_cauldron",()-> BlockEntityType.Builder.of(WaterCauldronBlockEntity::new, Blocks.WATER_CAULDRON).build(null));
    public static boolean isInvalidCauldron(BlockState blockState, Level level, BlockPos blockPos){
        Optional<WaterCauldronBlockEntity> opt;
        return blockState.is(Blocks.WATER_CAULDRON) && (opt = level.getBlockEntity(blockPos, LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get())).isPresent() && (opt.get().potion != Potions.WATER || opt.get().waterColor != null);
    }
    public static void register(){
        if (Legacy4J.serverProperties != null && !Legacy4J.serverProperties.legacyRegistries) return;
        LifecycleEvent.SETUP.register(() -> {
            Map<Item, CauldronInteraction> emptyCauldron = CauldronInteraction.EMPTY.map();
            Map<Item, CauldronInteraction> waterCauldron = CauldronInteraction.WATER.map();
            CauldronInteraction emptyCauldronPotion = (blockState, level, blockPos, player, interactionHand, itemStack) ->{
                PotionContents p;
                if ((p = itemStack.get(DataComponents.POTION_CONTENTS)) == null || p.potion().isEmpty()) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                if (!level.isClientSide) {
                    Item item = itemStack.getItem();
                    player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(item));
                    level.playSound(null, blockPos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
                }
                level.setBlockAndUpdate(blockPos, Blocks.WATER_CAULDRON.defaultBlockState());
                level.getBlockEntity(blockPos,WATER_CAULDRON_BLOCK_ENTITY.get()).ifPresent(be->{
                    be.potion = p.potion().get();
                    be.setChanged();
                });
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            };
            emptyCauldron.put(Items.POTION,emptyCauldronPotion);
            emptyCauldron.put(Items.SPLASH_POTION,emptyCauldronPotion);
            emptyCauldron.put(Items.LINGERING_POTION,emptyCauldronPotion);
            waterCauldron.put(Items.GLASS_BOTTLE, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
                Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,WATER_CAULDRON_BLOCK_ENTITY.get());
                if (opt.map(be->be.waterColor).orElse(null) != null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                if (!level.isClientSide) {
                    Item item = itemStack.getItem();
                    player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.POTION.arch$holder(),1, DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(opt.map(be->be.potion).orElse(Potions.WATER))).build())));
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(item));
                    LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
                    level.playSound(null, blockPos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                    level.gameEvent(null, GameEvent.FLUID_PICKUP, blockPos);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            });
            CauldronInteraction waterCauldronPotion = (blockState, level, blockPos, player, interactionHand, itemStack) -> {
                PotionContents p;
                Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,WATER_CAULDRON_BLOCK_ENTITY.get());
                if ((p = itemStack.get(DataComponents.POTION_CONTENTS)) == null || p.potion().isEmpty() || (blockState.getValue(LayeredCauldronBlock.LEVEL) == 3 && (opt.isEmpty() || opt.get().potion == p.potion().get()))) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                if (!level.isClientSide) {
                    player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.GLASS_BOTTLE)));
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                    level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
                }
                if (opt.isPresent() && opt.get().potion != p.potion().get()){
                    level.setBlockAndUpdate(blockPos, CAULDRON.defaultBlockState());
                    if (opt.get().potion != p.potion().get()) level.playSound(null, blockPos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
                }else{
                    if (opt.isEmpty() || opt.get().waterColor == null) level.setBlockAndUpdate(blockPos, blockState.cycle(LayeredCauldronBlock.LEVEL));
                    else {
                        opt.get().waterColor = null;
                        level.setBlockAndUpdate(blockPos, blockState.setValue(LayeredCauldronBlock.LEVEL,1));
                        opt.get().setChanged();
                    }
                    level.playSound(null, blockPos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            };
            waterCauldron.put(Items.POTION, waterCauldronPotion);
            waterCauldron.put(Items.SPLASH_POTION, waterCauldronPotion);
            waterCauldron.put(Items.LINGERING_POTION, waterCauldronPotion);
            waterCauldron.put(Items.ARROW, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
                if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || be.potion == Potions.WATER) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                if (!level.isClientSide) {
                    int l = blockState.getValue(LayeredCauldronBlock.LEVEL);
                    int arrowCount = Math.min(itemStack.getCount(), l < 3 ? l * 16 : 64);
                    itemStack.shrink(arrowCount);
                    ItemStack tippedArrow = new ItemStack(Items.TIPPED_ARROW,arrowCount);
                    tippedArrow.set(DataComponents.POTION_CONTENTS,new PotionContents(be.potion));
                    player.getInventory().placeItemBackInInventory(tippedArrow);
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                    int i = (int) Math.min(3,Math.ceil(arrowCount / 16d));
                    BlockState blockState2 = l - i  == 0 ? Blocks.CAULDRON.defaultBlockState() : blockState.setValue(LayeredCauldronBlock.LEVEL, i);
                    level.setBlockAndUpdate(blockPos, blockState2);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(blockState2));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            });

            for (DyeColor color : DyeColor.values()) {
                waterCauldron.put(DyeItem.byColor(color),(blockState, level, blockPos, player, interactionHand, itemStack) -> {
                    Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,WATER_CAULDRON_BLOCK_ENTITY.get());
                    if (!(itemStack.getItem() instanceof DyeItem) || opt.isEmpty() || opt.get().potion != Potions.WATER) {
                        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                    }
                    int dyeColor = new Color(color.getTextureDiffuseColors()[0],color.getTextureDiffuseColors()[1],color.getTextureDiffuseColors()[2]).getRGB();
                    if (opt.get().waterColor == null) opt.get().waterColor = dyeColor;
                    else opt.get().waterColor = Legacy4J.mixColors(List.of(opt.get().waterColor,dyeColor).iterator());
                    opt.get().setChanged();
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                });
            }
            BuiltInRegistries.ITEM.stream().filter(i-> i.arch$holder().is(ItemTags.DYEABLE)).forEach(i-> waterCauldron.put(i,(blockState, level, blockPos, player, interactionHand, itemStack) -> {
                Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,WATER_CAULDRON_BLOCK_ENTITY.get());
                if (!(itemStack.is(ItemTags.DYEABLE)) || (opt.isPresent() && opt.get().potion != Potions.WATER) || (itemStack.get(DataComponents.DYED_COLOR) == null && (opt.isEmpty() || opt.get().waterColor == null))) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }

                if (!level.isClientSide) {
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                    if (opt.isEmpty() || opt.get().waterColor == null) itemStack.set(DataComponents.DYED_COLOR,null);
                    else Legacy4J.dyeItem(itemStack,opt.get().waterColor);
                    LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }));
        });
        BLOCK_REGISTER.register();
        BLOCK_ENTITIES_REGISTER.register();
        BLOCK_ITEMS_REGISTER.register();
        BLOCK_ITEMS_REGISTER.forEach(b-> ITEM_REGISTER.register(b.getId(),()-> new BlockItem(b.get(), new Item.Properties())));
        ITEM_REGISTER.register();
    }
}
