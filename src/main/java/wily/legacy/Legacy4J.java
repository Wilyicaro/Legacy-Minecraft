package wily.legacy;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
//? if >1.20.1 {
import net.minecraft.core.dispenser.BlockSource;
//?} else {
/*import net.minecraft.core.BlockSource;
*///?}
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
//? if >=1.20.5 && <1.21.2 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.alchemy.Potion;
//? if >=1.20.5 {
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
//?} else {
/*import net.minecraft.world.item.alchemy.PotionUtils;
import wily.factoryapi.util.ColorUtil;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.util.ItemAccessor;
*///?}
//? if <1.21.2 {
import net.minecraft.world.entity.MobSpawnType;
//?} else {
/*import net.minecraft.world.entity.EntitySpawnReason;
import wily.factoryapi.base.network.CommonRecipeManager;
*///?}
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.block.ColoredWaterCauldronBlock;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.config.LegacyMixinToggles;
import wily.legacy.config.LegacyWorldOptions;
import wily.legacy.init.*;
import wily.legacy.network.*;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.ArmorStandPose;

//? if fabric {
//?} else if forge {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
*///?} else if neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
*///?}

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.minecraft.world.level.block.Blocks.CAULDRON;

//? if forge || neoforge
/*@Mod(Legacy4J.MOD_ID)*/
public class Legacy4J {

    public static final String MOD_ID = "legacy";
    public static final Supplier<String> VERSION = ()-> FactoryAPIPlatform.getModInfo(MOD_ID).getVersion();
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final FactoryConfig.StorageHandler MIXIN_CONFIGS_STORAGE = FactoryConfig.StorageHandler.fromMixin(LegacyMixinToggles.COMMON_STORAGE, true);

    private static Collection<CommonNetwork.Payload> playerInitialPayloads = Collections.emptySet();

    public Legacy4J(){
        init();
        //? if forge || neoforge {
        /*if (FMLEnvironment.dist == Dist.CLIENT)
            Legacy4JClient.init();
        *///?}
    }

    public static List<Integer> getParsedVersion(String version){
        List<Integer> parsedVersion = new ArrayList<>();
        String[] versions = version.split("[.\\-]");
        for (String s : versions) {
            int value;
            try {
                value = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                value = 0;
            }
            parsedVersion.add(value);
        }
        return parsedVersion;
    }

    public static boolean isNewerVersion(String actualVersion, String previous){
        return isNewerVersion(actualVersion, previous, 2);
    }

    public static boolean isNewerVersion(String actualVersion, String previous, int limitCount){
        List<Integer> v = getParsedVersion(actualVersion);
        List<Integer> v1 = getParsedVersion(previous);
        int size = limitCount <= 0 ? v.size() : Math.min(limitCount, v.size());
        for (int i = 0; i < size; i++) {
            if (v.get(i) > (v1.size() <= i ? 0 : v1.get(i))) return true;
        }
        return false;
    }

    public static void init(){
        FactoryConfig.registerCommonStorage(createModLocation("common"), LegacyCommonOptions.COMMON_STORAGE);
        FactoryConfig.registerCommonStorage(createModLocation("mixin_common"), MIXIN_CONFIGS_STORAGE);
        LegacyRegistries.register();
        LegacyGameRules.init();
        FactoryEvent.registerPayload(r->{
            r.register(false, ClientAdvancementsPayload.ID);
            r.register(false, ClientAnimalInLoveSyncPayload.ID);
            r.register(false, ClientEffectActivationPayload.ID);
            r.register(true, ClientMerchantTradingPayload.ID_C2S);
            r.register(false, ClientMerchantTradingPayload.ID_S2C);
            r.register(true, PlayerInfoSync.ID);
            r.register(true, PlayerInfoSync.All.ID_C2S);
            r.register(false, PlayerInfoSync.All.ID_S2C);
            r.register(true, ServerMenuCraftPayload.ID);
            r.register(true, ServerOpenClientMenuPayload.ID);
            r.register(true, ServerPlayerMissHitPayload.ID);
            r.register(false, TipCommand.Payload.ID);
            r.register(false, TipCommand.EntityPayload.ID);
            r.register(false, TopMessage.Payload.ID);
        });
        ArmorStandPose.init();
        //? if >=1.20.5 {
        FactoryEvent.setItemComponent(Items.CAKE,DataComponents.MAX_STACK_SIZE,64);
        //?} else {
        /*ItemAccessor.of(Items.CAKE).setMaxStackSize(64);
        ItemAccessor.of(Items.MUSIC_DISC_CAT).setRecordLengthInTicks(330);
        *///?}
        FactoryEvent.registerCommands((dispatcher,context,selection)->{
            TipCommand.register(dispatcher,context,selection);
        });
        FactoryEvent.setup(Legacy4J::setup);
        FactoryEvent.tagsLoaded(Legacy4J::tagsLoaded);
        FactoryEvent.serverStarted(Legacy4J::onServerStart);
        FactoryEvent.PlayerEvent.JOIN_EVENT.register(Legacy4J::onServerPlayerJoin);
        FactoryEvent.PlayerEvent.RELOAD_RESOURCES_EVENT.register(Legacy4J::onResourcesReload);
    }

    public static ResourceLocation createModLocation(String path){
        return FactoryAPI.createLocation(MOD_ID,path);
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

    public static void setup(){
        LegacyCommonOptions.COMMON_STORAGE.load();
        //? if >=1.21.2 {
        /*CommonRecipeManager.addRecipeTypeToSync(RecipeType.CRAFTING);
        CommonRecipeManager.addRecipeTypeToSync(RecipeType.STONECUTTING);
        *///?}

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
            Holder<Potion> p;
            if (/*? if <1.20.5 {*//*(p = BuiltInRegistries.POTION.wrapAsHolder(PotionUtils.getPotion(itemStack))).value() == Potions.EMPTY*//*?} else {*/ (p = itemStack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).potion().orElse(null)) == null/*?}*/) {
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
                player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, setItemStackPotion(new ItemStack(be.lastPotionItemUsed),be.potion)));
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
            if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || (p = getPotionContent(itemStack)) == null || (blockState.getValue(LayeredCauldronBlock.LEVEL) == 3 && be.potion.equals(p))) {
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
                ItemStack tippedArrow = setItemStackPotion(new ItemStack(Items.TIPPED_ARROW,arrowCount), be.potion);
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

        for (DyeColor color : DyeColor.values()) {
            putInteractionOrFallback(waterCauldron, DyeItem.byColor(color),(blockState, level, blockPos, player, interactionHand, itemStack) -> {
                if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || !(itemStack.getItem() instanceof DyeItem) || !be.hasWater()) {
                    return defaultPassInteraction();
                }
                int dyeColor = getDyeColor(color);
                if (be.waterColor == null) be.setWaterColor(dyeColor);
                else be.setWaterColor(be.waterColor = Legacy4J.mixColors(List.of(be.waterColor,dyeColor).iterator()));
                be.setChanged();

                if (!level.isClientSide) {
                    level.playSound(null, blockPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.25f, 1.0f);
                    sendCauldronBubblesParticles(level, blockPos);
                }

                return level.isClientSide ? successInteraction() : consumeInteraction();
            });
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

    public static boolean isChunkPosVisibleInSquare(int centerX, int centerZ, int viewDistance, int x, int z, boolean offset){
        int n = Math.max(0, Math.abs(x - centerX) - 1);
        int o = Math.max(0, Math.abs(z - centerZ) - 1);
        long p = Math.max(0, Math.max(n, o) - (offset ? 1 : 0));
        long q = Math.min(n, o);
        return Math.max(p, q) < viewDistance;
    }

    public static Holder<Potion> getPotionContent(ItemStack itemStack){
        //? if <1.20.5 {
        /*Holder<Potion> potion = BuiltInRegistries.POTION.wrapAsHolder(PotionUtils.getPotion(itemStack));
        return potion.value() == Potions.EMPTY ? null : potion;
        *///?} else {
        return itemStack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).potion().orElse(null);
        //?}
    }

    public static ItemStack setItemStackPotion(ItemStack stack, Holder<Potion> potion){
        //? if <1.20.5 {
        /*return PotionUtils.setPotion(stack, potion.value());
        *///?} else {
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return stack;
        //?}
    }

    public static void addPotionTooltip(Holder<Potion> potion, List<Component> tooltipList, float f/*? if >=1.20.3 {*/, float tickRate/*?}*/){
        //? if <1.20.5 {
        /*PotionUtils.addPotionTooltip(potion.value().getEffects(), tooltipList, f/^? if >=1.20.3 {^/, tickRate/^?}^/);
        *///?} else {
        PotionContents.addPotionTooltip(potion.value().getEffects(), tooltipList::add, f, tickRate);
        //?}
    }

    public static int getDyeColor(DyeColor dyeColor){
        return /*? if <1.20.5 {*//*ColorUtil.colorFromFloat(dyeColor.getTextureDiffuseColors())*//*?} else {*/dyeColor.getTextureDiffuseColor()/*?}*/;
    }

    public static float getItemDamageModifier(ItemStack stack){
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat)){
            if (stack.is(ItemTags.SWORDS)) return 1;
            else if (stack.getItem() instanceof ShovelItem) return -0.5f;
            else if (stack.is(ItemTags.PICKAXES)) return 1;
            else if (stack.getItem() instanceof AxeItem) {
                if (stack.is(Items.STONE_AXE)) return -4;
                else if (stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE)) return -2;
                else return -3;
            }
        }
        return 0;
    }

    public static void tagsLoaded(){
        //? if >=1.20.5 {
        registerDyedWaterCauldronInteraction(CauldronInteraction.WATER.map());
        //?}
    }

    public static void registerDyedWaterCauldronInteraction(Map<Item, CauldronInteraction> waterCauldron){
        if (!LegacyMixinToggles.legacyCauldrons.get()) return;
        BuiltInRegistries.ITEM.asHolderIdMap().forEach(i-> {
            if (!isDyeableItem(i)) return;
            waterCauldron.put(i.value(),(blockState, level, blockPos, player, interactionHand, itemStack) -> {
                if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || !be.hasWater() || (isDyedItem(itemStack) && be.waterColor == null)) {
                    return defaultPassInteraction();
                }

                if (!level.isClientSide) {
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                    if (be.waterColor == null) /*? if <1.20.5 {*//*((DyeableLeatherItem)itemStack.getItem()).clearColor(itemStack)*//*?} else {*/itemStack.set(DataComponents.DYED_COLOR,null)/*?}*/;
                    else {
                        Legacy4J.dyeItem(itemStack, be.waterColor);
                        level.playSound(null, blockPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.25f, 1.0f);
                        sendCauldronSplashParticles(level, blockPos);
                    }
                    ColoredWaterCauldronBlock.lowerFillLevel(be);
                }
                return successInteraction();
            });
        });
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

    public static Vec3 getRelativeMovement(LivingEntity entity, float f, Vec3 vec3, int relRot){
        vec3 = getNormal(vec3,Math.toRadians(relRot));
        double d = vec3.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec32 = (d > 1.0 ? vec3.normalize() : vec3).scale(f);
            double angle = Math.toRadians(relRot == 0 ? entity.getYRot() : Math.round(entity.getYRot() / relRot) * relRot);
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            return new Vec3(vec32.x * cos - vec32.z * sin, vec32.y, vec32.z * cos + vec32.x * sin);
        }
    }

    public static Vec3 getNormal(Vec3 vec3, double relRot){
        if (relRot == 0) return vec3;
        double angleRad = Math.atan2(vec3.z, vec3.x);
        double quantizedAngle = Math.round(angleRad / relRot) * relRot;
        double length = vec3.length();
        return new Vec3(length*Math.cos(quantizedAngle), vec3.y,length*Math.sin(quantizedAngle));
    }

    public static boolean canRepair(ItemStack repairItem, ItemStack ingredient){
        return repairItem.is(ingredient.getItem()) && repairItem.getCount() == 1 && ingredient.getCount() == 1 && /*? if <1.20.5 {*//*repairItem.getItem().canBeDepleted()*//*?} else {*/repairItem.getItem().components().has(DataComponents.DAMAGE) && !repairItem.isEnchanted()/*?}*/ && !ingredient.isEnchanted();
    }

    public static boolean isDyedItem(ItemStack itemStack){
        return /*? if <1.20.5 {*//*((DyeableLeatherItem)itemStack.getItem()).hasCustomColor(itemStack) *//*?} else {*/itemStack.get(DataComponents.DYED_COLOR) == null/*?}*/;
    }

    public static boolean isDyeableItem(Holder<Item> item){
        return /*? if <1.20.5 {*//*(item.value() instanceof DyeableLeatherItem)*//*?} else {*/item.is(ItemTags.DYEABLE)/*?}*/;
    }

    public static ItemStack dyeItem(ItemStack itemStack, int color) {
        List<Integer> colors = new ArrayList<>();
        //? if <1.20.5 {
        /*DyeableLeatherItem dyeableLeatherItem = null;
        Item item = itemStack.getItem();
        if (item instanceof DyeableLeatherItem) {
            dyeableLeatherItem = (DyeableLeatherItem) item;
            if (dyeableLeatherItem.hasCustomColor(itemStack)) colors.add(dyeableLeatherItem.getColor(itemStack));
            colors.add(color);
        }
        if (dyeableLeatherItem != null) dyeableLeatherItem.setColor(itemStack, mixColors(colors.iterator()));
        *///?} else {
        DyedItemColor dyedItemColor = itemStack.get(DataComponents.DYED_COLOR);
        /*? if <1.21.5 {*/boolean bl = dyedItemColor == null || dyedItemColor.showInTooltip();/*?}*/
        if (dyedItemColor != null) colors.add(color);
        colors.add(color);
        itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(mixColors(colors.iterator())/*? if <1.21.5 {*/, bl/*?}*/));
        //?}
        return itemStack;
    }

    public static int mixColors(Iterator<Integer> colors){
        int n;
        float h;
        int[] is = new int[3];
        int i = 0;
        int j = 0;

        for (Iterator<Integer> it = colors; it.hasNext(); ) {
            Integer color = it.next();
            float f = (float)(color >> 16 & 0xFF) / 255.0f;
            float g = (float)(color >> 8 & 0xFF) / 255.0f;
            h = (float)(color & 0xFF) / 255.0f;
            i += (int)(Math.max(f, Math.max(g, h)) * 255.0f);
            is[0] = is[0] + (int)(f * 255.0f);
            is[1] = is[1] + (int)(g * 255.0f);
            is[2] = is[2] + (int)(h * 255.0f);
            ++j;
        }
        int k = is[0] / j;
        int o = is[1] / j;
        int p = is[2] / j;
        h = (float)i / (float)j;
        float q = Math.max(k, Math.max(o, p));
        k = (int)((float)k * h / q);
        o = (int)((float)o * h / q);
        p = (int)((float)p * h / q);
        n = k;
        n = (n << 8) + o;
        n = (n << 8) + p;
        return n;
    }

    public static boolean itemHasValidPatterns(ItemStack stack){
        int count = getItemPatternsCount(stack);
        return count > 0 && count <=6;
    }
    public static int getItemPatternsCount(ItemStack stack){
        //? if <1.20.5 {
        /*CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        return beTag == null ? 0 : beTag.contains("Patterns") ? beTag.getList("Patterns",10).size() : -1;
        *///?} else
        return stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).layers().size();
    }

    public static boolean anyArmorSlotMatch(Inventory inventory, Predicate<ItemStack> predicate){
        //? if <1.21.5 {
        return inventory.armor.stream().anyMatch(predicate);
        //?} else {
        /*return Inventory.EQUIPMENT_SLOT_MAPPING.int2ObjectEntrySet().stream().anyMatch(e-> e.getValue() != EquipmentSlot.OFFHAND && predicate.test(inventory.getItem(e.getIntKey())));
        *///?}
    }

    public static void onServerPlayerJoin(ServerPlayer p){
        if (p.getServer() == null) return;
        int pos = 0;
        boolean b = true;
        main : while (b) {
            b = false;
            for (ServerPlayer player : p.server.getPlayerList().getPlayers())
                if (player != p && ((LegacyPlayerInfo)player).getIdentifierIndex() == pos){
                    pos++;
                    b = true;
                    continue main;
                }
        }
        ((LegacyPlayerInfo)p).setIdentifierIndex(pos);
        CommonNetwork.sendToPlayers(p.getServer().getPlayerList().getPlayers().stream().filter(sp-> sp != p).collect(Collectors.toSet()), new PlayerInfoSync.All(Map.of(p.getUUID(),(LegacyPlayerInfo)p), Collections.emptyMap(), p.server.getDefaultGameType(),PlayerInfoSync.All.ID_S2C));

        CommonNetwork.sendToPlayer(p, PlayerInfoSync.All.fromPlayerList(p.getServer()), true);
        playerInitialPayloads.forEach(payload->CommonNetwork.sendToPlayer(p, payload, true));

        if (!p.server.isDedicatedServer()) Legacy4JClient.serverPlayerJoin(p);
    }

    public static void onServerStart(MinecraftServer server){
        playerInitialPayloads = createPlayerInitialPayloads(server);
        LegacyWorldOptions.WORLD_STORAGE.withServerFile(server, "legacy_data.json").load();
    }

    public static void onResourcesReload(PlayerList playerList){
        onServerStart(playerList.getServer());
        playerInitialPayloads.forEach(payload->CommonNetwork.sendToPlayers(playerList.getPlayers(), payload));
    }

    public static Collection<CommonNetwork.Payload> createPlayerInitialPayloads(MinecraftServer server){
        HashSet<CommonNetwork.Payload> payloads = new HashSet<>();
        payloads.add(new ClientAdvancementsPayload(/*? if >1.20.1 {*/List.copyOf(server.getAdvancements().getAllAdvancements())/*?} else {*//*server.getAdvancements().getAllAdvancements().stream().collect(Collectors.toMap(Advancement::getId, Advancement::deconstruct))*//*?}*/));
        return payloads;
    }

    public static void copySaveToDirectory(InputStream stream, File directory){
        if (directory.exists()) FileUtils.deleteQuietly(directory);
        try (ZipInputStream inputStream = new ZipInputStream(stream))
        {
            ZipEntry zipEntry;
            byte[] buffer = new byte[1024];
            while ((zipEntry = inputStream.getNextEntry()) != null)
            {
                File newFile = new File(directory, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = inputStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
