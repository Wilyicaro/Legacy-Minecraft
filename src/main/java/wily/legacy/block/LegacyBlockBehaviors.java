package wily.legacy.block;

import net.minecraft.core.*;
import net.minecraft.core.cauldron.CauldronInteraction;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
 //?}
//? if >1.20.1 {
import net.minecraft.core.dispenser.BlockSource;
 //?} else {
//?}
//? if >=1.20.5 && <1.21.2 {
import net.minecraft.world.ItemInteractionResult;
 //?}
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
//? if <1.21.2 {
import net.minecraft.world.entity.MobSpawnType;
//?} else {
/*import net.minecraft.world.entity.EntitySpawnReason;
*///?}
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.FactoryAPI;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.config.LegacyMixinToggles;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.LegacyItemUtil;
//? if <1.21.1 {
/*import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
*///?}

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import static net.minecraft.world.level.block.Blocks.CAULDRON;

public class LegacyBlockBehaviors {
    private static final TagKey<Item> CAULDRON_CAN_REMOVE_DYE = TagKey.create(Registries.ITEM, FactoryAPI.createVanillaLocation("cauldron_can_remove_dye"));
    private static final TagKey<Item> DYEABLE_ITEMS = TagKey.create(Registries.ITEM, FactoryAPI.createVanillaLocation("dyeable"));

    public static void setup() {
        registerTridentDispenseBehavior();

        //? if <1.21.5 {
        DispenserBlock.registerBehavior(Blocks.TNT, new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
                setSuccess(blockSource./*? if >1.20.1 {*/level/*?} else {*//*getLevel*//*?}*/().getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES));
                if (isSuccess()){
                    BlockPos blockPos = blockSource./*? if >1.20.1 {*/pos/*?} else {*//*getPos*//*?}*/().relative(blockSource./*? if >1.20.1 {*/state/*?} else {*//*getBlockState*//*?}*/().getValue(DispenserBlock.FACING));
                    TntBlock.explode(blockSource./*? if >1.20.1 {*/level/*?} else {*//*getLevel*//*?}*/(), blockPos);
                    blockSource./*? if >1.20.1 {*/level/*?} else {*//*getLevel*//*?}*/().gameEvent(null, GameEvent.ENTITY_PLACE, blockSource./*? if >1.20.1 {*/pos/*?} else {*//*getPos*//*?}*/());
                    itemStack.shrink(1);
                }
                return itemStack;
            }

            @Override
            protected void playAnimation(BlockSource blockSource, Direction direction) {
                if (isSuccess()) super.playAnimation(blockSource, direction);
            }
        });
        //?}

        DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior() {
            public ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
                Direction direction = blockSource./*? if >1.20.1 {*/state/*?} else {*//*getBlockState*//*?}*/().getValue(DispenserBlock.FACING);
                EntityType<?> entityType = ((SpawnEggItem)itemStack.getItem()).getType(/*? if >=1.21.4 {*//*blockSource.level().registryAccess(), *//*?}*//*? if <1.20.5 {*//*null*//*?} else {*/itemStack/*?}*/);

                try {
                    if (entityType.spawn(blockSource./*? if >1.20.1 {*/level/*?} else {*//*getLevel*//*?}*/(), itemStack, null, blockSource./*? if >1.20.1 {*/pos/*?} else {*//*getPos*//*?}*/().relative(direction), /*? if <1.21.3 {*/MobSpawnType/*?} else {*//*EntitySpawnReason*//*?}*/.DISPENSER, direction != Direction.UP, false) != null){
                        itemStack.shrink(1);
                        blockSource./*? if >1.20.1 {*/level/*?} else {*//*getLevel*//*?}*/().gameEvent(null, GameEvent.ENTITY_PLACE, blockSource./*? if >1.20.1 {*/pos/*?} else {*//*getPos*//*?}*/());
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
        CauldronInteraction emptyCauldronPotion = (blockState, level, blockPos, player, interactionHand, itemStack) ->{
            Holder<Potion> p = LegacyItemUtil.getPotionContent(itemStack);
            if (p == null) {
                return defaultPassInteraction();
            }
            level.setBlockAndUpdate(blockPos, Blocks.WATER_CAULDRON.defaultBlockState());
            level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get()).ifPresent(be->{
                be.potion = p;
                be.lastPotionItemUsed = itemStack.getItemHolder();
                be.setChanged();
                if (be.hasWater()) sendCauldronBubblesParticles(level, blockPos);
            });
            if (!level.isClientSide) {
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
        putInteractionOrFallback(emptyCauldron, Items.POTION,emptyCauldronPotion);
        putInteractionOrFallback(emptyCauldron, Items.SPLASH_POTION,emptyCauldronPotion);
        putInteractionOrFallback(emptyCauldron, Items.LINGERING_POTION,emptyCauldronPotion);
        putInteractionOrFallback(waterCauldron, Items.GLASS_BOTTLE, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || be.waterColor != null) return defaultPassInteraction();
            if (!level.isClientSide) {
                Item item = itemStack.getItem();
                player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, LegacyItemUtil.setItemStackPotion(new ItemStack(be.lastPotionItemUsed),be.potion)));
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
            if (!be.potion.equals(p)){
                level.setBlockAndUpdate(blockPos, CAULDRON.defaultBlockState());
                if (!be.potion.equals(p) && !level.isClientSide) level.playSound(null, blockPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
            }else{
                level.setBlockAndUpdate(blockPos, blockState.cycle(LayeredCauldronBlock.LEVEL));
                if (be.waterColor != null) {
                    be.setWaterColor(null);
                    level.setBlockAndUpdate(blockPos, blockState.setValue(LayeredCauldronBlock.LEVEL,1));
                    be.setRemoved();
                }
                be.lastPotionItemUsed = itemStack.getItemHolder();
                if (!level.isClientSide) level.playSound(null, blockPos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            if (!level.isClientSide) {
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
            if (!level.isClientSide) {
                int l = blockState.getValue(LayeredCauldronBlock.LEVEL);
                int arrowCount = Math.min(itemStack.getCount(), l < 3 ? l * 16 : 64);
                //? if <1.20.5 {
                /*if(!player.getAbilities().instabuild) itemStack.shrink(arrowCount);
                 *///?} else {
                itemStack.consume(arrowCount,player);
                //?}
                ItemStack tippedArrow = LegacyItemUtil.setItemStackPotion(new ItemStack(Items.TIPPED_ARROW,arrowCount), be.potion);
                player.getInventory().placeItemBackInInventory(tippedArrow);
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                int i = (int) Math.min(3,Math.ceil(arrowCount / 16d));
                BlockState blockState2 = l - i  == 0 ? Blocks.CAULDRON.defaultBlockState() : blockState.setValue(LayeredCauldronBlock.LEVEL, i);
                level.setBlockAndUpdate(blockPos, blockState2);
                level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(blockState2));
                return consumeInteraction();
            }
            return successInteraction();
        });

        BiFunction<CauldronInteraction,CauldronInteraction,CauldronInteraction> beforeInteraction = (a, b)-> (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (LegacyMixinToggles.legacyCauldrons.get()) b.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            return a.interact(blockState, level, blockPos, player, interactionHand, itemStack);
        };

        CauldronInteraction fillWater = (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            sendCauldronBubblesParticles(level, blockPos);
            if (level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) {
                be.setWaterColor(null);
                if (!be.hasWater()) {
                    be.potion = be.getDefaultPotion();
                    if (!level.isClientSide) level.playSound(null, blockPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
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
            if (!LegacyRegistries.isInvalidCauldron(blockState, level, blockPos)){
                sendCauldronSplashParticles(level, blockPos);
                return CauldronInteraction.fillBucket(blockState, level, blockPos, player, interactionHand, itemStack, new ItemStack(Items.WATER_BUCKET), (blockStatex) -> blockStatex.getValue(LayeredCauldronBlock.LEVEL) == 3, SoundEvents.BUCKET_FILL);
            }
            return consumeInteraction();
        });

        CauldronInteraction dyeCauldronInteraction = (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            DyeColor color = LegacyItemUtil.getDyeColorOrNull(itemStack.getItem());
            if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || color == null || !be.hasWater()) {
                return defaultPassInteraction();
            }
            int dyeColor = LegacyItemUtil.getDyeColor(color);
            if (be.waterColor == null) be.setWaterColor(dyeColor);
            else be.setWaterColor(be.waterColor = LegacyItemUtil.mixColors(List.of(be.waterColor,dyeColor).iterator()));
            be.setChanged();

            if (!level.isClientSide) {
                level.playSound(null, blockPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.25f, 1.0f);
                sendCauldronBubblesParticles(level, blockPos);
            }

            return level.isClientSide ? successInteraction() : consumeInteraction();
        };
        for (DyeColor color : DyeColor.values()) {
            putInteractionOrFallback(waterCauldron, LegacyItemUtil.getDyeItem(color), dyeCauldronInteraction);
            Item legacyDyeItem = LegacyItemUtil.getLegacyDyeItem(color);
            if (legacyDyeItem != null) putInteractionOrFallback(waterCauldron, legacyDyeItem, dyeCauldronInteraction);
        }


        //? if <1.20.5 {
        /*registerDyedWaterCauldronInteraction(waterCauldron);
        *///?}
    }

    public static void putInteractionOrFallback(Map<Item, CauldronInteraction> interactionMap, Item item, CauldronInteraction cauldronInteraction){
        putInteractionOrFallback(interactionMap, item, cauldronInteraction, LegacyMixinToggles.legacyCauldrons::get);
    }

    public static void putInteractionOrFallback(Map<Item, CauldronInteraction> interactionMap, Item item, CauldronInteraction cauldronInteraction, BooleanSupplier supplier){
        interactionMap.merge(item, cauldronInteraction, (a, b) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (supplier.getAsBoolean()) {
                return b.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            }
            return a.interact(blockState, level, blockPos, player, interactionHand, itemStack);
        });

    }

    public static void registerDyedWaterCauldronInteraction(Map<Item, CauldronInteraction> waterCauldron) {
        if (!LegacyMixinToggles.legacyCauldrons.get()) return;
        BuiltInRegistries.ITEM.asHolderIdMap().forEach(i-> {
            if (!LegacyItemUtil.isDyeableItem(i)) return;
            waterCauldron.put(i.value(),(blockState, level, blockPos, player, interactionHand, itemStack) -> {
                if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || !be.hasWater() || (LegacyItemUtil.isDyedItem(itemStack) && be.waterColor == null)) {
                    return defaultPassInteraction();
                }

                if (!level.isClientSide) {
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                    if (be.waterColor == null) /*? if <1.20.5 {*//*((DyeableLeatherItem)itemStack.getItem()).clearColor(itemStack)*//*?} else {*/itemStack.set(DataComponents.DYED_COLOR,null)/*?}*/;
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

    private static void registerTridentDispenseBehavior() {
        //? if <1.21.1 {
        /*DispenserBlock.registerBehavior(Items.TRIDENT, new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level level, Position position, ItemStack itemStack) {
                ThrownTrident trident = new ThrownTrident(EntityType.TRIDENT, level);
                CompoundTag tag = new CompoundTag();
                tag.put("Trident", itemStack.copyWithCount(1).save(new CompoundTag()));
                trident.readAdditionalSaveData(tag);
                trident.setPos(position.x(), position.y(), position.z());
                trident.pickup = AbstractArrow.Pickup.ALLOWED;
                return trident;
            }
        });
        *///?} else {
        DispenserBlock.registerProjectileBehavior(Items.TRIDENT);
        //?}
    }

    public static void sendCauldronBubblesParticles(Level level, BlockPos blockPos){
        if (level instanceof ServerLevel sl) {
            Vec3 center = blockPos.getCenter();
            sl.sendParticles(ParticleTypes.BUBBLE, center.x, center.y + 0.5F, center.z, 2, 0.2, 0.1, 0.2, 0.02f);
        }
    }

    public static void sendCauldronSplashParticles(Level level, BlockPos blockPos){
        if (level instanceof ServerLevel sl) {
            Vec3 center = blockPos.getCenter();
            sl.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.5F, center.z, 2, 0.2, 0.2, 0.2, 1);
        }
    }

    public static /*? if <1.20.5 || >=1.21.2 {*/ /*InteractionResult *//*?} else {*/ItemInteractionResult/*?}*/ defaultPassInteraction() {
        return /*? if <1.20.5 || >=1.21.2 {*/ /*InteractionResult.PASS*//*?} else {*/ ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION/*?}*/;
    }

    public static /*? if <1.20.5 || >=1.21.2 {*/ /*InteractionResult *//*?} else {*/ItemInteractionResult/*?}*/ successInteraction() {
        return /*? if <1.20.5 || >=1.21.2 {*/ /*InteractionResult.SUCCESS*//*?} else {*/ ItemInteractionResult.SUCCESS/*?}*/;
    }
    public static /*? if <1.20.5 || >=1.21.2 {*/ /*InteractionResult *//*?} else {*/ItemInteractionResult/*?}*/ consumeInteraction() {
        return /*? if <1.20.5 || >=1.21.2 {*/ /*InteractionResult.CONSUME*//*?} else {*/ ItemInteractionResult.CONSUME/*?}*/;
    }
}
