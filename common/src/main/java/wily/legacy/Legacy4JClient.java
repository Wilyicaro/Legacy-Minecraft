package wily.legacy;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.server.LanServerPinger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.LiquidBlock;
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
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.client.*;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.screen.*;
import wily.legacy.init.LegacyMenuTypes;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.ServerOpenClientMenuPacket;
import wily.legacy.player.LegacyPlayerInfo;
import wily.legacy.util.ScreenUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static wily.legacy.Legacy4J.MOD_ID;
import static wily.legacy.init.LegacyRegistries.SHRUB;


public class Legacy4JClient {
    public static boolean legacyFont = true;
    public static boolean canLoadVanillaOptions = true;
    public static boolean manualSave = false;
    public static boolean retakeWorldIcon = false;
    public static boolean deleteLevelWhenExitWithoutSaving = false;
    public static final Map<Component, Component> OPTION_BOOLEAN_CAPTION = Map.of(Component.translatable("key.sprint"),Component.translatable("options.key.toggleSprint"),Component.translatable("key.sneak"),Component.translatable("options.key.toggleSneak"));
    public static LegacyLoadingScreen legacyLoadingScreen = new LegacyLoadingScreen();
    public static MultiBufferSource.BufferSource guiBufferSourceOverride = null;
    public static final LegacyTipManager legacyTipManager = new LegacyTipManager();
    public static final LegacyCreativeTabListing.Manager legacyCreativeListingManager = new LegacyCreativeTabListing.Manager();
    public static final LegacyCraftingTabListing.Manager legacyCraftingListingManager = new LegacyCraftingTabListing.Manager();
    public static final LegacyBiomeOverride.Manager legacyBiomeOverrides = new LegacyBiomeOverride.Manager();
    public static final LegacyWorldTemplate.Manager legacyWorldTemplateManager = new LegacyWorldTemplate.Manager();
    public static final LegacyTipOverride.Manager legacyTipOverridesManager = new LegacyTipOverride.Manager();
    public static final LegacyResourceManager legacyResourceManager = new LegacyResourceManager();
    public static final StoneCuttingGroupManager stoneCuttingGroupManager = new StoneCuttingGroupManager();
    public static final LoomTabListing.Manager loomListingManager = new LoomTabListing.Manager();
    public static final ControlTooltip.GuiManager controlTooltipGuiManager = new ControlTooltip.GuiManager();
    public static final LeaderboardsScreen.Manager leaderBoardListingManager = new LeaderboardsScreen.Manager();

    public static ControllerManager controllerManager;
    public static KnownListing<Block> knownBlocks;
    public static KnownListing<EntityType<?>> knownEntities;
    public static GameType enterWorldGameType;

    private static int advancementKeyHold = 0;
    public static PostChain gammaEffect;
    public static int[] MAP_PLAYER_COLORS = new int[]{0xFFFFFF,0x00FF4C,0xFF2119,0x6385FF,0xFF63D9,0xFF9C00,0xFFFB19,0x63FFE4};
    public static float[] getVisualPlayerColor(LegacyPlayerInfo info){
        return getVisualPlayerColor(info.getPosition() >= 0 ? info.getPosition() : info.legacyMinecraft$getProfile().getId().hashCode());
    }
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
    public static void updateLegacyPlayerInfos(Map<UUID, LegacyPlayerInfo> map){
        Minecraft minecraft = Minecraft.getInstance();
        map.forEach((s,i)->{
            if (minecraft.getConnection() != null && minecraft.getConnection().getPlayerInfo(s) instanceof LegacyPlayerInfo info)
                info.copyFrom(i);
        });
        LeaderboardsScreen.refreshStatsBoards(minecraft);
        if (minecraft.screen instanceof LeaderboardsScreen s && LeaderboardsScreen.statsBoards.get(s.selectedStatBoard).statsList.isEmpty()) minecraft.executeIfPossible(()-> s.changeStatBoard(false));
    }


    public static final Map<Optional<ResourceKey<WorldPreset>>,PresetEditor> VANILLA_PRESET_EDITORS = new HashMap<>(Map.of(Optional.of(WorldPresets.FLAT), (createWorldScreen, settings) -> {
        ChunkGenerator chunkGenerator = settings.selectedDimensions().overworld();
        RegistryAccess.Frozen registryAccess =  settings.worldgenLoadContext();
        HolderLookup.RegistryLookup<Biome> biomeGetter = registryAccess.lookupOrThrow(Registries.BIOME);
        HolderLookup.RegistryLookup<StructureSet> structureGetter = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
        HolderLookup.RegistryLookup<PlacedFeature> placeFeatureGetter = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
        return new LegacyFlatWorldScreen(createWorldScreen, createWorldScreen.getUiState(),biomeGetter, structureGetter, flatLevelGeneratorSettings -> createWorldScreen.getUiState().updateDimensions(PresetEditor.flatWorldConfigurator(flatLevelGeneratorSettings)), chunkGenerator instanceof FlatLevelSource ? ((FlatLevelSource)chunkGenerator).settings() : FlatLevelGeneratorSettings.getDefault(biomeGetter, structureGetter, placeFeatureGetter));
    }, Optional.of(WorldPresets.SINGLE_BIOME_SURFACE), (createWorldScreen, settings) -> new LegacyBuffetWorldScreen(createWorldScreen, settings.worldgenLoadContext().lookupOrThrow(Registries.BIOME), holder -> createWorldScreen.getUiState().updateDimensions(PresetEditor.fixedBiomeConfigurator(holder)))));

    public static Screen getReplacementScreen(Screen screen){
        if (screen instanceof TitleScreen)
            return new MainMenuScreen();
        else if (screen instanceof JoinMultiplayerScreen)
            return new PlayGameScreen(new MainMenuScreen(),2);
        else if (screen instanceof DisconnectedScreen s)
            return ConfirmationScreen.createInfoScreen(getReplacementScreen(s.parent), s.getTitle(),s.reason);
        else if (screen instanceof AlertScreen s) {
            MultiLineLabel messageLines = MultiLineLabel.create(Minecraft.getInstance().font,s.messageText,200);
            return new ConfirmationScreen(Minecraft.getInstance().screen, 230, 97 + messageLines.getLineCount() * 12, s.getTitle(), messageLines, b -> true) {
                protected void initButtons() {
                    okButton = addRenderableWidget(Button.builder(Component.translatable("gui.ok"), b -> s.callback.run()).bounds(panel.x + 15, panel.y + panel.height - 30, 200, 20).build());
                }
                public boolean shouldCloseOnEsc() {
                    return s.shouldCloseOnEsc();
                }
            };
        }else if (screen instanceof BackupConfirmScreen s) {
            MultiLineLabel messageLines = MultiLineLabel.create(Minecraft.getInstance().font,s.description,200);
            return new ConfirmationScreen(Minecraft.getInstance().screen, 230, 141 + messageLines.getLineCount() * 12 + (s.promptForCacheErase ? 14 : 0), s.getTitle(), messageLines, b -> true) {
                boolean eraseCache = false;
                protected void initButtons() {
                    if (s.promptForCacheErase) addRenderableWidget(new TickBox(panel.x + 15, panel.y + panel.height - 88,eraseCache,b->Component.translatable("selectWorld.backupEraseCache"),b->null,b-> eraseCache = b.selected));
                    okButton = addRenderableWidget(Button.builder(Component.translatable("selectWorld.backupJoinConfirmButton"), b -> s.onProceed.proceed(true, eraseCache)).bounds(panel.x + 15, panel.y + panel.height - 74, 200, 20).build());
                    addRenderableWidget(Button.builder(Component.translatable("selectWorld.backupJoinSkipButton"), b -> s.onProceed.proceed(false, eraseCache)).bounds(panel.x + 15, panel.y + panel.height - 52, 200, 20).build());
                    addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> s.onCancel.run()).bounds(panel.x + 15, panel.y + panel.height - 30, 200, 20).build());
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if (s.keyPressed(i,j,k)) return true;
                    return super.keyPressed(i, j, k);
                }

                public boolean shouldCloseOnEsc() {
                    return s.shouldCloseOnEsc();
                }
            };
        }
        return screen;
    }
    public static void init() {
        controllerManager = new ControllerManager(Minecraft.getInstance());
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyTipManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyCreativeListingManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyCraftingListingManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyWorldTemplateManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyTipOverridesManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyBiomeOverrides);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, legacyResourceManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, stoneCuttingGroupManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, loomListingManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, controlTooltipGuiManager);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, leaderBoardListingManager);


        KeyMappingRegistry.register(keyCrafting);
        KeyMappingRegistry.register(keyHostOptions);
        KeyMappingRegistry.register(keyCycleHeldLeft);
        KeyMappingRegistry.register(keyCycleHeldRight);
        KeyMappingRegistry.register(keyToggleCursor);
        KeyMappingRegistry.register(keyFlyUp);
        KeyMappingRegistry.register(keyFlyDown);
        KeyMappingRegistry.register(keyFlyLeft);
        KeyMappingRegistry.register(keyFlyRight);
        knownBlocks = new KnownListing<>(Registries.BLOCK,Minecraft.getInstance().gameDirectory.toPath());
        knownEntities = new KnownListing<>(Registries.ENTITY_TYPE,Minecraft.getInstance().gameDirectory.toPath());

        LifecycleEvent.SERVER_LEVEL_SAVE.register(l-> deleteLevelWhenExitWithoutSaving = false);

        TickEvent.SERVER_LEVEL_PRE.register(l-> l.noSave = !ScreenUtil.getLegacyOptions().autoSave().get());
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(p-> Legacy4J.NETWORK.sendToServer(new PlayerInfoSync(ScreenUtil.hasClassicCrafting() ? 1 : 2,p)));
        ClientGuiEvent.SET_SCREEN.register((screen) -> {
            Screen replacement = getReplacementScreen(screen);
            if (replacement != screen) return CompoundEventResult.interruptTrue(replacement);
            if (Minecraft.getInstance().screen == null && Minecraft.getInstance().level != null && screen != null && (screen instanceof PauseScreen || !screen.isPauseScreen()) ) ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
            return CompoundEventResult.interruptDefault(screen);
        });
        ClientGuiEvent.INIT_POST.register((screen,access) -> {
            if (screen.getFocused() != null && !screen.children().contains(screen.getFocused())){
                screen.clearFocus();
            }
            if ((controllerManager.connectedController != null) && !controllerManager.forceEnableCursor) {
                if (!(screen instanceof MenuAccess<?>) || screen.children().stream().anyMatch(g-> g instanceof LegacyIconHolder)) controllerManager.disableCursor();
                if (screen.getFocused() == null || !screen.getFocused().isFocused()) {
                    ComponentPath path = screen.nextFocusPath(new FocusNavigationEvent.ArrowNavigation(ScreenDirection.DOWN));
                    if (path != null) path.applyFocus(true);
                }
                controllerManager.resetCursor();
            }
        });
        ClientLifecycleEvent.CLIENT_STARTED.register(m-> controllerManager.setup());
        ClientTickEvent.CLIENT_PRE.register(minecraft -> {
            while (keyCrafting.consumeClick()){
                AdvancementToast toast = minecraft.getToasts().getToast(AdvancementToast.class, Toast.NO_TOKEN);
                if (toast != null){
                    advancementKeyHold++;
                    if (advancementKeyHold == 10){
                        minecraft.getToasts().clear();
                        advancementKeyHold = 0;
                        ControllerBinding.LEFT_BUTTON.bindingState.block();
                        minecraft.setScreen(new LegacyAdvancementsScreen(null,minecraft.getConnection().getAdvancements()));
                    }
                    continue;
                }
                if (minecraft.gameMode != null && minecraft.gameMode.hasInfiniteItems()) {
                    minecraft.setScreen(CreativeModeScreen.getActualCreativeScreenInstance(minecraft));
                    continue;
                }
                if (ScreenUtil.hasClassicCrafting() || !isModEnabledOnServer()) {
                    if (!isModEnabledOnServer()) ScreenUtil.getLegacyOptions().classicCrafting().set(true);
                    minecraft.getTutorial().onOpenInventory();
                    minecraft.setScreen(new InventoryScreen(minecraft.player));
                }else Legacy4J.NETWORK.sendToServer(minecraft.hitResult != null && minecraft.hitResult instanceof BlockHitResult r && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof CraftingTableBlock ?
                new ServerOpenClientMenuPacket(r.getBlockPos(),0) : new ServerOpenClientMenuPacket(1));
            }
            while (keyHostOptions.consumeClick()) {
                minecraft.setScreen(new HostOptionsScreen());
            }
            boolean left;
            while ((left=keyCycleHeldLeft.consumeClick()) || keyCycleHeldRight.consumeClick()){
                if (minecraft.player != null)  minecraft.player.getInventory().swapPaint(left ? 1 : -1);
            }

        });
        ClientLifecycleEvent.CLIENT_STOPPING.register(p-> {
            knownBlocks.save();
            knownEntities.save();
        });
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(p-> {
            Minecraft minecraft = Minecraft.getInstance();
            LegacyCreativeTabListing.rebuildVanillaCreativeTabsItems(minecraft);
            if (enterWorldGameType != null && minecraft.hasSingleplayerServer()){
                minecraft.getSingleplayerServer().getPlayerList().getPlayer(p.getUUID()).setGameMode(enterWorldGameType);
                enterWorldGameType = null;
            }
            if (minecraft.getConnection().getPlayerInfo(p.getUUID()) instanceof LegacyPlayerInfo i && i.getPosition() < 0) p.sendSystemMessage(Component.literal("You're using Legacy4J in a server without the mod, be aware that you may be banned if the server does not allow changes to the client, even if you disable Smooth Movement of the controller, you still have this risk."));
        });
        ClientTickEvent.CLIENT_LEVEL_POST.register((level)->{
            Minecraft minecraft = Minecraft.getInstance();
            if (level != null && minecraft.screen == null && ((LegacyOptions)minecraft.options).hints().get()) {
                HitResult hit = minecraft.hitResult;
                if (hit instanceof BlockHitResult blockHitResult) {
                    BlockState state = level.getBlockState(blockHitResult.getBlockPos());
                    if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock) && state.getBlock().asItem() instanceof BlockItem) {
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
    public static boolean isModEnabledOnServer(){
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null && minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID()) instanceof LegacyPlayerInfo i && i.getPosition() >= 0;
    }
    public static void onClientPlayerInfoChange(){
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null)
            Legacy4J.NETWORK.sendToServer(new PlayerInfoSync(0, minecraft.player));
        if (minecraft.screen instanceof HostOptionsScreen s) s.reloadPlayerButtons();
        else if (minecraft.screen instanceof LeaderboardsScreen s){
            s.rebuildRenderableVList(minecraft);
            s.repositionElements();
        }
    }
    public static void enqueueInit() {
        MenuRegistry.registerScreenFactory(LegacyMenuTypes.CRAFTING_PANEL_MENU.get(),LegacyCraftingScreen::craftingScreen);
        MenuRegistry.registerScreenFactory(LegacyMenuTypes.PLAYER_CRAFTING_PANEL_MENU.get(),LegacyCraftingScreen::playerCraftingScreen);
        MenuRegistry.registerScreenFactory(LegacyMenuTypes.LOOM_PANEL_MENU.get(),LegacyLoomScreen::new);
        MenuRegistry.registerScreenFactory(LegacyMenuTypes.STONECUTTER_PANEL_MENU.get(),LegacyStonecutterScreen::new);
        MenuRegistry.registerScreenFactory(LegacyMenuTypes.MERCHANT_MENU.get(),LegacyMerchantScreen::new);
        ColorHandlerRegistry.registerBlockColors((blockState, blockAndTintGetter, blockPos, i) -> blockAndTintGetter == null || blockPos == null ? GrassColor.getDefaultColor() : BiomeColors.getAverageGrassColor(blockAndTintGetter, blockPos),SHRUB.get());
        ColorHandlerRegistry.registerBlockColors((blockState, blockAndTintGetter, blockPos, i) -> {
            if (blockAndTintGetter != null && blockPos != null){
                if (blockAndTintGetter.getBlockEntity(blockPos) instanceof WaterCauldronBlockEntity be){
                    if (be.potion != Potions.WATER) return PotionUtils.getColor(be.potion);
                    else if (be.waterColor != null) return be.waterColor;
                }
                return BiomeColors.getAverageWaterColor(blockAndTintGetter, blockPos);
            }
            return -1;
        }, Blocks.WATER_CAULDRON);
        ColorHandlerRegistry.registerItemColors((item,i) ->0xFF3153AF, LegacyRegistries.WATER.get());
        ColorHandlerRegistry.registerItemColors((itemStack, i) -> GrassColor.getDefaultColor(),SHRUB.get().asItem());
        RenderTypeRegistry.register(RenderType.cutoutMipped(),SHRUB.get());
        RenderTypeRegistry.register(RenderType.translucent(),Blocks.WATER);
    }
    public static final KeyMapping keyCrafting = new KeyMapping("legacy.key.crafting", InputConstants.KEY_E, "key.categories.inventory");
    public static final KeyMapping keyCycleHeldLeft = new KeyMapping("legacy.key.cycleHeldLeft", InputConstants.KEY_PAGEDOWN, "key.categories.inventory");
    public static final KeyMapping keyCycleHeldRight = new KeyMapping("legacy.key.cycleHeldRight", InputConstants.KEY_PAGEUP, "key.categories.inventory");
    public static final KeyMapping keyToggleCursor = new KeyMapping("legacy.key.toggleCursor", -1, "key.categories.misc");
    public static KeyMapping keyHostOptions = new KeyMapping( MOD_ID +".key.host_options", InputConstants.KEY_H, "key.categories.misc");
    public static KeyMapping keyFlyUp = new KeyMapping( MOD_ID +".key.flyUp", InputConstants.KEY_UP, "key.categories.movement");
    public static KeyMapping keyFlyDown = new KeyMapping( MOD_ID +".key.flyDown", InputConstants.KEY_DOWN, "key.categories.movement");
    public static KeyMapping keyFlyLeft = new KeyMapping( MOD_ID +".key.flyLeft", InputConstants.KEY_LEFT, "key.categories.movement");
    public static KeyMapping keyFlyRight = new KeyMapping( MOD_ID +".key.flyRight", InputConstants.KEY_RIGHT, "key.categories.movement");
    public static void resetVanillaOptions(Minecraft minecraft){
        canLoadVanillaOptions = false;
        minecraft.options = new Options(minecraft,minecraft.gameDirectory);
        minecraft.options.save();
        canLoadVanillaOptions = true;
    }
    public static String manageAvailableSaveDirName(Minecraft minecraft, Consumer<File> copy, String saveDirName){
        StringBuilder builder = new StringBuilder(saveDirName);
        int levelRepeat = 0;
        while (minecraft.getLevelSource().levelExists(builder +(levelRepeat > 0 ? String.format(" (%s)",levelRepeat) : "")))
            levelRepeat++;
        if (levelRepeat > 0)
            builder.append(String.format(" (%s)",levelRepeat));
        copy.accept(new File(minecraft.gameDirectory, "saves/" + builder));
        return builder.toString();
    }
    public static String importSaveFile(Minecraft minecraft, InputStream saveInputStream, String saveDirName){
        return manageAvailableSaveDirName(minecraft, f-> Legacy4J.copySaveToDirectory(saveInputStream,f),saveDirName);
    }
    public static String copySaveFile(Minecraft minecraft, Path savePath, String saveDirName){
        return manageAvailableSaveDirName(minecraft, f-> {
            try {
                FileUtils.copyDirectory(savePath.toFile(),f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        },saveDirName);
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
                return Pair.of(port,Component.translatable("lanServer.port.unavailable.new", 1024, 65535));
            }
            return Pair.of(port,null);
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
            Legacy4J.LOGGER.info("Started serving on {}", i);
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