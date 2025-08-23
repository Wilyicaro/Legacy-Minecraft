package wily.legacy;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.init.*;
import wily.legacy.inventory.LegacyIngredient;
import wily.legacy.network.*;
import wily.legacy.player.LegacyPlayerInfo;
import wily.legacy.util.ArmorStandPose;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.minecraft.world.level.block.Blocks.CAULDRON;


public class Legacy4J {
    public static final CommonNetwork.SecureExecutor SECURE_EXECUTOR = new CommonNetwork.SecureExecutor() {
        @Override
        public boolean isSecure() {
            return true;
        }
    };

    public static LegacyServerProperties serverProperties;

    public static final String MOD_ID = "legacy";
    public static final Supplier<String> VERSION = ()-> Legacy4JPlatform.getModInfo(MOD_ID).getVersion();

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init(){
        LegacyRegistries.register();
        LegacyGameRules.init();
        CommonNetwork.register();
        ArmorStandPose.init();
        LegacyIngredient.init();
    }
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection environment){
        TipCommand.register(dispatcher,context);
    }
    public static void setup(){
        Items.CAKE.maxStackSize = 64;
        ((RecordItem)Items.MUSIC_DISC_CAT).lengthInTicks = 330;
        Map<Item, CauldronInteraction> emptyCauldron = CauldronInteraction.EMPTY.map();
        Map<Item, CauldronInteraction> waterCauldron = CauldronInteraction.WATER.map();
        CauldronInteraction emptyCauldronPotion = (blockState, level, blockPos, player, interactionHand, itemStack) ->{
            Potion p;
            if ((p = PotionUtils.getPotion(itemStack)) == Potions.EMPTY) {
                return InteractionResult.PASS;
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
            level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get()).ifPresent(be->{
                be.potion = p;
                be.setChanged();
            });
            return InteractionResult.sidedSuccess(level.isClientSide);
        };
        emptyCauldron.put(Items.POTION,emptyCauldronPotion);
        emptyCauldron.put(Items.SPLASH_POTION,emptyCauldronPotion);
        emptyCauldron.put(Items.LINGERING_POTION,emptyCauldronPotion);
        waterCauldron.put(Items.GLASS_BOTTLE, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get());
            if (opt.map(be->be.waterColor).orElse(null) != null) return InteractionResult.PASS;
            if (!level.isClientSide) {
                Item item = itemStack.getItem();
                player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, PotionUtils.setPotion(new ItemStack(Items.POTION), opt.map(be->be.potion).orElse(Potions.WATER))));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(item));
                LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
                level.playSound(null, blockPos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, blockPos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });
        CauldronInteraction waterCauldronPotion = (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            Potion p;
            Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get());
            if ((p = PotionUtils.getPotion(itemStack)) == Potions.EMPTY || (blockState.getValue(LayeredCauldronBlock.LEVEL) == 3 && (opt.isEmpty() || opt.get().potion == p))) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
            }
            if (opt.isPresent() && opt.get().potion != p){
                level.setBlockAndUpdate(blockPos, CAULDRON.defaultBlockState());
                if (opt.get().potion != p) level.playSound(null, blockPos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }else{
                if (opt.isEmpty() || opt.get().waterColor == null) level.setBlockAndUpdate(blockPos, blockState.cycle(LayeredCauldronBlock.LEVEL));
                else {
                    opt.get().waterColor = null;
                    level.setBlockAndUpdate(blockPos, blockState.setValue(LayeredCauldronBlock.LEVEL,1));
                    opt.get().setChanged();
                }
                level.playSound(null, blockPos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        };
        waterCauldron.put(Items.POTION, waterCauldronPotion);
        waterCauldron.put(Items.SPLASH_POTION, waterCauldronPotion);
        waterCauldron.put(Items.LINGERING_POTION, waterCauldronPotion);
        waterCauldron.put(Items.ARROW, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!(level.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be) || be.potion == Potions.WATER) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                int l = blockState.getValue(LayeredCauldronBlock.LEVEL);
                int arrowCount = Math.min(itemStack.getCount(), l < 3 ? l * 16 : 64);
                if(!player.getAbilities().instabuild) itemStack.shrink(arrowCount);
                ItemStack tippedArrow = new ItemStack(Items.TIPPED_ARROW,arrowCount);
                PotionUtils.setPotion(tippedArrow,be.potion);
                player.getInventory().placeItemBackInInventory(tippedArrow);
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                int i = (int) Math.min(3,Math.ceil(arrowCount / 16d));
                BlockState blockState2 = l - i  == 0 ? Blocks.CAULDRON.defaultBlockState() : blockState.setValue(LayeredCauldronBlock.LEVEL, i);
                level.setBlockAndUpdate(blockPos, blockState2);
                level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(blockState2));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });

        for (DyeColor color : DyeColor.values()) {
            waterCauldron.put(DyeItem.byColor(color),(blockState, level, blockPos, player, interactionHand, itemStack) -> {
                Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get());
                if (!(itemStack.getItem() instanceof DyeItem) || opt.isEmpty() || opt.get().potion != Potions.WATER) {
                    return InteractionResult.PASS;
                }
                int dyeColor = new Color(color.getTextureDiffuseColors()[0],color.getTextureDiffuseColors()[1],color.getTextureDiffuseColors()[2]).getRGB();
                if (opt.get().waterColor == null) opt.get().waterColor = dyeColor;
                else opt.get().waterColor = Legacy4J.mixColors(List.of(opt.get().waterColor,dyeColor).iterator());
                opt.get().setChanged();
                return InteractionResult.sidedSuccess(level.isClientSide);
            });
        }
        BuiltInRegistries.ITEM.stream().filter(i-> i instanceof DyeableLeatherItem).forEach(i-> waterCauldron.put(i,(blockState, level, blockPos, player, interactionHand, itemStack) -> {
            Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get());
            if (!(itemStack.getItem() instanceof DyeableLeatherItem dyeable) || (opt.isPresent() && opt.get().potion != Potions.WATER) || (!dyeable.hasCustomColor(itemStack) && (opt.isEmpty() || opt.get().waterColor == null))) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide) {
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                if (opt.isEmpty() || opt.get().waterColor == null) dyeable.clearColor(itemStack);
                else dyeArmor(itemStack,opt.get().waterColor);
                LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }));
    }
    public static boolean canRepair(ItemStack repairItem, ItemStack ingredient){
        return repairItem.is(ingredient.getItem()) && repairItem.getCount() == 1 && ingredient.getCount() == 1 && repairItem.getItem().canBeDepleted() && !repairItem.isEnchanted() && !ingredient.isEnchanted();
    }
    public static void dyeArmor(ItemStack itemStack, int color) {
        List<Integer> colors = new ArrayList<>();

        DyeableLeatherItem dyeableLeatherItem = null;
        Item item = itemStack.getItem();
        if (item instanceof DyeableLeatherItem) {
            dyeableLeatherItem = (DyeableLeatherItem) item;
            if (dyeableLeatherItem.hasCustomColor(itemStack)) colors.add(dyeableLeatherItem.getColor(itemStack));
            colors.add(color);
        }
        if (dyeableLeatherItem == null) return;
        dyeableLeatherItem.setColor(itemStack, mixColors(colors.iterator()));
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

    public static void preServerTick(MinecraftServer server) {
        SECURE_EXECUTOR.executeAll();
        if (!server.isDedicatedServer()) Legacy4JClient.preServerTick(server);
    }

    public static void serverSave(MinecraftServer server) {
        if (!server.isDedicatedServer()) Legacy4JClient.serverSave(server);
    }

    @FunctionalInterface
    public interface PackRegistry {
        void register(String path, String name, Component translation, Pack.Position position, boolean enabledByDefault);
        default void register(String path, String name, boolean enabledByDefault){
            register(path,name,Component.translatable(MOD_ID + ".builtin." + name), Pack.Position.TOP,enabledByDefault);
        }
        default void register(String pathName, boolean enabledByDefault){
            register("resourcepacks/"+pathName,pathName,enabledByDefault);
        }
    }
    public static void registerBuiltInPacks(PackRegistry registry){
        registry.register("legacy_waters",true);
        registry.register("console_aspects",false);
        if (Legacy4JPlatform.getLoader().isForgeLike()) {
            registry.register("programmer_art", "programmer_art", Component.translatable("legacy.builtin.console_programmer"), Pack.Position.TOP, false);
            registry.register("high_contrast", "high_contrast", Component.translatable("legacy.builtin.high_contrast"), Pack.Position.TOP, false);
        }
    }

    public static void onServerPlayerJoin(ServerPlayer p){
        if (p.getServer() == null) return;
        CommonNetwork.sendToPlayer(p, new ClientAdvancementsPacket(List.copyOf(p.getServer().getAdvancements().getAllAdvancements())));
        int pos = 0;
        boolean b = true;
        main : while (b) {
            b = false;
            for (ServerPlayer player : p.server.getPlayerList().getPlayers())
                if (player != p && ((LegacyPlayerInfo)player).getPosition() == pos){
                    pos++;
                    b = true;
                    continue main;
                }
        }
        ((LegacyPlayerInfo)p).setPosition(pos);
        CommonNetwork.sendToPlayer(p, new PlayerInfoSync.All(Map.of(p.getUUID(),(LegacyPlayerInfo)p), Collections.emptyMap(),p.server.getDefaultGameType()));
        if (!p.server.isDedicatedServer()) Legacy4JClient.serverPlayerJoin(p);
    }
    public static void copySaveToDirectory(InputStream stream, File directory){
        try (ZipInputStream inputStream = new ZipInputStream(stream))
        {
            ZipEntry zipEntry = inputStream.getNextEntry();
            byte[] buffer = new byte[1024];
            while (zipEntry != null)
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
                zipEntry = inputStream.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
