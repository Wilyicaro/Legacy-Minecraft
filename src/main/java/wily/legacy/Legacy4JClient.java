package wily.legacy;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.DataResult;
import net.minecraft.SharedConstants;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
//? if <1.21.5 {
/*import net.minecraft.client.resources.model.BakedModel;
 *///?} else {
import net.minecraft.client.renderer.block.model.BlockStateModel;
//?}
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.*;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.*;
//?}
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
//? if >=1.20.5 {
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.client.gui.screens.options.OptionsScreen;
//?} else {
/*import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.client.gui.screens.OptionsScreen;
*///?}
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
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.client.controller.*;
//? if fabric {
import wily.legacy.client.screen.compat.ModMenuCompat;
//?} else if forge {
/*import net.minecraftforge.client.event.RegisterPresetEditorsEvent;
 *///?} else if neoforge {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;
*///?}
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.client.*;
import wily.legacy.client.screen.*;
//? if fabric || >=1.21 && neoforge {
import wily.legacy.client.screen.compat.IrisCompat;
import wily.legacy.client.screen.compat.SodiumCompat;
//?}
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.entity.LegacyLocalPlayer;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.init.LegacyUIElementTypes;
import wily.legacy.inventory.LegacyPistonMovingBlockEntity;
import wily.legacy.network.ServerOpenClientMenuPayload;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.network.TopMessage;
import wily.legacy.util.client.LegacyGuiElements;
import wily.legacy.util.client.MCAccount;


import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static wily.legacy.Legacy4J.MOD_ID;
import static wily.legacy.init.LegacyRegistries.SHRUB;


public class Legacy4JClient {

    public static final List<Runnable> whenResetOptions = new ArrayList<>();
    public static final LegacyTipManager legacyTipManager = new LegacyTipManager();
    public static final MapIdValueManager<LegacyCreativeTabListing, ?> legacyCreativeListingManager = MapIdValueManager.create(Legacy4J.createModLocation("creative_tab_listing"), LegacyCreativeTabListing.CODEC);
    public static final MapIdValueManager<LegacyCraftingTabListing, ?> legacyCraftingListingManager = MapIdValueManager.create(Legacy4J.createModLocation("crafting_tab_listing"), LegacyCraftingTabListing.CODEC);
    public static final MapIdValueManager<LegacyBiomeOverride, ?> legacyBiomeOverrides = MapIdValueManager.createWithListCodec(Legacy4J.createModLocation("biome_overrides"), LegacyBiomeOverride.LIST_MAP_CODEC);
    public static final LegacyWorldTemplate.Manager legacyWorldTemplateManager = new LegacyWorldTemplate.Manager();
    public static final LegacyTipOverride.Manager legacyTipOverridesManager = new LegacyTipOverride.Manager();
    public static final LegacyResourceManager legacyResourceManager = new LegacyResourceManager();
    public static final StoneCuttingGroupManager stoneCuttingGroupManager = new StoneCuttingGroupManager();
    public static final MapIdValueManager<LoomTabListing, ?> loomListingManager = MapIdValueManager.create(Legacy4J.createModLocation("loom_tab_listing"), LoomTabListing.CODEC);
    public static final MapIdValueManager<TypeCraftingTab, ?> typeCraftingTabs = MapIdValueManager.create(Legacy4J.createModLocation("type_crafting_tabs"), TypeCraftingTab.CODEC);
    public static final MapIdValueManager<LegacyTabDisplay, ?> mixedCraftingTabs = MapIdValueManager.create(Legacy4J.createModLocation("mixed_crafting_tabs"), LegacyTabDisplay.CODEC.validate(display -> MixedCraftingScreen.isValidTab(display) ? DataResult.success(display) : DataResult.error(() -> display.id() + " is an invalid tab!")));
    public static final ControlTooltip.GuiManager controlTooltipGuiManager = new ControlTooltip.GuiManager();
    public static final LeaderboardsScreen.Manager leaderBoardListingManager = new LeaderboardsScreen.Manager();
    public static final HowToPlayScreen.Manager howToPlaySectionManager = new HowToPlayScreen.Manager();
    public static final MapIdValueManager<OptionsPreset, ListMap<ResourceLocation, OptionsPreset>> optionPresetsManager = MapIdValueManager.createListMap(Legacy4J.createModLocation("option_presets"), OptionsPreset.CODEC);
    public static final MapIdValueManager<ControlType, ListMap<ResourceLocation, ControlType>> controlTypesManager = MapIdValueManager.createListMap(Legacy4J.createModLocation("control_types"), ControlType.CODEC);
    public static final ControllerManager controllerManager = new ControllerManager();
    public static final Map<Block, ResourceLocation> fastLeavesModels = new HashMap<>();
    public static final FactoryConfig.StorageHandler MIXIN_CONFIGS_STORAGE = FactoryConfig.StorageHandler.fromMixin(LegacyMixinOptions.CLIENT_MIXIN_STORAGE, false);
    public static final RenderType GHAST_SHOOTING_GLOW = RenderType.eyes(FactoryAPI.createVanillaLocation("textures/entity/ghast/ghast_shooting_glow.png"));
    public static final RenderType DROWNED_GLOW = RenderType.eyes(FactoryAPI.createVanillaLocation("textures/entity/zombie/drowned_glow.png"));
    public static final Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> VANILLA_PRESET_EDITORS = new HashMap<>(Map.of(Optional.of(WorldPresets.FLAT), (createWorldScreen, settings) -> {
        ChunkGenerator chunkGenerator = settings.selectedDimensions().overworld();
        RegistryAccess.Frozen registryAccess = settings.worldgenLoadContext();
        HolderLookup.RegistryLookup<Biome> biomeGetter = registryAccess.lookupOrThrow(Registries.BIOME);
        HolderLookup.RegistryLookup<StructureSet> structureGetter = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
        HolderLookup.RegistryLookup<PlacedFeature> placeFeatureGetter = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
        return new LegacyFlatWorldScreen(createWorldScreen, createWorldScreen.getUiState(), biomeGetter, structureGetter, flatLevelGeneratorSettings -> createWorldScreen.getUiState().updateDimensions(PresetEditor.flatWorldConfigurator(flatLevelGeneratorSettings)), chunkGenerator instanceof FlatLevelSource ? ((FlatLevelSource) chunkGenerator).settings() : FlatLevelGeneratorSettings.getDefault(biomeGetter, structureGetter, placeFeatureGetter));
    }, Optional.of(WorldPresets.SINGLE_BIOME_SURFACE), (createWorldScreen, settings) -> new LegacyBuffetWorldScreen(createWorldScreen, settings.worldgenLoadContext().lookupOrThrow(Registries.BIOME), holder -> createWorldScreen.getUiState().updateDimensions(PresetEditor.fixedBiomeConfigurator(holder)))));
    public static final KeyMapping keyCrafting = new KeyMapping("legacy.key.crafting", InputConstants.KEY_E, KeyMapping.Category.INVENTORY);
    public static final KeyMapping keyCycleHeldLeft = new KeyMapping("legacy.key.cycleHeldLeft", InputConstants.KEY_PAGEDOWN, KeyMapping.Category.INVENTORY);
    public static final KeyMapping keyCycleHeldRight = new KeyMapping("legacy.key.cycleHeldRight", InputConstants.KEY_PAGEUP, KeyMapping.Category.INVENTORY);
    public static final KeyMapping keyToggleCursor = new KeyMapping("legacy.key.toggleCursor", -1, KeyMapping.Category.MISC);
    public static KeyMapping keyHostOptions = new KeyMapping(MOD_ID + ".key.host_options", InputConstants.KEY_H, KeyMapping.Category.MISC);
    public static KeyMapping keyLegacy4JSettings = new KeyMapping(MOD_ID + ".key.legacy4JSettings", InputConstants.KEY_Y, KeyMapping.Category.MISC);
    public static KeyMapping keyFlyUp = new KeyMapping(MOD_ID + ".key.flyUp", InputConstants.KEY_UP, KeyMapping.Category.MOVEMENT);
    public static KeyMapping keyFlyDown = new KeyMapping(MOD_ID + ".key.flyDown", InputConstants.KEY_DOWN, KeyMapping.Category.MOVEMENT);
    public static KeyMapping keyFlyLeft = new KeyMapping(MOD_ID + ".key.flyLeft", InputConstants.KEY_LEFT, KeyMapping.Category.MOVEMENT);
    public static KeyMapping keyFlyRight = new KeyMapping(MOD_ID + ".key.flyRight", InputConstants.KEY_RIGHT, KeyMapping.Category.MOVEMENT);
    public static boolean isNewerVersion = false;
    public static boolean isNewerMinecraftVersion = false;
    public static ControlType lastControlType;
    public static boolean canSprint = false;
    public static int sprintTicksLeft = -1;
    public static LegacyLoadingScreen legacyLoadingScreen = new LegacyLoadingScreen();
    public static KnownListing<Block> knownBlocks;
    public static KnownListing<EntityType<?>> knownEntities;
    public static GameType defaultServerGameType;
    public static GameRules gameRules;
    public static Consumer<ServerPlayer> serverPlayerJoinConsumer;

    public static float[] getVisualPlayerColor(LegacyPlayerInfo info) {
        return getVisualPlayerColor(info.getIdentifierIndex() >= 0 ? info.getIdentifierIndex() : info.legacyMinecraft$getProfile().id().hashCode());
    }

    public static float[] getVisualPlayerColor(int i) {
        PlayerIdentifier playerIdentifier = PlayerIdentifier.of(i);
        if (PlayerIdentifier.list.containsKey(i))
            return new float[]{(playerIdentifier.color() >> 16 & 255) / 255f, (playerIdentifier.color() >> 8 & 255) / 255f, (playerIdentifier.color() & 255) / 255f};
        float r = ((playerIdentifier.color() >> 16 & 255) * (0.8f + (i % 15) / 30f)) / 255f;
        float g = ((playerIdentifier.color() >> 8 & 255) * (1.2f - (i % 16) / 32f)) / 255f;
        float b = ((playerIdentifier.color() & 255) * (0.8f + (i % 17) / 34f)) / 255f;
        return new float[]{r, g, b};
    }

    public static void updateLegacyPlayerInfos(Map<UUID, LegacyPlayerInfo> map) {
        Minecraft minecraft = Minecraft.getInstance();
        map.forEach((s, i) -> {
            if (minecraft.getConnection() != null && minecraft.getConnection().getPlayerInfo(s) instanceof LegacyPlayerInfo info)
                info.copyFrom(i);
        });
        LeaderboardsScreen.refreshStatsBoards(minecraft);
        if (minecraft.screen instanceof LeaderboardsScreen s && LeaderboardsScreen.statsBoards.get(s.selectedStatBoard).statsList.isEmpty())
            minecraft.executeIfPossible(() -> s.changeStatBoard(false));
        if (minecraft.player != null) {
            LegacyOptions.classicCrafting.set(LegacyOptions.classicCrafting.get());
            LegacyOptions.classicTrading.set(LegacyOptions.classicTrading.get());
            LegacyOptions.classicStonecutting.set(LegacyOptions.classicStonecutting.get());
            LegacyOptions.classicLoom.set(LegacyOptions.classicLoom.get());
        }
    }

    public static boolean playerHasInfiniteMaterials() {
        return Minecraft.getInstance().player.hasInfiniteMaterials();
    }

    public static Screen getReplacementScreen(Screen screen) {
        if (!LegacyMixinOptions.legacyTitleScreen.get()) return screen;
        if (screen instanceof JoinMultiplayerScreen)
            return new PlayGameScreen(new TitleScreen(), 2);
        else if (screen instanceof DisconnectedScreen s)
            return ConfirmationScreen.createInfoScreen(getReplacementScreen(DisconnectedScreenAccessor.of(s).getParent()), s.getTitle(), DisconnectedScreenAccessor.of(s).getReason());
        else if (screen instanceof AlertScreen s) {
            return new ConfirmationScreen(Minecraft.getInstance().screen, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 50 : 75, s.getTitle(), s.messageText, LegacyScreen::onClose) {
                protected void addButtons() {
                    renderableVList.addRenderable(okButton = Button.builder(Component.translatable("gui.ok"), b -> s.callback.run()).bounds(panel.x + 15, panel.y + panel.height - 30, 200, 20).build());
                }

                public boolean shouldCloseOnEsc() {
                    return s.shouldCloseOnEsc();
                }
            };
        } else if (screen instanceof BackupConfirmScreen s) {
            return new ConfirmationScreen(Minecraft.getInstance().screen, ConfirmationScreen::getPanelWidth, () -> (LegacyOptions.getUIMode().isSD() ? 94 : 141) + (BackupConfirmScreenAccessor.of(s).hasCacheErase() ? LegacyOptions.getUIMode().isSD() ? 11 : 14 : 0), s.getTitle(), BackupConfirmScreenAccessor.of(s).getDescription(), LegacyScreen::onClose) {
                boolean eraseCache = false;

                protected void addButtons() {
                    if (BackupConfirmScreenAccessor.of(s).hasCacheErase())
                        renderableVList.addRenderable(new TickBox(panel.x + 15, panel.y + panel.height - 88, eraseCache, b -> Component.translatable("selectWorld.backupEraseCache"), b -> null, b -> eraseCache = b.selected));
                    renderableVList.addRenderable(okButton = Button.builder(Component.translatable("selectWorld.backupJoinConfirmButton"), b -> BackupConfirmScreenAccessor.of(s).proceed(true, eraseCache)).build());
                    renderableVList.addRenderable(Button.builder(Component.translatable("selectWorld.backupJoinSkipButton"), b -> BackupConfirmScreenAccessor.of(s).proceed(false, eraseCache)).build());
                    renderableVList.addRenderable(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).build());
                }

                @Override
                public void onClose() {
                    BackupConfirmScreenAccessor.of(s).cancel();
                }
            };
        }
        return screen;
    }

    public static void preTick(Minecraft minecraft) {
        if (minecraft.isGameLoadFinished()) {
            ControlType activeControlType = ControlType.getActiveType();
            if (lastControlType != activeControlType) {
                if (lastControlType != null)
                    ControlType.UpdateEvent.EVENT.invoker.change(lastControlType, activeControlType);
                lastControlType = activeControlType;
            }
        }

        if (minecraft.screen instanceof ReplaceableScreen r && r.canReplace()) minecraft.setScreen(r.getReplacement());

        if (LegacyOptions.unfocusedInputs.get()) minecraft.setWindowActive(true);
        while (keyCrafting.consumeClick()) {
            if (minecraft.player != null && (minecraft.player.isCreative() || minecraft.player.isSpectator())) {
                if (minecraft.player.isSpectator()) minecraft.gui.getSpectatorGui().onHotbarActionKeyPressed();
                else minecraft.setScreen(CreativeModeScreen.getActualCreativeScreenInstance(minecraft));
                continue;
            }
            if (minecraft.hitResult instanceof BlockHitResult r && minecraft.level.getBlockState(r.getBlockPos()).getBlock() instanceof CraftingTableBlock && controllerManager.isControllerTheLastInput()) {
                minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, r);
            } else if (LegacyOptions.hasClassicCrafting()) {
                minecraft.getTutorial().onOpenInventory();
                minecraft.setScreen(new InventoryScreen(minecraft.player));
            } else if (LegacyOptions.hasMixedCrafting()) {
                minecraft.setScreen(MixedCraftingScreen.playerCraftingScreen(minecraft.player));
            } else CommonNetwork.sendToServer(ServerOpenClientMenuPayload.playerCrafting());
        }
        while (keyHostOptions.consumeClick()) {
            minecraft.setScreen(new HostOptionsScreen());
        }
        while (keyLegacy4JSettings.consumeClick()) {
            minecraft.setScreen(new Legacy4JSettingsScreen(Minecraft.getInstance().screen));
        }
        boolean left;
        while ((left = keyCycleHeldLeft.consumeClick()) || keyCycleHeldRight.consumeClick()) {
            if (minecraft.player != null) {
                if (minecraft.player.isSpectator()) {
                    if (minecraft.gui.getSpectatorGui().isMenuActive())
                        minecraft.gui.getSpectatorGui().onMouseScrolled(left ? -1 : 1);
                } else {
                    minecraft.player.getInventory().setSelectedSlot(Stocker.cyclic(0, minecraft.player.getInventory().getSelectedSlot() + (left ? -1 : 1), 9));
                }
            }
        }

        if (sprintTicksLeft > 0) --sprintTicksLeft;
        if (minecraft.player != null && controllerManager.isControllerTheLastInput()) {
            BindingState.Axis stick = controllerManager.getButtonState(ControllerBinding.LEFT_STICK);
            float y = Math.abs(stick.y) > stick.getDeadZone() ? stick.y : 0;
            if (((LegacyLocalPlayer) minecraft.player).canSprintController()) {
                if (y < -0.85) {
                    if (!canSprint && sprintTicksLeft == -1) sprintTicksLeft = 9;
                    else if (canSprint && sprintTicksLeft > 0) minecraft.player.setSprinting(true);
                    else canSprint = false;
                } else if (y > -0.85 && sprintTicksLeft == 0) {
                    canSprint = false;
                    sprintTicksLeft = -1;
                } else if (y > -0.5 && !canSprint && sprintTicksLeft > 0) canSprint = true;
            } else {
                if (y > -0.85) minecraft.player.setSprinting(false);
                canSprint = false;
                sprintTicksLeft = -1;
            }
        }

        if (!Minecraft.getInstance().isPaused()) {
            TopMessage.tick();
        }
    }

    public static void postTick(Minecraft minecraft) {
        if (minecraft.level != null && minecraft.screen == null && LegacyOptions.hints.get() && LegacyTipManager.getActualTip() == null) {
            HitResult hit = minecraft.hitResult;
            if (hit instanceof BlockHitResult blockHitResult) {
                BlockState state = minecraft.level.getBlockState(blockHitResult.getBlockPos());
                if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock) && state.getBlock().asItem() instanceof BlockItem) {
                    if (!knownBlocks.contains(state.getBlock()) && LegacyTipManager.setTip(LegacyTipManager.getTip(state.getBlock().asItem().getDefaultInstance())))
                        knownBlocks.add(state.getBlock());
                }
            } else if (hit instanceof EntityHitResult r) {
                Entity e = r.getEntity();
                if (!knownEntities.contains(e.getType()) && LegacyTipManager.setTip(LegacyTipManager.getTip(e)))
                    knownEntities.add(r.getEntity().getType());
            }
        }
    }

    public static void postScreenInit(Screen screen) {
        if (screen.getFocused() != null && !screen.children().contains(screen.getFocused())) {
            screen.clearFocus();
        }
        if ((Minecraft.getInstance().getLastInputType().isKeyboard() || controllerManager.isControllerTheLastInput() || controllerManager.getCursorMode().isNever()) && !controllerManager.getCursorMode().isAlways()) {
            Controller.Event e = Controller.Event.of(screen);
            if (e.disableCursorOnInit() && !controllerManager.getCursorMode().isAlways())
                controllerManager.tryDisableCursor();
            if (controllerManager.isCursorDisabled && (!e.disableCursorOnInit() || controllerManager.getCursorMode().isAlways()))
                controllerManager.enableCursorAndScheduleReset();
            if (controllerManager.isCursorDisabled && (screen.getFocused() == null || !screen.getFocused().isFocused())) {
                ComponentPath path = screen.nextFocusPath(new FocusNavigationEvent.ArrowNavigation(ScreenDirection.DOWN));
                if (path != null) path.applyFocus(true);
            }
        }
        controllerManager.resetCursor();
    }

    public static void clientPlayerJoin(LocalPlayer p) {
        gameRules = new GameRules(/*? if >=1.21.2 {*/p.connection.enabledFeatures()/*?}*/);
        LegacyCreativeTabListing.rebuildVanillaCreativeTabsItems(Minecraft.getInstance());
    }

    public static void serverPlayerJoin(ServerPlayer player) {
        if (serverPlayerJoinConsumer != null) {
            serverPlayerJoinConsumer.accept(player);
            serverPlayerJoinConsumer = null;
        }
    }

    public static void init() {
        ControlType.UpdateEvent.EVENT.register((last, actual) -> {
            UIAccessor uiAccessor = Minecraft.getInstance().screen == null ? FactoryScreenUtil.getGuiAccessor() : FactoryScreenUtil.getScreenAccessor();
            uiAccessor.reloadUI();
            LegacyTipManager.rebuildActual();
        });
        FactoryAPIClient.registerKeyMapping(registry -> {
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
        DynamicUtil.COMMON_ITEMS.put(FactoryAPI.createVanillaLocation("ominous_banner"), () -> {
            if (Minecraft.getInstance().getConnection() == null) return ItemStack.EMPTY;
            return Raid.getOminousBannerInstance(Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN));
        });
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, legacyTipManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, legacyCreativeListingManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, legacyCraftingListingManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, legacyWorldTemplateManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, legacyTipOverridesManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, legacyBiomeOverrides);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, optionPresetsManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, controlTypesManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, legacyResourceManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, stoneCuttingGroupManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, loomListingManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, typeCraftingTabs);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, mixedCraftingTabs);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, controlTooltipGuiManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, leaderBoardListingManager);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, howToPlaySectionManager);
        FactoryOptions.NEAREST_MIPMAP_SCALING.setDefault(true);
        FactoryOptions.RANDOM_BLOCK_ROTATIONS.setDefault(false);
        FactoryAPIClient.setup(m -> {
            MCAccount.loadAll();
            controllerManager.setup(m);
            knownBlocks = new KnownListing<>(BuiltInRegistries.BLOCK, m.gameDirectory.toPath());
            knownEntities = new KnownListing<>(BuiltInRegistries.ENTITY_TYPE, m.gameDirectory.toPath());
            LegacySaveCache.setup(m);
            ControllerBinding.setupDefaultBindings(m);
            LegacyOptions.CLIENT_STORAGE.load();
            FactoryAPIClient.registerRenderType(ChunkSectionLayer.CUTOUT_MIPPED, SHRUB.get());
            FactoryAPIClient.registerRenderType(ChunkSectionLayer.TRANSLUCENT, Blocks.WATER);
            //? if fabric
            if (FactoryAPI.isModLoaded("modmenu")) ModMenuCompat.init();
            //? if fabric || >=1.21 && neoforge {
            if (FactoryAPI.isModLoaded("sodium")) SodiumCompat.init();
            if (FactoryAPI.isModLoaded("iris")) IrisCompat.init();
            //?}
            LegacyGuiElements.setup(m);
        });

        FactoryAPIClient.registerBlockColor(registry -> {
            registry.accept((blockState, blockAndTintGetter, blockPos, i) -> blockAndTintGetter == null || blockPos == null ? GrassColor.getDefaultColor() : BiomeColors.getAverageGrassColor(blockAndTintGetter, blockPos), SHRUB.get());
            BlockColor blockColor = (blockState, blockAndTintGetter, blockPos, i) -> {
                if (blockAndTintGetter != null && blockPos != null) {
                    BlockEntity blockEntity = blockAndTintGetter.getBlockEntity(blockPos);
                    if (blockEntity instanceof LegacyPistonMovingBlockEntity e && e.getRenderingBlockEntity() != null && LegacyOptions.enhancedPistonMovingRenderer.get()) {
                        blockEntity = e.getRenderingBlockEntity();
                    }
                    if (blockEntity instanceof WaterCauldronBlockEntity be) {
                        if (!be.hasWater())
                            return /*? if <1.20.5 {*//*PotionUtils.getColor*//*?} else if <1.21.4 {*//*PotionContents.getColor*//*?} else {*/PotionContents.getColorOptional/*?}*/(be.potion.value().getEffects())/*? if >=1.21.4 {*/.orElse(-13083194)/*?}*/;
                        else if (be.waterColor != null) return be.waterColor;
                    }
                    return BiomeColors.getAverageWaterColor(blockAndTintGetter, blockPos);
                }
                return -1;
            };
            registry.accept(blockColor, Blocks.WATER_CAULDRON);
            registry.accept(blockColor, LegacyRegistries.COLORED_WATER_CAULDRON.get());
        });
        fastLeavesModels.put(Blocks.OAK_LEAVES, FactoryAPI.createVanillaLocation("fast_oak_leaves"));
        fastLeavesModels.put(Blocks.SPRUCE_LEAVES, FactoryAPI.createVanillaLocation("fast_spruce_leaves"));
        fastLeavesModels.put(Blocks.BIRCH_LEAVES, FactoryAPI.createVanillaLocation("fast_birch_leaves"));
        fastLeavesModels.put(Blocks.JUNGLE_LEAVES, FactoryAPI.createVanillaLocation("fast_jungle_leaves"));
        fastLeavesModels.put(Blocks.ACACIA_LEAVES, FactoryAPI.createVanillaLocation("fast_acacia_leaves"));
        fastLeavesModels.put(Blocks.CHERRY_LEAVES, FactoryAPI.createVanillaLocation("fast_cherry_leaves"));
        fastLeavesModels.put(Blocks.DARK_OAK_LEAVES, FactoryAPI.createVanillaLocation("fast_dark_oak_leaves"));
        fastLeavesModels.put(Blocks.MANGROVE_LEAVES, FactoryAPI.createVanillaLocation("fast_mangrove_leaves"));
        fastLeavesModels.put(Blocks.PALE_OAK_LEAVES, FactoryAPI.createVanillaLocation("fast_pale_oak_leaves"));
        fastLeavesModels.put(Blocks.AZALEA_LEAVES, FactoryAPI.createVanillaLocation("fast_azalea_leaves"));
        fastLeavesModels.put(Blocks.FLOWERING_AZALEA_LEAVES, FactoryAPI.createVanillaLocation("fast_flowering_azalea_leaves"));

        FactoryAPIClient.registerExtraModels(register -> fastLeavesModels.values().forEach(register));
        FactoryAPIClient.registerMenuScreen(registry -> {
            registry.register(LegacyRegistries.CRAFTING_PANEL_MENU.get(), LegacyCraftingScreen::craftingScreen);
            registry.register(LegacyRegistries.PLAYER_CRAFTING_PANEL_MENU.get(), LegacyCraftingScreen::playerCraftingScreen);
            registry.register(LegacyRegistries.LOOM_PANEL_MENU.get(), LegacyLoomScreen::new);
            registry.register(LegacyRegistries.STONECUTTER_PANEL_MENU.get(), LegacyStonecutterScreen::new);
            registry.register(LegacyRegistries.MERCHANT_MENU.get(), LegacyMerchantScreen::new);
        });
        FactoryAPIClient.preTick(Legacy4JClient::preTick);
        FactoryAPIClient.postTick(Legacy4JClient::postTick);
        FactoryAPIClient.PlayerEvent.JOIN_EVENT.register(Legacy4JClient::clientPlayerJoin);
        FactoryAPIClient.STOPPING.register(m -> {
            knownBlocks.save();
            knownEntities.save();
            PackAlbum.applyDefaultResourceAlbum();
            LegacyOptions.lastLoadedVersion.set(Legacy4J.VERSION.get());
            LegacyOptions.lastLoadedMinecraftVersion.set(SharedConstants.getCurrentVersion().name());
            LegacyOptions.CLIENT_STORAGE.save();
        });
        FactoryEvent.ServerSave.EVENT.register((server, log, flush, force) -> {
            LegacySaveCache.retakeWorldIcon = true;
            knownBlocks.save();
            knownEntities.save();
        });
        FactoryAPIClient.RESIZE_DISPLAY.register(minecraft -> {
            LegacyTipManager.rebuildActual();
            LegacyTipManager.rebuildActualLoading();
            minecraft.gui.getChat().rescaleChat();
        });
        FactoryEvent.registerBuiltInPacks(registry -> {
            registry.registerResourcePack(FactoryAPI.createLocation(MOD_ID, "legacy_resources"), true);
            registry.registerResourcePack(FactoryAPI.createLocation(MOD_ID, "legacy_waters"), true);
            registry.registerResourcePack(FactoryAPI.createLocation(MOD_ID, "console_aspects"), false);
            if (FactoryAPI.getLoader().isForgeLike()) {
                registry.register("programmer_art", FactoryAPI.createLocation(MOD_ID, "programmer_art"), Component.translatable("legacy.builtin.console_programmer"), Pack.Position.TOP, false);
                registry.register("high_contrast", FactoryAPI.createLocation(MOD_ID, "high_contrast"), Component.translatable("legacy.builtin.high_contrast"), Pack.Position.TOP, false);
            }
        });
        LegacyUIElementTypes.init();
        FactoryRenderStateExtension.types.add(new FactoryRenderStateExtension.Type<>(ThrownTridentRenderState.class, LoyaltyLinesRenderState::new));
        FactoryRenderStateExtension.types.add(new FactoryRenderStateExtension.Type<>(FireworkRocketRenderState.class, LegacyFireworkRenderState::new));
        FactoryRenderStateExtension.types.add(new FactoryRenderStateExtension.Type<>(LivingEntityRenderState.class, LegacyLivingEntityRenderState::new));
        FactoryRenderStateExtension.types.add(new FactoryRenderStateExtension.Type<>(VillagerRenderState.class, LegacyVillagerRenderState::new));

        FactoryAPIClient.registerRenderLayer(r -> {
            if (r.getEntityRenderer(EntityType.GHAST) instanceof GhastRenderer renderer) {
                r.register(renderer, new EyesLayer<>(renderer) {
                    @Override
                    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, GhastRenderState entityRenderState, float f, float g) {
                        if (!entityRenderState.isCharging) return;
                        super.submit(poseStack, submitNodeCollector, i, entityRenderState, f, g);
                    }

                    @Override
                    public RenderType renderType() {
                        return GHAST_SHOOTING_GLOW;
                    }
                });
            }
            if (r.getEntityRenderer(EntityType.DROWNED) instanceof DrownedRenderer renderer) {
                r.register(renderer, new EyesLayer<>(renderer) {
                    @Override
                    public RenderType renderType() {
                        return DROWNED_GLOW;
                    }
                });
            }
        });
        //? if neoforge {
        /*FactoryAPIPlatform.getModEventBus().addListener(EventPriority.NORMAL,false, RegisterPresetEditorsEvent.class, e->Legacy4JClient.VANILLA_PRESET_EDITORS.forEach(((o, presetEditor) -> o.ifPresent(worldPresetResourceKey -> e.register(worldPresetResourceKey, presetEditor)))));
         *///?}
        //? if forge {
        /*RegisterPresetEditorsEvent.getBus(FactoryAPIPlatform.getModEventBus()).addListener(e-> Legacy4JClient.VANILLA_PRESET_EDITORS.forEach(((o, presetEditor) -> o.ifPresent(worldPresetResourceKey -> e.register(worldPresetResourceKey, presetEditor)))));
         *///?}
        FactoryAPIClient.PlayerEvent.DISCONNECTED_EVENT.register(p -> {
            PackAlbum.applyDefaultResourceAlbum();
            TopMessage.setSmall(null);
            TopMessage.setMedium(null);
        });
        FactoryAPIClient.registerConfigScreen(FactoryAPIPlatform.getModInfo(MOD_ID), Legacy4JSettingsScreen::new);
        FactoryAPIClient.registerDefaultConfigScreen("minecraft", s -> new OptionsScreen(s, Minecraft.getInstance().options));
    }

    public static void updateChunks() {
        FactoryAPIClient.SECURE_EXECUTOR.execute(() -> Minecraft.getInstance().levelRenderer.allChanged());
    }

    public static void updateSkyShape() {
        Minecraft.getInstance().execute(() -> ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).updateSkyBuffers());
    }

    public static void buildLegacySkyDisc(VertexConsumer consumer, float f) {
        for (int k = -384; k <= 384; k += 64) {
            for (int l = -256; l <= 384; l += 64) {
                float g = (float) k;
                float f1 = (float) (k + 64);
                consumer.addVertex(g, f, l);
                consumer.addVertex(f1, f, l);
                consumer.addVertex(f1, f, l + 64);
                consumer.addVertex(g, f, l + 64);
            }
        }
    }

    public static BlockStateModel getFastLeavesModelReplacement(BlockGetter blockGetter, BlockPos pos, BlockState blockState, /*? if <1.21.5 {*//*BakedModel*//*?} else {*/BlockStateModel/*?}*/ model) {
        boolean fastGraphics = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        if (LegacyOptions.fastLeavesCustomModels.get() && blockState.getBlock() instanceof LeavesBlock && fastLeavesModels.containsKey(blockState.getBlock()) && (fastGraphics || LegacyOptions.fastLeavesWhenBlocked.get())) {
            if (!fastGraphics && blockGetter != null) {
                for (Direction value : Direction.values()) {
                    BlockPos relative = pos.relative(value);
                    BlockState relativeBlockState = blockGetter.getBlockState(relative);
                    if (!(relativeBlockState.getBlock() instanceof LeavesBlock) && !relativeBlockState.isSolidRender(/*? if <1.21.2 {*//*blockGetter, relative*//*?}*/)) {
                        return model;
                    }
                }
            }
            return FactoryAPIClient.getExtraModel(fastLeavesModels.get(blockState.getBlock()));
        }
        return model;
    }

    public static boolean hasModOnServer() {
        return FactoryAPIClient.hasModOnServer(MOD_ID);
    }

    public static int getEffectiveRenderDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance();
    }

    public static void onClientPlayerInfoChange() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof HostOptionsScreen s) s.reloadPlayerButtons();
        else if (minecraft.screen instanceof LeaderboardsScreen s) {
            s.rebuildRenderableVList(minecraft);
            s.repositionElements();
        }
    }

    public static void resetOptions(Minecraft minecraft) {
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

    public static String manageAvailableSaveDirName(Consumer<File> copy, Predicate<String> exists, LevelStorageSource source, String levelId) {
        String destId = manageAvailableName(exists, levelId);
        copy.accept(source.getBaseDir().resolve(destId).toFile());
        return destId;
    }

    public static String manageAvailableName(Predicate<String> exists, String saveDirName) {
        StringBuilder builder = new StringBuilder(saveDirName);
        int repeat = 0;
        while (exists.test(builder + (repeat > 0 ? String.format(" (%s)", repeat) : "")))
            repeat++;
        if (repeat > 0)
            builder.append(String.format(" (%s)", repeat));
        return builder.toString();
    }

}