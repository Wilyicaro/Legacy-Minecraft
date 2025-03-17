package wily.legacy;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.lwjgl.glfw.GLFW;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.*;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.*;
*///?}
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.GhastRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.client.gui.screens.options.OptionsScreen;
//?} else {
/*import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.client.gui.screens.OptionsScreen;
*///?}
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.controller.ControllerBinding;
//? if fabric {
import wily.legacy.client.screen.compat.ModMenuCompat;
//?} else if forge {
/*import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RegisterPresetEditorsEvent;
*///?} else if neoforge {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;
*///?}
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.*;
import org.apache.commons.io.FileUtils;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.FactoryGuiElement;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.client.*;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.client.screen.*;
//? if fabric || >=1.21 && neoforge {
import wily.legacy.client.screen.compat.IrisCompat;
import wily.legacy.client.screen.compat.SodiumCompat;
//?}
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.init.LegacyUIElementTypes;
import wily.legacy.inventory.LegacyPistonMovingBlockEntity;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.network.ServerOpenClientMenuPayload;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.network.TopMessage;
import wily.legacy.util.MCAccount;
import wily.legacy.util.ScreenUtil;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static wily.legacy.Legacy4J.MOD_ID;
import static wily.legacy.client.LegacyResourceManager.INTROS;
import static wily.legacy.init.LegacyRegistries.SHRUB;


public class Legacy4JClient {

    public static boolean isNewerVersion = false;
    public static boolean isNewerMinecraftVersion = false;
    public static final List<Runnable> whenResetOptions = new ArrayList<>();
    public static LevelStorageSource currentWorldSource;
    public static boolean legacyFont = true;
    public static boolean forceVanillaFontShadowColor = false;
    public static ResourceLocation defaultFontOverride = null;
    public static ControlType lastControlType;
    public static boolean manualSave = false;
    public static boolean saveExit = false;
    public static boolean retakeWorldIcon = false;
    public static LegacyLoadingScreen legacyLoadingScreen = new LegacyLoadingScreen();
    public static Renderable itemActivationRenderReplacement = null;
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
    public static final HowToPlayScreen.Manager howToPlaySectionManager = new HowToPlayScreen.Manager();

    public static final ControllerManager controllerManager = new ControllerManager(Minecraft.getInstance());
    public static final Map<Block, ModelResourceLocation> fastLeavesModels = new HashMap<>();
    public static KnownListing<Block> knownBlocks;
    public static KnownListing<EntityType<?>> knownEntities;
    public static GameType defaultServerGameType;
    public static GameRules gameRules;
    public static Consumer<ServerPlayer> serverPlayerJoinConsumer;
    private static final List<ItemStack> lastArmorSlots = new ArrayList<>();
    public static final FactoryConfig.StorageHandler MIXIN_CONFIGS_STORAGE = FactoryConfig.StorageHandler.fromMixin(LegacyMixinOptions.CLIENT_MIXIN_STORAGE, false);
    //? if <1.21.2 {
    public static PostChain gammaEffect;
    //?}

    public static final RenderType GHAST_SHOOTING_GLOW = RenderType.eyes(FactoryAPI.createVanillaLocation("textures/entity/ghast/ghast_shooting_glow.png"));
    public static final RenderType DROWNED_GLOW = RenderType.eyes(FactoryAPI.createVanillaLocation("textures/entity/zombie/drowned_glow.png"));

    public static float[] getVisualPlayerColor(LegacyPlayerInfo info){
        return getVisualPlayerColor(info.getIdentifierIndex() >= 0 ? info.getIdentifierIndex() : info.legacyMinecraft$getProfile().getId().hashCode());
    }

    public static PostChain getGammaEffect(){
        //? if <1.21.2 {
        return gammaEffect;
        //?} else {
        /*return Minecraft.getInstance().getShaderManager().getPostChain(LegacyResourceManager.GAMMA_LOCATION, LevelTargetBundle.MAIN_TARGETS);
        *///?}
    }

    public static float[] getVisualPlayerColor(int i){
        PlayerIdentifier playerIdentifier = PlayerIdentifier.of(i);
        if (PlayerIdentifier.list.containsKey(i)) return new float[]{(playerIdentifier.color() >> 16 & 255) / 255f,(playerIdentifier.color() >> 8 & 255) / 255f,(playerIdentifier.color() & 255) / 255f};
        float r = ((playerIdentifier.color() >> 16 & 255) * (0.8f + (i%15) /30f)) / 255f;
        float g = ((playerIdentifier.color() >> 8 & 255) * (1.2f - (i%16) /32f)) / 255f;
        float b = ((playerIdentifier.color() & 255) * (0.8f + (i%17) /34f)) / 255f;
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
        if (minecraft.player != null) {
            LegacyOptions.classicCrafting.set(LegacyOptions.classicCrafting.get());
            LegacyOptions.classicTrading.set(LegacyOptions.classicTrading.get());
            LegacyOptions.classicStonecutting.set(LegacyOptions.classicStonecutting.get());
            LegacyOptions.classicLoom.set(LegacyOptions.classicLoom.get());
        }
    }

    public static void displayActivationAnimation(Renderable renderable){
        itemActivationRenderReplacement = renderable;
        Minecraft.getInstance().gameRenderer.displayItemActivation(ItemStack.EMPTY);
    }

    public static void applyFontOverrideIf(boolean b, ResourceLocation override, Consumer<Boolean> fontRender){
        if (b) defaultFontOverride = override;
        fontRender.accept(b);
        if (b) defaultFontOverride = null;
    }

    public static LevelStorageSource getLevelStorageSource(){
        return LegacyOptions.saveCache.get() ? currentWorldSource : Minecraft.getInstance().getLevelSource();
    }

    public static boolean hasSaveSystem(Minecraft minecraft){
        return minecraft.hasSingleplayerServer() && !minecraft.isDemo() && !minecraft.getSingleplayerServer().isHardcore() && isCurrentWorldSource(minecraft.getSingleplayerServer().storageSource);
    }

    public static boolean isCurrentWorldSource(LevelStorageSource.LevelStorageAccess storageSource){
        return storageSource.getDimensionPath(Level.OVERWORLD).getParent().equals(Legacy4JClient.currentWorldSource.getBaseDir());
    }

    public static void saveLevel(LevelStorageSource.LevelStorageAccess storageSource){
        if (isCurrentWorldSource(storageSource)) Legacy4JClient.copySaveBtwSources(storageSource,Minecraft.getInstance().getLevelSource());
    }

    public static void displayEffectActivationAnimation(/*? if <1.20.5 {*//*MobEffect*//*?} else {*/Holder<MobEffect>/*?}*/ effect){
        displayActivationAnimation(((guiGraphics, i, j, f) -> {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.5f,0.5f,0.5f);
            FactoryGuiGraphics.of(guiGraphics).blit(0, 1, 0, 1, -1, Minecraft.getInstance().getMobEffectTextures().get(effect));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.5f,0.5f,0);
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180));
            guiGraphics.pose().translate(-0.5f,-0.5f,0);
            FactoryGuiGraphics.of(guiGraphics).blit(0, 0, 0, 1, 1, Minecraft.getInstance().getMobEffectTextures().get(effect));
            guiGraphics.pose().popPose();
            guiGraphics.pose().popPose();
        }));
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
        if (!LegacyMixinOptions.legacyTitleScreen.get()) return screen;
        if (screen instanceof JoinMultiplayerScreen)
            return new PlayGameScreen(new TitleScreen(),2);
        else if (screen instanceof DisconnectedScreen s)
            return ConfirmationScreen.createInfoScreen(getReplacementScreen(DisconnectedScreenAccessor.of(s).getParent()), s.getTitle(),DisconnectedScreenAccessor.of(s).getReason());
        else if (screen instanceof AlertScreen s) {
            return new ConfirmationScreen(Minecraft.getInstance().screen, 230, 97, s.getTitle(), s.messageText, LegacyScreen::onClose) {
                protected void addButtons() {
                    renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b -> s.callback.run()).bounds(panel.x + 15, panel.y + panel.height - 30, 200, 20).build());
                }
                public boolean shouldCloseOnEsc() {
                    return s.shouldCloseOnEsc();
                }
            };
        }else if (screen instanceof BackupConfirmScreen s) {
            return new ConfirmationScreen(Minecraft.getInstance().screen, 230, 141 + (BackupConfirmScreenAccessor.of(s).hasCacheErase() ? 14 : 0), s.getTitle(), BackupConfirmScreenAccessor.of(s).getDescription(), LegacyScreen::onClose) {
                boolean eraseCache = false;
                protected void addButtons() {
                    if (BackupConfirmScreenAccessor.of(s).hasCacheErase()) renderableVList.addRenderable(new TickBox(panel.x + 15, panel.y + panel.height - 88,eraseCache,b->Component.translatable("selectWorld.backupEraseCache"),b->null,b-> eraseCache = b.selected));
                    renderableVList.addRenderable(okButton = Button.builder(Component.translatable("selectWorld.backupJoinConfirmButton"), b -> BackupConfirmScreenAccessor.of(s).proceed(true, eraseCache)).build());
                    renderableVList.addRenderable(Button.builder(Component.translatable("selectWorld.backupJoinSkipButton"), b -> BackupConfirmScreenAccessor.of(s).proceed(false, eraseCache)).build());
                    renderableVList.addRenderable(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).build());
                }
                //? if >1.20.2 {
                @Override
                public void onClose() {
                    BackupConfirmScreenAccessor.of(s).cancel();
                }
                //?}
            };
        }
        return screen;
    }

    public static void preTick(Minecraft minecraft){
        ControlType activeControlType = ControlType.getActiveType();
        if (lastControlType != activeControlType){
            if (lastControlType != null) ControlType.UpdateEvent.EVENT.invoker.change(lastControlType, activeControlType);
            lastControlType = activeControlType;
        }
        if (minecraft.screen instanceof ReplaceableScreen r && r.canReplace()) minecraft.setScreen(r.getReplacement());
        if (minecraft.player != null){
            if (!lastArmorSlots.equals(minecraft.player.getInventory().armor)){
                if (!lastArmorSlots.isEmpty()){
                    ScreenUtil.animatedCharacterTime = Util.getMillis();
                    ScreenUtil.remainingAnimatedCharacterTime = 1500;
                }
                lastArmorSlots.clear();
                lastArmorSlots.addAll(minecraft.player.getInventory().armor);
            }
        }
        if (LegacyOptions.unfocusedInputs.get()) minecraft.setWindowActive(true);
        while (keyCrafting.consumeClick()){
            if (minecraft.player != null && (minecraft.player.isCreative() || minecraft.player.isSpectator())) {
                if (minecraft.player.isSpectator()) minecraft.gui.getSpectatorGui().onMouseMiddleClick();
                else minecraft.setScreen(CreativeModeScreen.getActualCreativeScreenInstance(minecraft));
                continue;
            }
            if (ScreenUtil.hasClassicCrafting()) {
                minecraft.getTutorial().onOpenInventory();
                minecraft.setScreen(new InventoryScreen(minecraft.player));
            }else {
                if (minecraft.hitResult instanceof BlockHitResult r && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof CraftingTableBlock){
                    minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, r);
                } else if (ScreenUtil.hasMixedCrafting()) {
                    minecraft.setScreen(MixedCraftingScreen.playerCraftingScreen(minecraft.player));
                } else CommonNetwork.sendToServer(ServerOpenClientMenuPayload.playerCrafting());
            }
        }
        while (keyHostOptions.consumeClick()) {
            minecraft.setScreen(new HostOptionsScreen());
        }
        while (keyLegacy4JSettings.consumeClick()) {
            minecraft.setScreen(new Legacy4JSettingsScreen(Minecraft.getInstance().screen));
        }
        boolean left;
        while ((left=keyCycleHeldLeft.consumeClick()) || keyCycleHeldRight.consumeClick()){
            if (minecraft.player != null) {
                if (minecraft.player.isSpectator()) {
                    if (minecraft.gui.getSpectatorGui().isMenuActive()) minecraft.gui.getSpectatorGui().onMouseScrolled(left ? -1 : 1);
                } else /*? if <1.21.2 {*/minecraft.player.getInventory().swapPaint(left ? 1 : -1)/*?} else {*/ /*minecraft.player.getInventory().setSelectedHotbarSlot(Stocker.cyclic(0, minecraft.player.getInventory().selected + (left ? -1 : 1),9))*//*?}*/;
            }
        }

        if (!Minecraft.getInstance().isPaused()) {
            TopMessage.tick();
        }
    }

    public static void postTick(Minecraft minecraft){
        if (minecraft.level != null && minecraft.screen == null && LegacyOptions.hints.get() && LegacyTipManager.getActualTip() == null) {
            HitResult hit = minecraft.hitResult;
            if (hit instanceof BlockHitResult blockHitResult) {
                BlockState state = minecraft.level.getBlockState(blockHitResult.getBlockPos());
                if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock) && state.getBlock().asItem() instanceof BlockItem) {
                    if (!knownBlocks.contains(state.getBlock()) && LegacyTipManager.setTip(LegacyTipManager.getTip(state.getBlock().asItem().getDefaultInstance()))) knownBlocks.add(state.getBlock());
                }
            } else if (hit instanceof EntityHitResult r){
                Entity e = r.getEntity();
                if (!knownEntities.contains(e.getType()) && LegacyTipManager.setTip(LegacyTipManager.getTip(e))) knownEntities.add(r.getEntity().getType());
            }
        }
    }

    public static void postScreenInit(Screen screen){
        if (screen.getFocused() != null && !screen.children().contains(screen.getFocused())){
            //? if >1.20.1 {
            screen.clearFocus();
            //?} else {
            /*screen.setFocused(null);
            *///?}
        }
        if ((Minecraft.getInstance().getLastInputType().isKeyboard() || controllerManager.connectedController != null || controllerManager.getCursorMode() == 2) && controllerManager.getCursorMode() != 1) {
            Controller.Event e = Controller.Event.of(screen);
            if (e.disableCursorOnInit() && controllerManager.getCursorMode() != 1) controllerManager.disableCursor();
            if (controllerManager.isCursorDisabled && (!e.disableCursorOnInit() || controllerManager.getCursorMode() == 1)) controllerManager.enableAndResetCursor();
            if (screen.getFocused() == null || !screen.getFocused().isFocused()) {
                ComponentPath path = screen.nextFocusPath(new FocusNavigationEvent.ArrowNavigation(ScreenDirection.DOWN));
                if (path != null) path.applyFocus(true);
            }
        }
        controllerManager.resetCursor();
    }

    public static void clientPlayerJoin(LocalPlayer p){
        gameRules = new GameRules(/*? if >=1.21.2 {*//*p.connection.enabledFeatures()*//*?}*/);
        LegacyCreativeTabListing.rebuildVanillaCreativeTabsItems(Minecraft.getInstance());
    }

    public static void serverPlayerJoin(ServerPlayer player){
        if (serverPlayerJoinConsumer != null) {
            serverPlayerJoinConsumer.accept(player);
            serverPlayerJoinConsumer = null;
        }
    }

    public static void init() {
        knownBlocks = new KnownListing<>(BuiltInRegistries.BLOCK,Minecraft.getInstance().gameDirectory.toPath());
        knownEntities = new KnownListing<>(BuiltInRegistries.ENTITY_TYPE,Minecraft.getInstance().gameDirectory.toPath());
        currentWorldSource = LevelStorageSource.createDefault(Minecraft.getInstance().gameDirectory.toPath().resolve("current-world"));
        MCAccount.loadAll();
        FactoryOptions.NEAREST_MIPMAP_SCALING.set(true);
        FactoryOptions.RANDOM_BLOCK_ROTATIONS.set(false);
        ControlType.UpdateEvent.EVENT.register((last, actual)->{
            UIAccessor uiAccessor = Minecraft.getInstance().screen == null ? FactoryScreenUtil.getGuiAccessor() : FactoryScreenUtil.getScreenAccessor();
            uiAccessor.reloadUI();
            LegacyTipManager.rebuildActual();
        });
        FactoryAPIClient.registerKeyMapping(registry->{
            registry.accept(keyCrafting);
            registry.accept(keyHostOptions);
            registry.accept(keyLegacy4JSettings);
            registry.accept(keyCycleHeldLeft);
            registry.accept(keyCycleHeldRight);
            registry.accept(keyToggleCursor);
            registry.accept(keyFlyUp);
            registry.accept(keyFlyDown);
            registry.accept(keyFlyLeft);
            registry.accept(keyFlyRight);
        });
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,legacyTipManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,legacyCreativeListingManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,legacyCraftingListingManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,legacyWorldTemplateManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,legacyTipOverridesManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,legacyBiomeOverrides);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,legacyResourceManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,stoneCuttingGroupManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,loomListingManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,controlTooltipGuiManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,leaderBoardListingManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES,howToPlaySectionManager);
        FactoryAPIClient.setup(m->{
            LegacyOptions.CLIENT_STORAGE.load();
            FactoryAPIClient.registerRenderType(RenderType.cutoutMipped(), SHRUB.get());
            FactoryAPIClient.registerRenderType(RenderType.translucent(), Blocks.WATER);
            ControllerBinding.setupDefaultBindings(m);
            controllerManager.setup();
            //? if fabric
            if (FactoryAPI.isModLoaded("modmenu")) ModMenuCompat.init();
            //? if fabric || >=1.21 && neoforge {
            if (FactoryAPI.isModLoaded("sodium")) SodiumCompat.init();
            if (FactoryAPI.isModLoaded("iris")) IrisCompat.init();
            //?}

            String[] nonScaledElements = new String[]{FactoryGuiElement.SELECTED_ITEM_NAME.name(), FactoryGuiElement.OVERLAY_MESSAGE.name(), FactoryGuiElement.SPECTATOR_TOOLTIP.name()};
            ArbitrarySupplier<Float> hudScale = ()-> 3 / ScreenUtil.getHUDScale();
            ArbitrarySupplier<Float> crosshairScale = ()-> ScreenUtil.getStandardHeight() >= 1080 ? switch (LegacyOptions.hudScale.get()){
                case 1,2 -> 0.9f;
                default -> 1f;
            } : 1f;
            UIAccessor accessor = FactoryScreenUtil.getGuiAccessor();
            FactoryGuiElement.HOTBAR.pre().register(guiGraphics -> {
                ScreenUtil.renderAnimatedCharacter(guiGraphics);
                int newSelection = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getInventory().selected : -1;
                if (ScreenUtil.lastHotbarSelection >= 0 && ScreenUtil.lastHotbarSelection != newSelection) ScreenUtil.lastHotbarSelectionChange = Util.getMillis();
                ScreenUtil.lastHotbarSelection = newSelection;
                if (ColorUtil.getAlpha(FactoryGuiElement.HOTBAR.getColor(accessor)) < 1.0)
                    FactoryGuiGraphics.of(guiGraphics).pushBufferSource(BufferSourceWrapper.translucent(FactoryGuiGraphics.of(guiGraphics).getBufferSource()));
            });
            FactoryGuiElement.HOTBAR.post().register(guiGraphics -> {
                if (ColorUtil.getAlpha(FactoryGuiElement.HOTBAR.getColor(accessor)) < 1.0)
                    FactoryGuiGraphics.of(guiGraphics).popBufferSource();
                if (Minecraft.getInstance().player != null) ControlTooltip.Renderer.of(Minecraft.getInstance().gui).render(guiGraphics, 0,0, FactoryAPIClient.getPartialTick());
                ScreenUtil.renderTopText(guiGraphics, TopMessage.small,21,1.0f, TopMessage.smallTicks);
                ScreenUtil.renderTopText(guiGraphics, TopMessage.medium,37,1.5f, TopMessage.mediumTicks);
            });
            FactoryGuiElement.SPECTATOR_HOTBAR.pre().register(guiGraphics -> {
                Legacy4JClient.legacyFont = false;
                ScreenUtil.renderAnimatedCharacter(guiGraphics);
            });
            FactoryGuiElement.SPECTATOR_HOTBAR.post().register(guiGraphics -> Legacy4JClient.legacyFont = true);
            accessor.getStaticDefinitions().add(UIDefinition.createBeforeInit(a->{
                if (!LegacyMixinOptions.legacyGui.get()) return;
                a.getElements().put(FactoryGuiElement.VIGNETTE.name()+".isVisible", ()-> false);
                a.getElements().put("isGuiVisible", ()-> Minecraft.getInstance().screen == null);
                a.getElements().put("hud.scaleX", hudScale);
                a.getElements().put("hud.scaleY", hudScale);
                a.getElements().put("hud.scaleZ", hudScale);
                a.getElements().put("hud.translateX", ()-> Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2);
                a.getElements().put("hud.scaledTranslateX", ()-> -Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2);
                a.getElements().put("hud.translateY", ()-> Minecraft.getInstance().getWindow().getGuiScaledHeight() + ScreenUtil.getHUDDistance());
                a.getElements().put("hud.scaledTranslateY", ()-> -Minecraft.getInstance().getWindow().getGuiScaledHeight());
                a.getElements().put("hud.renderColor", ()-> ColorUtil.colorFromFloat(1.0f,1.0f,1.0f, ScreenUtil.getHUDOpacity()));
                a.getElements().put(FactoryGuiElement.BOSSHEALTH.name()+".renderColor", ()-> ColorUtil.colorFromFloat(1.0f,1.0f,1.0f, ScreenUtil.getInterfaceOpacity()));

                a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".scaleX", crosshairScale);
                a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".scaleY", crosshairScale);
                a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".scaleZ", crosshairScale);
                a.putStaticElement(FactoryGuiElement.CROSSHAIR.name()+".hud.translateY", false);
                a.putStaticElement(FactoryGuiElement.CROSSHAIR.name()+".hud.scaledTranslateY", false);
                a.getElements().put(FactoryGuiElement.SPECTATOR_HOTBAR.name()+".translateY", ()-> (22-ScreenUtil.getHUDDistance()) * (1- SpectatorGuiAccessor.getInstance().getVisibility()));
                a.getElements().put(FactoryGuiElement.SPECTATOR_TOOLTIP.name()+".translateY", ()-> (22-ScreenUtil.getHUDDistance()) * (1- SpectatorGuiAccessor.getInstance().getVisibility()) + 35 - ScreenUtil.getHUDSize() + ScreenUtil.getHUDDistance());

                for (String element : nonScaledElements) {
                    a.putStaticElement(element+".hud.translateX", false);
                    a.putStaticElement(element+".hud.translateY", false);
                    a.putStaticElement(element+".hud.scaledTranslateX", false);
                    a.putStaticElement(element+".hud.scaledTranslateY", false);
                    a.putStaticElement(element+".hud.scale", false);
                }
                a.getElements().put(FactoryGuiElement.OVERLAY_MESSAGE.name()+".translateY", ()-> ScreenUtil.getHUDDistance() + 72 - LegacyOptions.selectedItemTooltipSpacing.get() - ScreenUtil.getHUDSize() - (GuiAccessor.getInstance().getLastToolHighlight().isEmpty() || GuiAccessor.getInstance().getToolHighlightTimer() <= 0 || ScreenUtil.getSelectedItemTooltipLines() == 0 ? 0 : (Math.min(ScreenUtil.getSelectedItemTooltipLines() + 1, ScreenUtil.getTooltip(GuiAccessor.getInstance().getLastToolHighlight()).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * LegacyOptions.selectedItemTooltipSpacing.get()));

                a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".translateY", ()-> Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2);
                a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".scaledTranslateY", ()-> -Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2);
            }));
        });

        FactoryAPIClient.registerBlockColor(registry->{
            registry.accept((blockState, blockAndTintGetter, blockPos, i) -> blockAndTintGetter == null || blockPos == null ? GrassColor.getDefaultColor() : BiomeColors.getAverageGrassColor(blockAndTintGetter, blockPos), SHRUB.get());

            registry.accept((blockState, blockAndTintGetter, blockPos, i) -> {
                if (blockAndTintGetter != null && blockPos != null){
                    BlockEntity blockEntity = blockAndTintGetter.getBlockEntity(blockPos);
                    if (blockEntity instanceof LegacyPistonMovingBlockEntity e && e.getRenderingBlockEntity() != null && LegacyOptions.enhancedPistonMovingRenderer.get()) {
                        blockEntity = e.getRenderingBlockEntity();
                    }
                    if (blockEntity instanceof WaterCauldronBlockEntity be){
                        if (!be.hasWater()) return /*? if <1.20.5 {*//*PotionUtils.getColor*//*?} else if <1.21.4 {*/PotionContents.getColor/*?} else {*//*PotionContents.getColorOptional*//*?}*/(be.potion.value().getEffects())/*? if >=1.21.4 {*//*.orElse(-13083194)*//*?}*/;
                        else if (be.waterColor != null) return be.waterColor;
                    }
                    return BiomeColors.getAverageWaterColor(blockAndTintGetter, blockPos);
                }
                return -1;
            }, Blocks.WATER_CAULDRON);
        });
        fastLeavesModels.put(Blocks.OAK_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_oak_leaves")));
        fastLeavesModels.put(Blocks.SPRUCE_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_spruce_leaves")));
        fastLeavesModels.put(Blocks.BIRCH_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_birch_leaves")));
        fastLeavesModels.put(Blocks.JUNGLE_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_jungle_leaves")));
        fastLeavesModels.put(Blocks.ACACIA_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_acacia_leaves")));
        fastLeavesModels.put(Blocks.CHERRY_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_cherry_leaves")));
        fastLeavesModels.put(Blocks.DARK_OAK_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_dark_oak_leaves")));
        fastLeavesModels.put(Blocks.MANGROVE_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_mangrove_leaves")));
        //? if >=1.21.2 {
        /*fastLeavesModels.put(Blocks.PALE_OAK_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_pale_oak_leaves")));
        *///?}
        fastLeavesModels.put(Blocks.AZALEA_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_azalea_leaves")));
        fastLeavesModels.put(Blocks.FLOWERING_AZALEA_LEAVES, FactoryAPIClient.createModelLocation(FactoryAPI.createVanillaLocation("fast_flowering_azalea_leaves")));

        FactoryAPIClient.registerExtraModels(register-> fastLeavesModels.values().forEach(register));

        //? if <1.21.4 {
        FactoryAPIClient.registerItemColor(registry->{
            registry.accept((item,i) ->0xFF3153AF, LegacyRegistries.WATER.get());
            registry.accept((itemStack, i) -> GrassColor.getDefaultColor(),SHRUB.get().asItem());
        });
        //?}
        FactoryAPIClient.registerMenuScreen(registry->{
            registry.register(LegacyRegistries.CRAFTING_PANEL_MENU.get(),LegacyCraftingScreen::craftingScreen);
            registry.register(LegacyRegistries.PLAYER_CRAFTING_PANEL_MENU.get(),LegacyCraftingScreen::playerCraftingScreen);
            registry.register(LegacyRegistries.LOOM_PANEL_MENU.get(),LegacyLoomScreen::new);
            registry.register(LegacyRegistries.STONECUTTER_PANEL_MENU.get(),LegacyStonecutterScreen::new);
            registry.register(LegacyRegistries.MERCHANT_MENU.get(),LegacyMerchantScreen::new);
        });
        FactoryAPIClient.preTick(Legacy4JClient::preTick);
        FactoryAPIClient.postTick(Legacy4JClient::postTick);
        FactoryAPIClient.PlayerEvent.JOIN_EVENT.register(Legacy4JClient::clientPlayerJoin);
        FactoryAPIClient.STOPPING.register(m->{
            knownBlocks.save();
            knownEntities.save();
            PackAlbum.applyDefaultResourceAlbum();
            LegacyOptions.lastLoadedVersion.set(Legacy4J.VERSION.get());
            LegacyOptions.lastLoadedMinecraftVersion.set(SharedConstants.getCurrentVersion().getName());
            LegacyOptions.CLIENT_STORAGE.save();
        });
        FactoryEvent.ServerSave.EVENT.register((server, log, flush, force) -> {
            Legacy4JClient.retakeWorldIcon = true;
            knownBlocks.save();
            knownEntities.save();
        });
        FactoryEvent.registerBuiltInPacks(registry->{
            registry.registerResourcePack(FactoryAPI.createLocation(MOD_ID,"legacy_waters"),true);
            registry.registerResourcePack(FactoryAPI.createLocation(MOD_ID,"console_aspects"),false);
            if (FactoryAPI.getLoader().isForgeLike()) {
                registry.register("programmer_art", FactoryAPI.createLocation(MOD_ID, "programmer_art"), Component.translatable("legacy.builtin.console_programmer"), Pack.Position.TOP, false);
                registry.register("high_contrast", FactoryAPI.createLocation(MOD_ID,"high_contrast"), Component.translatable("legacy.builtin.high_contrast"), Pack.Position.TOP, false);
            }
        });
        FactoryAPIClient.uiDefinitionManager.staticList.add(UIDefinition.createBeforeInit(a-> {
            CommonValue.COMMON_VALUES.forEach((s,c)-> a.getElements().put("commonValue."+(s.getNamespace().equals("minecraft") ? "" : s.getNamespace() + ".")+s.getPath(),c));
            CommonColor.COMMON_COLORS.forEach((s,c)-> a.getElements().put("commonColor."+(s.getNamespace().equals("minecraft") ? "" : s.getNamespace() + ".")+s.getPath(),c));
            ControlTooltip.commonIcons.forEach((s,i)-> {
                a.getElements().put("controlIcon."+s, i.map(ControlTooltip.ComponentIcon::getComponent));
            });
            for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
                a.getElements().put("controlIcon."+keyMapping.getName(), ()-> ControlTooltip.getIconComponentFromKeyMapping(LegacyKeyMapping.of(keyMapping)));
            }
            a.getElements().put("interfaceResolution", ScreenUtil::getInterfaceResolution);
            ControlType.types.forEach((s,c)->{
                a.getElements().put("activeControlType."+s, ()-> ControlType.getActiveType().equals(c));
            });
 
        }));
        LegacyUIElementTypes.init();
        //? if >=1.21.2 {
        /*FactoryRenderStateExtension.types.add(new FactoryRenderStateExtension.Type<>(ThrownTridentRenderState.class,LoyaltyLinesRenderState::new));
        FactoryRenderStateExtension.types.add(new FactoryRenderStateExtension.Type<>(FireworkRocketRenderState.class,LegacyFireworkRenderState::new));
        FactoryRenderStateExtension.types.add(new FactoryRenderStateExtension.Type<>(LivingEntityRenderState.class,LegacyLivingEntityRenderState::new));
        FactoryRenderStateExtension.types.add(new FactoryRenderStateExtension.Type<>(VillagerRenderState.class,LegacyVillagerRenderState::new));
        *///?}

        FactoryAPIClient.registerRenderLayer(r->{
            if (r.getEntityRenderer(EntityType.GHAST) instanceof GhastRenderer renderer){
                r.register(renderer, new EyesLayer<>(renderer){
                    @Override
                    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, /*? if <1.21.2 {*/Ghast ghast/*?} else {*//*GhastRenderState ghastRenderState*//*?}*/, float f, float g/*? if <1.21.2 {*/, float h, float j, float k, float l/*?}*/) {
                        if (!/*? if <1.21.2 {*/ghast.isCharging()/*?} else {*//*ghastRenderState.isCharging*//*?}*/) return;
                        super.render(poseStack, multiBufferSource, i, /*? if <1.21.2 {*/ghast, f, g, h, j, k, l/*?} else {*//*ghastRenderState, f, g*//*?}*/);
                    }
                    @Override
                    public RenderType renderType() {
                        return GHAST_SHOOTING_GLOW;
                    }
                });
            }
            if (r.getEntityRenderer(EntityType.DROWNED) instanceof DrownedRenderer renderer){
                r.register(renderer, new EyesLayer<>(renderer){
                    @Override
                    public RenderType renderType() {
                        return DROWNED_GLOW;
                    }
                });
            }
        });
        //? if forge || neoforge {
        /*FactoryAPIPlatform.getModEventBus().addListener(EventPriority.NORMAL,false, RegisterPresetEditorsEvent.class, e->Legacy4JClient.VANILLA_PRESET_EDITORS.forEach(((o, presetEditor) -> o.ifPresent(worldPresetResourceKey -> e.register(worldPresetResourceKey, presetEditor)))));
        *///?}
        FactoryAPIClient.PlayerEvent.DISCONNECTED_EVENT.register(p->{
            lastArmorSlots.clear();
            PackAlbum.applyDefaultResourceAlbum();
        });
        FactoryAPIClient.registerConfigScreen(FactoryAPIPlatform.getModInfo(MOD_ID),Legacy4JSettingsScreen::new);
        FactoryAPIClient.registerDefaultConfigScreen("minecraft", s-> new OptionsScreen(s, Minecraft.getInstance().options));
    }

    public static void updateChunks(){
        FactoryAPIClient.SECURE_EXECUTOR.execute(()->Minecraft.getInstance().levelRenderer.allChanged());
    }

    public static void updateSkyShape(){
        FactoryAPIClient.SECURE_EXECUTOR.execute(()->((LevelRendererAccessor)Minecraft.getInstance().levelRenderer).updateSkyBuffers());
    }

    public static void buildLegacySkyDisc(VertexConsumer consumer, float f) {
        for(int k = -384; k <= 384; k += 64) {
            for(int l = -384; l <= 384; l += 64) {
                float g = (float)k;
                float f1 = (float)(k + 64);
                //? if <1.20.5 {
                /*consumer.vertex(g, f, l).endVertex();
                consumer.vertex(f1, f, l).endVertex();
                consumer.vertex(f1, f, l + 64).endVertex();
                consumer.vertex(g, f, l + 64).endVertex();
                *///?} else {
                consumer.addVertex(g, f, l);
                consumer.addVertex(f1, f, l);
                consumer.addVertex(f1, f, l + 64);
                consumer.addVertex(g, f, l + 64);
                //?}
            }
        }
    }

    public static BakedModel getFastLeavesModelReplacement(BlockGetter blockGetter, BlockPos pos, BlockState blockState, BakedModel model){
        boolean fastGraphics = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        if (LegacyOptions.fastLeavesCustomModels.get() && blockState.getBlock() instanceof LeavesBlock && fastLeavesModels.containsKey(blockState.getBlock()) && (fastGraphics || LegacyOptions.fastLeavesWhenBlocked.get())){
            if (!fastGraphics && blockGetter != null) {
                for (Direction value : Direction.values()) {
                    BlockPos relative = pos.relative(value);
                    BlockState relativeBlockState = blockGetter.getBlockState(relative);
                    if (!(relativeBlockState.getBlock() instanceof LeavesBlock) && !relativeBlockState.isSolidRender(/*? if <1.21.2 {*/blockGetter, relative/*?}*/)) {
                        return model;
                    }
                }
            }
            return FactoryAPIClient.getModel(fastLeavesModels.get(blockState.getBlock()));
        }
        return model;
    }

    public static int getEffectiveRenderDistance(){
        return Minecraft.getInstance().options.getEffectiveRenderDistance();
    }

    public static void onClientPlayerInfoChange(){
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof HostOptionsScreen s) s.reloadPlayerButtons();
        else if (minecraft.screen instanceof LeaderboardsScreen s){
            s.rebuildRenderableVList(minecraft);
            s.repositionElements();
        }
    }

    public static boolean canSkipIntro(float timer){
        return timer % INTROS.size() >= INTROS.size() - 0.01f || LegacyOptions.skipIntro.get() || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_RETURN) || GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS || ControllerBinding.DOWN_BUTTON.state().pressed;
    }

    public static final KeyMapping keyCrafting = new KeyMapping("legacy.key.crafting", InputConstants.KEY_E, "key.categories.inventory");
    public static final KeyMapping keyCycleHeldLeft = new KeyMapping("legacy.key.cycleHeldLeft", InputConstants.KEY_PAGEDOWN, "key.categories.inventory");
    public static final KeyMapping keyCycleHeldRight = new KeyMapping("legacy.key.cycleHeldRight", InputConstants.KEY_PAGEUP, "key.categories.inventory");
    public static final KeyMapping keyToggleCursor = new KeyMapping("legacy.key.toggleCursor", -1, "key.categories.misc");
    public static KeyMapping keyHostOptions = new KeyMapping( MOD_ID +".key.host_options", InputConstants.KEY_H, "key.categories.misc");
    public static KeyMapping keyLegacy4JSettings = new KeyMapping( MOD_ID +".key.legacy4JSettings", InputConstants.KEY_Y, "key.categories.misc");
    public static KeyMapping keyFlyUp = new KeyMapping( MOD_ID +".key.flyUp", InputConstants.KEY_UP, "key.categories.movement");
    public static KeyMapping keyFlyDown = new KeyMapping( MOD_ID +".key.flyDown", InputConstants.KEY_DOWN, "key.categories.movement");
    public static KeyMapping keyFlyLeft = new KeyMapping( MOD_ID +".key.flyLeft", InputConstants.KEY_LEFT, "key.categories.movement");
    public static KeyMapping keyFlyRight = new KeyMapping( MOD_ID +".key.flyRight", InputConstants.KEY_RIGHT, "key.categories.movement");


    public static void resetOptions(Minecraft minecraft){
        whenResetOptions.forEach(Runnable::run);
        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            keyMapping.setKey(keyMapping.getDefaultKey());
            LegacyKeyMapping.of(keyMapping).setBinding(LegacyKeyMapping.of(keyMapping).getDefaultBinding());
            KeyMapping.resetMapping();
        }
        LegacyOptions.CLIENT_STORAGE.configMap.values().forEach(FactoryConfig::reset);
        LegacyOptions.CLIENT_STORAGE.save();
        LegacyCommonOptions.COMMON_STORAGE.save();
        LegacyCommonOptions.COMMON_STORAGE.configMap.values().forEach(FactoryConfig::reset);
        minecraft.options.save();
    }

    public static String manageAvailableSaveDirName(Consumer<File> copy,Predicate<String> exists, LevelStorageSource source, String levelId){
        String destId = manageAvailableName(exists,levelId);
        copy.accept(source.getBaseDir().resolve(destId).toFile());
        return destId;
    }

    public static String manageAvailableName(Predicate<String> exists, String saveDirName){
        StringBuilder builder = new StringBuilder(saveDirName);
        int repeat = 0;
        while (exists.test(builder +(repeat > 0 ? String.format(" (%s)",repeat) : "")))
            repeat++;
        if (repeat > 0)
            builder.append(String.format(" (%s)",repeat));
        return builder.toString();
    }

    public static String importSaveFile(InputStream saveInputStream,Predicate<String> exists, LevelStorageSource source, String saveDirName){
        return manageAvailableSaveDirName(f-> Legacy4J.copySaveToDirectory(saveInputStream,f),exists,source,saveDirName);
    }

    public static String importSaveFile(InputStream saveInputStream, LevelStorageSource source, String saveDirName){
        return importSaveFile(saveInputStream,source::levelExists,source,saveDirName);
    }

    public static String copySaveFile(Path savePath, LevelStorageSource source, String saveDirName){
        return manageAvailableSaveDirName(f-> {
            try {
                FileUtils.copyDirectory(savePath.toFile(),f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        },source::levelExists,source,saveDirName);
    }

    public static void copySaveBtwSources(LevelStorageSource.LevelStorageAccess sendSource, LevelStorageSource destSource){
        try {
            File destLevelDirectory = destSource.getBaseDir().resolve(sendSource.getLevelId()).toFile();
            if (destLevelDirectory.exists()) FileUtils.deleteQuietly(destLevelDirectory);
            FileUtils.copyDirectory(sendSource.getDimensionPath(Level.OVERWORLD).toFile(), destLevelDirectory, p -> !p.getName().equals("session.lock"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
}