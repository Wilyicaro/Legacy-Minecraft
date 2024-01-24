package wily.legacy;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.server.LanServerPinger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import wily.legacy.client.*;
import wily.legacy.client.screen.*;
import wily.legacy.network.ServerDisplayInfoSync;
import wily.legacy.util.ScreenUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static wily.legacy.LegacyMinecraft.MOD_ID;


public class LegacyMinecraftClient {
    public static final ResourceLocation MAP_PLAYER_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"icon/map/player");
    public static final ResourceLocation SCROLL_DOWN = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_down");
    public static final ResourceLocation SCROLL_UP = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_up");
    public static final ResourceLocation SCROLL_RIGHT = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_right");
    public static final ResourceLocation SCROLL_LEFT = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_left");
    public static final ResourceLocation LOADING_BACKGROUND_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/loading_background");
    public static final ResourceLocation LOADING_BAR_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/loading_bar");
    public static final ResourceLocation SADDLE_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/saddle_slot");
    public static final ResourceLocation LLAMA_ARMOR_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/llama_armor_slot");
    public static final ResourceLocation ARMOR_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/armor_slot");
    public static final ResourceLocation EXPERIENCE_BAR_BACKGROUND_SPRITE = new ResourceLocation("hud/experience_bar_background");
    public static final ResourceLocation EXPERIENCE_BAR_CURRENT_SPRITE = new ResourceLocation("hud/experience_bar_progress");
    public static final ResourceLocation EXPERIENCE_BAR_RESULT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/experience_bar_result");
    public static final ResourceLocation PADLOCK_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/padlock");
    public static final ResourceLocation BEACON_1_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/beacon_1");
    public static final ResourceLocation BEACON_2_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/beacon_2");
    public static final ResourceLocation BEACON_3_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/beacon_3");
    public static final ResourceLocation BEACON_4_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/beacon_4");
    public static final ResourceLocation SHIELD_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/shield_slot");
    public static final ResourceLocation FULL_ARROW_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/full_arrow");
    public static final ResourceLocation SMALL_ARROW_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/small_arrow");
    public static final ResourceLocation LIT = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/lit");
    public static final ResourceLocation LIT_PROGRESS = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/lit_progress");
    public static final ResourceLocation BREWING_SLOTS_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/brewing_slots");
    public static final ResourceLocation BREWING_COIL_FLAME_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/brewing_coil_flame");
    public static final ResourceLocation BREWING_FUEL_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/brewing_fuel_slot");
    public static final ResourceLocation ERROR_CROSS_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/error_cross");
    public static final ResourceLocation ANVIL_HAMMER_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/anvil_hammer");
    public static final ResourceLocation SMITHING_HAMMER_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/smithing_hammer");
    public static final ResourceLocation COMBINER_PLUS_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/combiner_plus");
    public static final ResourceLocation ARROW_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/arrow");
    public static float FONT_SHADOW_OFFSET = 1.0F;
    public static boolean canLoadVanillaOptions = true;
    public static final Map<Component, Component> OPTION_BOOLEAN_CAPTION = Map.of(Component.translatable("key.sprint"),Component.translatable("options.key.toggleSprint"),Component.translatable("key.sneak"),Component.translatable("options.key.toggleSneak"));
    public static LegacyLoadingScreen legacyLoadingScreen = new LegacyLoadingScreen();
    public static RenderType itemRenderTypeOverride = null;
    public static RenderType blockItemRenderTypeOverride = null;
    public static final LegacyTipManager legacyTipManager = new LegacyTipManager();
    public static final LegacyCreativeTabListing.Manager legacyCreativeListingManager = new LegacyCreativeTabListing.Manager();
    public static final LegacyBiomeOverride.Manager legacyBiomeOverrides = new LegacyBiomeOverride.Manager();
    public static final LegacyWorldTemplate.Manager legacyWorldTemplateManager = new LegacyWorldTemplate.Manager();
    public static final LegacyTipOverride.Manager legacyTipOverridesManager = new LegacyTipOverride.Manager();
    public static KnownListing<Block> knownBlocks;
    public static KnownListing<EntityType<?>> knownEntities;
    public static GameType enterWorldGameType;

    public static int[] MAP_PLAYER_COLORS = new int[]{0xFFFFFF,0x00FF4C,0xFF2119,0x6385FF,0xFF63D9,0xFF9C00,0xFFFB19,0x63FFE4};

    public static float[] getVisualPlayerColor(int i){
        i = Math.abs(i);
        int baseColor = MAP_PLAYER_COLORS[i % MAP_PLAYER_COLORS.length];
        if (i < MAP_PLAYER_COLORS.length) return new float[]{(baseColor >> 16 & 255) / 255f,(baseColor >> 8 & 255) / 255f,(baseColor & 255) / 255f};
        float f = ((i - MAP_PLAYER_COLORS.length) % 101) / 250f;
        float r = ((baseColor >> 16 & 255) * (0.8f + f)) / 255f;
        float g = ((baseColor >> 8 & 255) * (0.8f +(f * 1.2f))) / 255f;
        float b = ((baseColor & 255) * (0.8f +(f / 2f))) / 255f;
        return new float[]{r,g,b};
    }
    public static int[] getVisualPlayerColor(String s){
        float[] c = getVisualPlayerColor(LegacyMinecraft.playerVisualIds.getOrDefault(s,s.hashCode()));
        return new int[]{(int) (c[0] * 255), (int) (c[1] * 255), (int) (c[2] * 255)};
    }


    public static final Map<Optional<ResourceKey<WorldPreset>>,PresetEditor> VANILLA_PRESET_EDITORS = new HashMap<>(Map.of(Optional.of(WorldPresets.FLAT), (createWorldScreen, settings) -> {
        ChunkGenerator chunkGenerator = settings.selectedDimensions().overworld();
        RegistryAccess.Frozen registryAccess =  settings.worldgenLoadContext();
        HolderLookup.RegistryLookup<Biome> biomeGetter = registryAccess.lookupOrThrow(Registries.BIOME);
        HolderLookup.RegistryLookup<StructureSet> structureGetter = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
        HolderLookup.RegistryLookup<PlacedFeature> placeFeatureGetter = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
        return new LegacyFlatWorldScreen(createWorldScreen, createWorldScreen.getUiState(),biomeGetter, structureGetter, flatLevelGeneratorSettings -> createWorldScreen.getUiState().updateDimensions(PresetEditor.flatWorldConfigurator(flatLevelGeneratorSettings)), chunkGenerator instanceof FlatLevelSource ? ((FlatLevelSource)chunkGenerator).settings() : FlatLevelGeneratorSettings.getDefault(biomeGetter, structureGetter, placeFeatureGetter));
    }, Optional.of(WorldPresets.SINGLE_BIOME_SURFACE), (createWorldScreen, settings) -> new LegacyBuffetWorldScreen(createWorldScreen, settings.worldgenLoadContext().lookupOrThrow(Registries.BIOME), holder -> createWorldScreen.getUiState().updateDimensions(PresetEditor.fixedBiomeConfigurator(holder)))));

    public static void init() {
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyTipManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyCreativeListingManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyWorldTemplateManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyTipOverridesManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyBiomeOverrides);
        KeyMappingRegistry.register(legacyKeyInventory);
        KeyMappingRegistry.register(keyHostOptions);
        knownBlocks = new KnownListing<>(Registries.BLOCK,Minecraft.getInstance().gameDirectory.toPath());
        knownEntities = new KnownListing<>(Registries.ENTITY_TYPE,Minecraft.getInstance().gameDirectory.toPath());
    }
    public static void onClientPlayerInfoChange(){
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.hasSingleplayerServer() && minecraft.level != null && minecraft.player != null)
            LegacyMinecraft.NETWORK.sendToServer(new ServerDisplayInfoSync(0));
        if (minecraft.screen instanceof HostOptionsScreen s) s.reloadPlayerButtons();
    }
    public static void enqueueInit() {
        ClientGuiEvent.SET_SCREEN.register((screen) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (screen instanceof TitleScreen)
                return CompoundEventResult.interruptTrue(new MainMenuScreen(false));
            if (screen instanceof JoinMultiplayerScreen)
                return CompoundEventResult.interruptTrue(new PlayGameScreen(new MainMenuScreen(false),2));
            if (screen instanceof DeathScreen d)
                return CompoundEventResult.interruptTrue(new LegacyDeathScreen(d.causeOfDeath,d.hardcore));
            if (((LegacyOptions)minecraft.options).legacyCreativeTab().get() && screen instanceof CreativeModeInventoryScreen c) {
                c.init(minecraft,0,0);
                return CompoundEventResult.interruptTrue(new CreativeModeScreen(Minecraft.getInstance().player));
            }
            if (screen instanceof AbstractContainerScreen<?>) ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
            return CompoundEventResult.interruptDefault(screen);
        });
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (legacyKeyInventory.consumeClick()) {
                if (minecraft.gameMode.isServerControlledInventory()) {
                    minecraft.player.sendOpenInventory();
                    continue;
                }
                minecraft.getTutorial().onOpenInventory();
                InventoryScreen inventoryScreen = new InventoryScreen(minecraft.player);
                ((ReplaceableScreen)inventoryScreen).setCanReplace(false);
                minecraft.setScreen(inventoryScreen);
            }
            while (keyHostOptions.consumeClick()) {
                minecraft.setScreen(new HostOptionsScreen());
            }
        });
        ClientLifecycleEvent.CLIENT_STOPPING.register(p-> {
            knownBlocks.save();
            knownEntities.save();
        });
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(p-> {
            Minecraft minecraft = Minecraft.getInstance();
        if (enterWorldGameType != null && minecraft.hasSingleplayerServer()){
            minecraft.getSingleplayerServer().getPlayerList().getPlayer(p.getUUID()).setGameMode(enterWorldGameType);
            enterWorldGameType = null;
        }
        });
        ClientTickEvent.CLIENT_LEVEL_POST.register((level)->{
            Minecraft minecraft = Minecraft.getInstance();
            if (level != null && minecraft.screen == null && ((LegacyOptions)minecraft.options).hints().get()) {
                HitResult hit = minecraft.hitResult;
                if (hit instanceof BlockHitResult blockHitResult) {
                    BlockState state = level.getBlockState(blockHitResult.getBlockPos());
                    if (!state.isAir()) {
                        if (!knownBlocks.contains(state.getBlock())) ScreenUtil.addTip(state.getBlock().asItem().getDefaultInstance());
                        knownBlocks.add(state.getBlock());

                    }
                } else if (hit instanceof EntityHitResult r){
                    Entity e = r.getEntity();
                    if (!knownEntities.contains(e.getType())) ScreenUtil.addTip(e);
                    knownEntities.add(e.getType());
                }
            }
        });

    }

    public static final KeyMapping legacyKeyInventory = new KeyMapping( MOD_ID +".key.inventory", InputConstants.KEY_I, "key.categories.inventory");
    public static final KeyMapping keyHostOptions = new KeyMapping( MOD_ID +".key.host_options", InputConstants.KEY_H, "key.categories.misc");
    public static void resetVanillaOptions(Minecraft minecraft){
        canLoadVanillaOptions = false;
        minecraft.options = new Options(minecraft,minecraft.gameDirectory);
        minecraft.options.save();
        canLoadVanillaOptions = true;
    }
    public static String importSaveFile(Minecraft minecraft, InputStream saveInputStream, String saveDirName){
        StringBuilder builder = new StringBuilder(saveDirName);
        int levelRepeat = 0;
        while (minecraft.getLevelSource().levelExists(builder +(levelRepeat > 0 ? String.format(" (%s)",levelRepeat) : "")))
            levelRepeat++;
        if (levelRepeat > 0)
            builder.append(String.format(" (%s)",levelRepeat));
        LegacyMinecraft.copySaveToDirectory(saveInputStream,new File(minecraft.gameDirectory, "saves/" + builder));
        return builder.toString();
    }
    public static void registerExtraModels(Consumer<ResourceLocation> register){

    }
    public static Pair<Integer,Component> tryParsePort(String string) {
        if (string.isBlank())
            return Pair.of(HttpUtil.getAvailablePort(),null);
        try {
            int port = Integer.parseInt(string);
            if (port < 1024 || port > 65535) {
                return Pair.of(port,Component.translatable("lanServer.port.invalid.new", 1024, 65535));
            }
            if (!HttpUtil.isPortAvailable(port)) {
                Pair.of(port,Component.translatable("lanServer.port.unavailable.new", 1024, 65535));
            }
            return Pair.of(null,null);
        } catch (NumberFormatException numberFormatException) {
            return  Pair.of(HttpUtil.getAvailablePort(),Component.translatable("lanServer.port.invalid.new", 1024, 65535));
        }
    }
    public static boolean publishUnloadedServer(Minecraft minecraft, @Nullable GameType gameType, boolean bl, int i) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        try {
            minecraft.prepareForMultiplayer();
            minecraft.getProfileKeyPairManager().prepareKeyPair().thenAcceptAsync((optional) -> {
                optional.ifPresent((profileKeyPair) -> {
                    ClientPacketListener clientPacketListener = minecraft.getConnection();
                    if (clientPacketListener != null) {
                        clientPacketListener.setKeyPair(profileKeyPair);
                    }

                });
            }, minecraft);
            server.getConnection().startTcpServerListener(null, i);
            LegacyMinecraft.LOGGER.info("Started serving on {}", i);
            server.publishedPort = i;
            server.lanPinger = new LanServerPinger(server.getMotd(), "" + i);
            server.lanPinger.start();
            server.publishedGameType = gameType;
            server.getPlayerList().setAllowCheatsForAllPlayers(bl);
            return true;
        } catch (IOException var7) {
            return false;
        }
    }

}