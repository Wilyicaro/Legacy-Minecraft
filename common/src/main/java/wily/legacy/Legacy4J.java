package wily.legacy;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
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
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.init.*;
import wily.legacy.inventory.LegacyIngredient;
import wily.legacy.network.*;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.ArmorStandPose;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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
    public static <T> Map<String, Field> getAccessibleFieldsMap(Class<T> fieldsClass, String... fields){
        Map<String,Field> map = new HashMap<>();
        for (String s : fields) {
            try {
                Field field = fieldsClass.getDeclaredField(s);
                field.setAccessible(true);
                map.put(s, field);
            } catch (NoSuchFieldException var1) {
                throw new IllegalStateException("Couldn't get field %s for %s".formatted(s, fieldsClass), var1);
            }
        }
        return map;
    }
    public static void setup(){
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
            level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get()).ifPresent(be->{
                be.potion = p.potion().get();
                be.setChanged();
            });
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        };
        emptyCauldron.put(Items.POTION,emptyCauldronPotion);
        emptyCauldron.put(Items.SPLASH_POTION,emptyCauldronPotion);
        emptyCauldron.put(Items.LINGERING_POTION,emptyCauldronPotion);
        waterCauldron.put(Items.GLASS_BOTTLE, (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get());
            if (opt.map(be->be.waterColor).orElse(null) != null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) {
                Item item = itemStack.getItem();
                player.setItemInHand(interactionHand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.POTION.builtInRegistryHolder(),1, DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(opt.map(be->be.potion).orElse(Potions.WATER))).build())));
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
            Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get());
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
                itemStack.consume(arrowCount,player);
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
                Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get());
                if (!(itemStack.getItem() instanceof DyeItem) || opt.isEmpty() || opt.get().potion != Potions.WATER) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                int dyeColor = color.getTextureDiffuseColor();
                if (opt.get().waterColor == null) opt.get().waterColor = dyeColor;
                else opt.get().waterColor = Legacy4J.mixColors(List.of(opt.get().waterColor,dyeColor).iterator());
                opt.get().setChanged();
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            });
        }
        BuiltInRegistries.ITEM.holders().filter(i-> i.is(ItemTags.DYEABLE)).forEach(i-> waterCauldron.put(i.value(),(blockState, level, blockPos, player, interactionHand, itemStack) -> {
            Optional<WaterCauldronBlockEntity> opt = level.getBlockEntity(blockPos,LegacyRegistries.WATER_CAULDRON_BLOCK_ENTITY.get());
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
        DispenserBlock.registerBehavior(Blocks.TNT, new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
                setSuccess(blockSource.level().getGameRules().getBoolean(LegacyGameRules.TNT_EXPLODES));
                if (isSuccess()){
                    TntBlock.explode(blockSource.level(),blockSource.pos());
                    blockSource.level().gameEvent(null, GameEvent.ENTITY_PLACE, blockSource.pos());
                    itemStack.shrink(1);
                }
                return itemStack;
            }

            @Override
            protected void playAnimation(BlockSource blockSource, Direction direction) {
                if (isSuccess()) super.playAnimation(blockSource, direction);
            }
        });
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
        return repairItem.is(ingredient.getItem()) && repairItem.getCount() == 1 && ingredient.getCount() == 1 && repairItem.getItem().components().has(DataComponents.DAMAGE) && !repairItem.isEnchanted() && !ingredient.isEnchanted();
    }
    public static ItemStack dyeItem(ItemStack itemStack, int color) {
        List<Integer> colors = new ArrayList<>();
        DyedItemColor dyedItemColor = itemStack.get(DataComponents.DYED_COLOR);
        boolean bl = dyedItemColor == null || dyedItemColor.showInTooltip();
        if (dyedItemColor != null) colors.add(color);
        colors.add(color);
        itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(mixColors(colors.iterator()), bl));
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
    public interface ComponentsBuilderModifier{
        <T> void set(DataComponentType<T> type, T value);
    }
    public static void changeItemDefaultComponents(BiConsumer<Item, Consumer<ComponentsBuilderModifier>> set) {
        set.accept(Items.CAKE,b-> b.set(DataComponents.MAX_STACK_SIZE,64));
    }
    public static void onServerPlayerJoin(ServerPlayer p){
        if (p.getServer() == null) return;
        CommonNetwork.sendToPlayer(p, new ClientAdvancementsPacket(List.copyOf(p.getServer().getAdvancements().getAllAdvancements())));
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
        CommonNetwork.sendToPlayer(p, new PlayerInfoSync.All(Map.of(p.getUUID(),(LegacyPlayerInfo)p), Collections.emptyMap(),p.server.getDefaultGameType()));
        if (!p.server.isDedicatedServer()) Legacy4JClient.serverPlayerJoin(p);
    }
    public static void copySaveToDirectory(InputStream stream, File directory){
        if (directory.exists()) FileUtils.deleteQuietly(directory);
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
