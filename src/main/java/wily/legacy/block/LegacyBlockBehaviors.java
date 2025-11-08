package wily.legacy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import wily.legacy.Legacy4J;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.config.LegacyMixinToggles;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.LegacyItemUtil;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import static net.minecraft.world.level.block.Blocks.CAULDRON;

public class LegacyBlockBehaviors {
    public static void setup() {
        DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior() {
            public ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
                Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
                EntityType<?> entityType = ((SpawnEggItem) itemStack.getItem()).getType(itemStack);

                try {
                    if (entityType.spawn(blockSource.level(), itemStack, null, blockSource./*? if >1.20.1 {*/pos/*?} else {*//*getPos*//*?}*/().relative(direction), /*? if <1.21.3 {*//*MobSpawnType*//*?} else {*/EntitySpawnReason/*?}*/.DISPENSER, direction != Direction.UP, false) != null) {
                        itemStack.shrink(1);
                        blockSource.level().gameEvent(null, GameEvent.ENTITY_PLACE, blockSource./*? if >1.20.1 {*/pos/*?} else {*//*getPos*//*?}*/());
                    }
                } catch (Exception var6) {
                    LOGGER.error("Error while dispensing spawn egg from dispenser at {}", blockSource./*? if >1.20.1 {*/pos/*?} else {*//*getPos*//*?}*/(), var6);
                    return ItemStack.EMPTY;
                }

                return itemStack;
            }
        };

        for (SpawnEggItem spawnEggItem : SpawnEggItem.eggs()) {
            DispenserBlock.registerBehavior(spawnEggItem, defaultDispenseItemBehavior);
        }

        Map<Item, CauldronInteraction> emptyCauldron = CauldronInteraction.EMPTY/*? if >1.20.1 {*/.map()/*?}*/;
        Map<Item, CauldronInteraction> waterCauldron = CauldronInteraction.WATER/*? if >1.20.1 {*/.map()/*?}*/;
        Map<Item, CauldronInteraction> powderSnowCauldron = CauldronInteraction.POWDER_SNOW/*? if >1.20.1 {*/.map()/*?}*/;
        Map<Item, CauldronInteraction> lavaCauldron = CauldronInteraction.LAVA/*? if >1.20.1 {*/.map()/*?}*/;
        CauldronInteraction emptyCauldronPotion = (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            Holder<Potion> p;
            if (/*? if <1.20.5 {*//*(p = BuiltInRegistries.POTION.wrapAsHolder(PotionUtils.getPotion(itemStack))).value() == Potions.EMPTY*//*?} else {*/ (p = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion().orElse(null)) == null/*?}*/) {
                return defaultPassInteraction();
            }
            level.setBlockAndUpdate(blockPos, Blocks.WATER_CAULDRON.defaultBlockState());
            level.getBlockEntity(blockPos, LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get()).ifPresent(be -> {
                be.potion = p;
                be.lastPotionItemUsed = itemStack.getItemHolder();
                be.setChanged();
                if (be.hasWater()) sendCauldronBubblesParticles(level, blockPos);
            });
            if (!level.isClientSide()) {
                Item item = itemStack.getItem();
                player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(item));
                level.playSound(null, blockPos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
                return consumeInteraction();
            }
            return successInteraction();
        };
        putInteractionOrFallback(emptyCauldron, Items.POTION, emptyCauldronPotion);
        putInteractionOrFallback(emptyCauldron, Items.SPLASH_POTION, emptyCauldronPotion);
        putInteractionOrFallback(emptyCauldron, Items.LINGERING_POTION, emptyCauldronPotion);
        putInteractionOrFallback(waterCauldron, Items.GLASS_BOTTLE, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || be.waterColor != null)
                return defaultPassInteraction();
            if (!level.isClientSide()) {
                Item item = itemStack.getItem();
                player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, LegacyItemUtil.setItemStackPotion(new ItemStack(be.lastPotionItemUsed), be.potion)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(item));
                LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
                sendCauldronSplashParticles(level, blockPos);
                level.playSound(null, blockPos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, blockPos);
                return consumeInteraction();
            }
            return successInteraction();
        });
        CauldronInteraction waterCauldronPotion = (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            Holder<Potion> p;
            if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || (p = LegacyItemUtil.getPotionContent(itemStack)) == null || (blockState.getValue(LayeredCauldronBlock.LEVEL) == 3 && be.potion.equals(p))) {
                return defaultPassInteraction();
            }
            if (!be.potion.equals(p)) {
                level.setBlockAndUpdate(blockPos, CAULDRON.defaultBlockState());
                if (!be.potion.equals(p) && !level.isClientSide())
                    level.playSound(null, blockPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
            } else {
                level.setBlockAndUpdate(blockPos, blockState.cycle(LayeredCauldronBlock.LEVEL));
                if (be.waterColor != null) {
                    be.setWaterColor(null);
                    level.setBlockAndUpdate(blockPos, blockState.setValue(LayeredCauldronBlock.LEVEL, 1));
                    be.setRemoved();
                }
                be.lastPotionItemUsed = itemStack.getItemHolder();
                if (!level.isClientSide())
                    level.playSound(null, blockPos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            if (!level.isClientSide()) {
                player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
                if (be.hasWater() && !be.isRemoved()) sendCauldronBubblesParticles(level, blockPos);
                return consumeInteraction();
            }
            return successInteraction();
        };
        putInteractionOrFallback(waterCauldron, Items.POTION, waterCauldronPotion);
        putInteractionOrFallback(waterCauldron, Items.SPLASH_POTION, waterCauldronPotion);
        putInteractionOrFallback(waterCauldron, Items.LINGERING_POTION, waterCauldronPotion);
        putInteractionOrFallback(waterCauldron, Items.ARROW, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || be.hasWater()) {
                return defaultPassInteraction();
            }
            if (!level.isClientSide()) {
                int l = blockState.getValue(LayeredCauldronBlock.LEVEL);
                int arrowCount = Math.min(itemStack.getCount(), l < 3 ? l * 16 : 64);
                //? if <1.20.5 {
                /*if(!player.getAbilities().instabuild) itemStack.shrink(arrowCount);
                 *///?} else {
                itemStack.consume(arrowCount, player);
                //?}
                ItemStack tippedArrow = LegacyItemUtil.setItemStackPotion(new ItemStack(Items.TIPPED_ARROW, arrowCount), be.potion);
                player.getInventory().placeItemBackInInventory(tippedArrow);
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                int i = (int) Math.min(3, Math.ceil(arrowCount / 16d));
                BlockState blockState2 = l - i == 0 ? Blocks.CAULDRON.defaultBlockState() : blockState.setValue(LayeredCauldronBlock.LEVEL, i);
                level.setBlockAndUpdate(blockPos, blockState2);
                level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(blockState2));
                return consumeInteraction();
            }
            return successInteraction();
        });

        BiFunction<CauldronInteraction, CauldronInteraction, CauldronInteraction> beforeInteraction = (a, b) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (LegacyMixinToggles.legacyCauldrons.get())
                b.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return a.interact(blockState, level, blockPos, player, interactionHand, itemStack);
        };

        CauldronInteraction fillWater = (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            sendCauldronBubblesParticles(level, blockPos);
            if (level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) {
                be.setWaterColor(null);
                if (!be.hasWater()) {
                    be.potion = be.getDefaultPotion();
                    if (!level.isClientSide())
                        level.playSound(null, blockPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
            return successInteraction();
        };

        waterCauldron.merge(Items.WATER_BUCKET, fillWater, beforeInteraction);
        emptyCauldron.merge(Items.WATER_BUCKET, fillWater, beforeInteraction);
        powderSnowCauldron.merge(Items.WATER_BUCKET, fillWater, beforeInteraction);
        lavaCauldron.merge(Items.WATER_BUCKET, fillWater, beforeInteraction);

        CauldronInteraction fillLava = (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (level instanceof ServerLevel sl) {
                Vec3 center = blockPos.getCenter();
                sl.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.5F, center.z, 2, 0.2, 0.1, 0.2, 0.02f);
            }
            return successInteraction();
        };

        waterCauldron.merge(Items.LAVA_BUCKET, fillLava, beforeInteraction);
        emptyCauldron.merge(Items.LAVA_BUCKET, fillLava, beforeInteraction);
        powderSnowCauldron.merge(Items.LAVA_BUCKET, fillLava, beforeInteraction);
        lavaCauldron.merge(Items.LAVA_BUCKET, fillLava, beforeInteraction);

        putInteractionOrFallback(waterCauldron, Items.BUCKET, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!LegacyRegistries.isInvalidCauldron(blockState, level, blockPos)) {
                sendCauldronSplashParticles(level, blockPos);
                return CauldronInteraction.fillBucket(blockState, level, blockPos, player, interactionHand, itemStack, new ItemStack(Items.WATER_BUCKET), (blockStatex) -> blockStatex.getValue(LayeredCauldronBlock.LEVEL) == 3, SoundEvents.BUCKET_FILL);
            }
            return consumeInteraction();
        });

        for (DyeColor color : DyeColor.values()) {
            putInteractionOrFallback(waterCauldron, DyeItem.byColor(color), (blockState, level, blockPos, player, interactionHand, itemStack) -> {
                if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || !(itemStack.getItem() instanceof DyeItem) || !be.hasWater()) {
                    return defaultPassInteraction();
                }
                int dyeColor = LegacyItemUtil.getDyeColor(color);
                if (be.waterColor == null) be.setWaterColor(dyeColor);
                else
                    be.setWaterColor(be.waterColor = LegacyItemUtil.mixColors(List.of(be.waterColor, dyeColor).iterator()));
                be.setChanged();

                if (!level.isClientSide()) {
                    level.playSound(null, blockPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.25f, 1.0f);
                    sendCauldronBubblesParticles(level, blockPos);
                }

                return level.isClientSide() ? successInteraction() : consumeInteraction();
            });
        }
    }

    public static void putInteractionOrFallback(Map<Item, CauldronInteraction> interactionMap, Item item, CauldronInteraction cauldronInteraction) {
        putInteractionOrFallback(interactionMap, item, cauldronInteraction, LegacyMixinToggles.legacyCauldrons::get);
    }

    public static void putInteractionOrFallback(Map<Item, CauldronInteraction> interactionMap, Item item, CauldronInteraction cauldronInteraction, BooleanSupplier supplier) {
        interactionMap.merge(item, cauldronInteraction, (a, b) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (supplier.getAsBoolean()) {
                return b.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            }
            return a.interact(blockState, level, blockPos, player, interactionHand, itemStack);
        });

    }

    public static void registerDyedWaterCauldronInteraction(Map<Item, CauldronInteraction> waterCauldron) {
        if (!LegacyMixinToggles.legacyCauldrons.get()) return;
        BuiltInRegistries.ITEM.asHolderIdMap().forEach(i -> {
            if (!LegacyItemUtil.isDyeableItem(i)) return;
            waterCauldron.put(i.value(), (blockState, level, blockPos, player, interactionHand, itemStack) -> {
                if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || !be.hasWater() || (LegacyItemUtil.isDyedItem(itemStack) && be.waterColor == null)) {
                    return defaultPassInteraction();
                }

                if (!level.isClientSide()) {
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                    if (be.waterColor == null) /*? if <1.20.5 {*//*((DyeableLeatherItem)itemStack.getItem()).clearColor(itemStack)*//*?} else {*/
                        itemStack.set(DataComponents.DYED_COLOR, null)/*?}*/;
                    else {
                        LegacyItemUtil.dyeItem(itemStack, be.waterColor);
                        level.playSound(null, blockPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.25f, 1.0f);
                        sendCauldronSplashParticles(level, blockPos);
                    }
                    ColoredWaterCauldronBlock.lowerFillLevel(be);
                }
                return successInteraction();
            });
        });
    }

    public static void sendCauldronBubblesParticles(Level level, BlockPos blockPos) {
        if (level instanceof ServerLevel sl) {
            Vec3 center = blockPos.getCenter();
            sl.sendParticles(ParticleTypes.BUBBLE, center.x, center.y + 0.5F, center.z, 2, 0.2, 0.1, 0.2, 0.02f);
        }
    }

    public static void sendCauldronSplashParticles(Level level, BlockPos blockPos) {
        if (level instanceof ServerLevel sl) {
            Vec3 center = blockPos.getCenter();
            sl.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.5F, center.z, 2, 0.2, 0.2, 0.2, 1);
        }
    }

    public static InteractionResult defaultPassInteraction() {
        return InteractionResult.PASS;
    }

    public static InteractionResult successInteraction() {
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult consumeInteraction() {
        return InteractionResult.CONSUME;
    }
}
