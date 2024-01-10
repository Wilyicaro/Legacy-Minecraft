package wily.legacy.client.screen;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.ConfirmExperimentalFeaturesScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.server.LanServerPinger;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Environment(value=EnvType.CLIENT)
public class LegacyCreateWorldScreen extends PanelBackgroundScreen{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMP_WORLD_PREFIX = "mcworld-";
    static final Component GAME_MODEL_LABEL = Component.translatable("selectWorld.gameMode");
    static final Component NAME_LABEL = Component.translatable("selectWorld.enterName");
    private static final Component PREPARING_WORLD_DATA = Component.translatable("createWorld.preparing");
    private boolean recreated;
    protected boolean onlineOnStart = false;
    private int port = HttpUtil.getAvailablePort();
    protected PackSelector resourcePackSelector = PackSelector.resources(panel.x + 13, panel.y + 106, 220,45);

    protected final boolean onlyLoad;
    private Path tempDataPackDir;
    private PackRepository tempDataPackRepository;

    protected final WorldCreationUiState uiState;

    public static void openFresh(Minecraft minecraft, @Nullable Screen screen) {
        PackRepository packRepository = new PackRepository(new ServerPacksSource(minecraft.directoryValidator()));
        WorldLoader.InitConfig initConfig = LegacyCreateWorldScreen.createDefaultLoadConfig(packRepository, WorldDataConfiguration.DEFAULT);

        CompletableFuture<WorldCreationContext> completableFuture = WorldLoader.load(initConfig, dataLoadContext -> new WorldLoader.DataLoadOutput<>(new DataPackReloadCookie(new WorldGenSettings(WorldOptions.defaultWithRandomSeed(), WorldPresets.createNormalWorldDimensions(dataLoadContext.datapackWorldgen())), dataLoadContext.dataConfiguration()), dataLoadContext.datapackDimensions()), (closeableResourceManager, reloadableServerResources, layeredRegistryAccess, dataPackReloadCookie) -> {
            closeableResourceManager.close();
            return new WorldCreationContext(dataPackReloadCookie.worldGenSettings(), layeredRegistryAccess, reloadableServerResources, dataPackReloadCookie.dataConfiguration());
        }, Util.backgroundExecutor(), minecraft);

        minecraft.managedBlock(completableFuture::isDone);
        minecraft.setScreen(new LegacyCreateWorldScreen(minecraft, screen, completableFuture.join(), Optional.of(WorldPresets.NORMAL), OptionalLong.empty(),false));
    }
    public LegacyPresetEditor getPresetEditor() {
        Holder<WorldPreset> holder = uiState.getWorldType().preset();
        return holder != null ? LegacyPresetEditor.EDITORS.get(holder.unwrapKey()) : null;
    }
    public static LegacyCreateWorldScreen createFromExisting(Minecraft minecraft, @Nullable Screen screen, LevelSettings levelSettings, WorldCreationContext worldCreationContext, @Nullable Path path, boolean onlyLoad) {
        LegacyCreateWorldScreen createWorldScreen = new LegacyCreateWorldScreen(minecraft, screen, worldCreationContext, WorldPresets.fromSettings(worldCreationContext.selectedDimensions().dimensions()), OptionalLong.of(worldCreationContext.options().seed()), onlyLoad);
        createWorldScreen.recreated = true;

        createWorldScreen.getUiState().setName(levelSettings.levelName());
        createWorldScreen.getUiState().setAllowCheats(levelSettings.allowCommands());
        createWorldScreen.getUiState().setDifficulty(levelSettings.difficulty());
        createWorldScreen.getUiState().getGameRules().assignFrom(levelSettings.gameRules(), null);
        if (levelSettings.hardcore()) {
            createWorldScreen.getUiState().setGameMode(WorldCreationUiState.SelectedGameMode.HARDCORE);
        } else if (levelSettings.gameType().isSurvival()) {
            createWorldScreen.getUiState().setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
        } else if (levelSettings.gameType().isCreative()) {
            createWorldScreen.getUiState().setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        }
        createWorldScreen.tempDataPackDir = path;
        return createWorldScreen;
    }

    public WorldCreationUiState getUiState() {
        return uiState;
    }


    private LegacyCreateWorldScreen(Minecraft minecraft, @Nullable Screen screen, @Nullable WorldCreationContext worldCreationContext, Optional<ResourceKey<WorldPreset>> optional, OptionalLong optionalLong, boolean onlyLoad) {
        super(245,228,Component.translatable("selectWorld.create"));
        this.onlyLoad = false;
        this.uiState = new WorldCreationUiState(minecraft.getLevelSource().getBaseDir(), worldCreationContext, optional, optionalLong);
        this.parent = screen;
    }
    private Component tryParsePort(String string) {
        if (string.isBlank()) {
            this.port = HttpUtil.getAvailablePort();
            return null;
        }
        try {
            this.port = Integer.parseInt(string);
            if (this.port < 1024 || this.port > 65535) {
                return  Component.translatable("lanServer.port.invalid.new", 1024, 65535);
            }
            if (!HttpUtil.isPortAvailable(this.port)) {
                return Component.translatable("lanServer.port.unavailable.new", 1024, 65535);
            }
            return null;
        } catch (NumberFormatException numberFormatException) {
            this.port = HttpUtil.getAvailablePort();
            return  Component.translatable("lanServer.port.invalid.new", 1024, 65535);
        }
    }

    @Override
    protected void init() {
        panel.init();
        LOGGER.warn(width + "/" + height);
        addRenderableOnly(panel);
        EditBox nameEdit = new EditBox(font, panel.x + 13, panel.y + 25,220, 20, Component.translatable("selectWorld.enterName"));
        nameEdit.setValue(getUiState().getName());
        nameEdit.setResponder(getUiState()::setName);
        getUiState().addListener(worldCreationUiState -> nameEdit.setTooltip(Tooltip.create(Component.translatable("selectWorld.targetFolder", Component.literal(worldCreationUiState.getTargetFolder()).withStyle(ChatFormatting.ITALIC)))));
        LegacyCreateWorldScreen.this.setInitialFocus(nameEdit);
        addRenderableWidget(nameEdit);
        LegacySliderButton<WorldCreationUiState.SelectedGameMode> gameModeButton = addRenderableWidget(new LegacySliderButton<>(panel.x + 13, panel.y + 51, 220,16, b -> b.getDefaultMessage(GAME_MODEL_LABEL,b.getValue().displayName),()->Tooltip.create(getUiState().getGameMode().getInfo()),getUiState().getGameMode(),()->List.of(WorldCreationUiState.SelectedGameMode.SURVIVAL, WorldCreationUiState.SelectedGameMode.HARDCORE, WorldCreationUiState.SelectedGameMode.CREATIVE), b->LegacyCreateWorldScreen.this.getUiState().setGameMode(b.objectValue)));
        getUiState().addListener(worldCreationUiState -> gameModeButton.active = !worldCreationUiState.isDebug());
        LegacySliderButton<Difficulty> difficultyButton = addRenderableWidget(new LegacySliderButton<>(panel.x + 13, panel.y + 77, 220,16, b -> b.getDefaultMessage(Component.translatable("options.difficulty"),b.getValue().getDisplayName()),()->Tooltip.create(getUiState().getDifficulty().getInfo()),getUiState().getDifficulty(),()-> Arrays.asList(Difficulty.values()), b->LegacyCreateWorldScreen.this.getUiState().setDifficulty(b.objectValue)));
        getUiState().addListener(worldCreationUiState -> difficultyButton.active = !getUiState().isHardcore());
        if (!SharedConstants.getCurrentVersion().isStable())
            addRenderableWidget(Button.builder(ExperimentsScreen.EXPERIMENTS_LABEL, button -> LegacyCreateWorldScreen.this.openExperimentsScreen(this,getUiState().getSettings().dataConfiguration())).bounds(panel.x + 13, panel.y + 129,220,20).build());
        EditBox portEdit = addRenderableWidget(new EditBox(minecraft.font, panel.x + 124, panel.y + 151,100,20,Component.translatable("lanServer.port")));
        portEdit.visible = onlineOnStart;

        portEdit.setHint(Component.literal("" + this.port).withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(Button.builder(Component.translatable( "createWorld.tab.more.title"), button -> minecraft.setScreen(new WorldMoreOptionsScreen(this))).bounds(panel.x + 13, panel.y + 172,220,20).build());
        Button createButton = addRenderableWidget(Button.builder(Component.translatable("selectWorld.create"), button -> this.onCreate()).bounds(panel.x + 13, panel.y + 197,220,20).build());
        addRenderableWidget(new TickBox(panel.x+ 14, panel.y+155,100,onlineOnStart, b-> Component.translatable("menu.shareToLan"), b->null, button -> {
            if (!(portEdit.visible = onlineOnStart = button.selected)) {
                createButton.active = true;
                portEdit.setValue("");
            }
        }));
        portEdit.setResponder(string -> {
            Component component = tryParsePort(string);
            portEdit.setHint(Component.literal("" + this.port).withStyle(ChatFormatting.DARK_GRAY));
            if (component == null) {
                portEdit.setTextColor(0xE0E0E0);
                portEdit.setTooltip(null);
                createButton.active = true;
            } else {
                portEdit.setTextColor(0xFF5555);
                portEdit.setTooltip(Tooltip.create(component));
                createButton.active = false;
            }
        });
        resourcePackSelector.setX(panel.x + 13);
        resourcePackSelector.setY(panel.y + 106);
        addRenderableWidget(resourcePackSelector);
        this.getUiState().onChanged();
    }

    @Override
    public void repositionElements() {
        rebuildWidgets();
    }

    private static void queueLoadScreen(Minecraft minecraft, Component component) {
        minecraft.forceSetScreen(new GenericDirtMessageScreen(component));
    }

    private void onCreate() {
        WorldCreationContext worldCreationContext = this.getUiState().getSettings();
        WorldDimensions.Complete complete = worldCreationContext.selectedDimensions().bake(worldCreationContext.datapackDimensions());
        LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess = worldCreationContext.worldgenRegistries().replaceFrom(RegistryLayer.DIMENSIONS, complete.dimensionsRegistryAccess());
        Lifecycle lifecycle = FeatureFlags.isExperimental(worldCreationContext.dataConfiguration().enabledFeatures()) ? Lifecycle.experimental() : Lifecycle.stable();
        Lifecycle lifecycle2 = layeredRegistryAccess.compositeAccess().allRegistriesLifecycle();
        Lifecycle lifecycle3 = lifecycle2.add(lifecycle);
        boolean bl = !this.recreated && lifecycle2 == Lifecycle.stable();
        confirmWorldCreation(this.minecraft, this, lifecycle3, () -> {
            try {
                this.createNewWorld(complete.specialWorldProperty(), layeredRegistryAccess, lifecycle3);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, bl);
        onLoad();
    }
    private void onLoad() {
        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer().isReady()){
            if (onlineOnStart) {
                MutableComponent component = publishUnloadedServer(minecraft, getUiState().getGameMode().gameType, getUiState().isAllowCheats(), this.port) ? PublishCommand.getSuccessMessage(this.port) : Component.translatable("commands.publish.failed");
                this.minecraft.gui.getChat().addMessage(component);
            }
        }
        resourcePackSelector.applyChanges(true);
    }
    public static void confirmWorldCreation(Minecraft minecraft, LegacyCreateWorldScreen createWorldScreen, Lifecycle lifecycle, Runnable runnable, boolean bl2) {
        if (bl2 || lifecycle == Lifecycle.stable()) {
            runnable.run();
        } else if (lifecycle == Lifecycle.experimental()) {
            minecraft.setScreen(new ConfirmationScreen(createWorldScreen, Component.translatable("selectWorld.warning.experimental.title"), Component.translatable("selectWorld.warning.experimental.question"),b->runnable.run()));
        } else {
            minecraft.setScreen(new ConfirmationScreen(createWorldScreen, Component.translatable("selectWorld.warning.deprecated.title"), Component.translatable("selectWorld.warning.deprecated.question"),b->runnable.run()));
        }
    }
    private void createNewWorld(PrimaryLevelData.SpecialWorldProperty specialWorldProperty, LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess, Lifecycle lifecycle) throws IOException {
        LegacyCreateWorldScreen.queueLoadScreen(this.minecraft, PREPARING_WORLD_DATA);
        Optional<LevelStorageSource.LevelStorageAccess> optional = this.createNewWorldDirectory();
        if (optional.isEmpty()) {
            return;
        }
        this.removeTempDataPackDir();
        boolean bl = specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.DEBUG;
        WorldCreationContext worldCreationContext = this.getUiState().getSettings();
        LevelSettings levelSettings = this.createLevelSettings(bl);
        PrimaryLevelData worldData = new PrimaryLevelData(levelSettings, worldCreationContext.options(), specialWorldProperty, lifecycle);
        this.minecraft.createWorldOpenFlows().createLevelFromExistingSettings(optional.get(), worldCreationContext.dataPackResources(), layeredRegistryAccess, worldData);
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
            LOGGER.info("Started serving on {}", i);
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
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics,false);
    }
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics, i, j, f);
        for (Renderable renderable : this.renderables)
            renderable.render(guiGraphics, i, j, f);
        guiGraphics.drawString(font,NAME_LABEL, panel.x + 14, panel.y + 15, 0x404040,false);
    }

    private LevelSettings createLevelSettings(boolean bl) {
        String string = this.getUiState().getName().trim();
        if (bl) {
            GameRules gameRules = new GameRules();
            gameRules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            return new LevelSettings(string, GameType.SPECTATOR, false, Difficulty.PEACEFUL, true, gameRules, WorldDataConfiguration.DEFAULT);
        }
        return new LevelSettings(string, this.getUiState().getGameMode().gameType, this.getUiState().isHardcore(), this.getUiState().getDifficulty(), this.getUiState().isAllowCheats(), this.getUiState().getGameRules(), this.getUiState().getSettings().dataConfiguration());
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (super.keyPressed(i, j, k)) {
            return true;
        }
        if (i == 257 || i == 335) {
            this.onCreate();
            return true;
        }
        return false;
    }




    @Nullable
    private Path getTempDataPackDir() {
        if (this.tempDataPackDir == null) {
            try {
                this.tempDataPackDir = Files.createTempDirectory(TEMP_WORLD_PREFIX);
            } catch (IOException iOException) {
                LOGGER.warn("Failed to create temporary dir", iOException);
                SystemToast.onPackCopyFailure(this.minecraft, this.getUiState().getTargetFolder());
                this.popScreen();
            }
        }
        return this.tempDataPackDir;
    }

    void openExperimentsScreen(Screen screen, WorldDataConfiguration worldDataConfiguration) {
        Pair<Path, PackRepository> pair = this.getDataPackSelectionSettings(worldDataConfiguration);
        if (pair != null) {
            this.minecraft.setScreen(new ExperimentsScreen(screen, pair.getSecond(), packRepository -> this.tryApplyNewDataPacks(packRepository, false, w-> openExperimentsScreen(screen,w))));
        }
    }

    void openDataPackSelectionScreen(WorldDataConfiguration worldDataConfiguration) {
        Pair<Path, PackRepository> pair = this.getDataPackSelectionSettings(worldDataConfiguration);
        if (pair != null) {
            this.minecraft.setScreen(new PackSelectionScreen(pair.getSecond(), packRepository -> this.tryApplyNewDataPacks(packRepository, true, this::openDataPackSelectionScreen), pair.getFirst(), Component.translatable("dataPack.title")));
        }
    }

    public void tryApplyNewDataPacks(PackRepository packRepository, boolean bl2, Consumer<WorldDataConfiguration> consumer) {
        ImmutableList<String> list = ImmutableList.copyOf(packRepository.getSelectedIds());
        WorldDataConfiguration worldDataConfiguration = new WorldDataConfiguration(new DataPackConfig(list, (List)packRepository.getAvailableIds().stream().filter(string -> !list.contains(string)).collect(ImmutableList.toImmutableList())), this.getUiState().getSettings().dataConfiguration().enabledFeatures());
        if (this.getUiState().tryUpdateDataConfiguration(worldDataConfiguration)) {
            this.minecraft.setScreen(this);
            return;
        }
        FeatureFlagSet featureFlagSet = packRepository.getRequestedFeatureFlags();
        if (FeatureFlags.isExperimental(featureFlagSet) && bl2) {
            this.minecraft.setScreen(new ConfirmExperimentalFeaturesScreen(packRepository.getSelectedPacks(), bl -> {
                if (bl) {
                    this.applyNewPackConfig(packRepository, worldDataConfiguration, consumer);
                } else {
                    consumer.accept(this.getUiState().getSettings().dataConfiguration());
                }
            }));
        } else {
            this.applyNewPackConfig(packRepository, worldDataConfiguration, consumer);
        }
    }

    private void applyNewPackConfig(PackRepository packRepository, WorldDataConfiguration worldDataConfiguration, Consumer<WorldDataConfiguration> consumer) {
        this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("dataPack.validation.working")));
        WorldLoader.InitConfig initConfig = LegacyCreateWorldScreen.createDefaultLoadConfig(packRepository, worldDataConfiguration);
        WorldLoader.load(initConfig, dataLoadContext -> {
            if (dataLoadContext.datapackWorldgen().registryOrThrow(Registries.WORLD_PRESET).size() == 0) {
                throw new IllegalStateException("Needs at least one world preset to continue");
            }
            if (dataLoadContext.datapackWorldgen().registryOrThrow(Registries.BIOME).size() == 0) {
                throw new IllegalStateException("Needs at least one biome continue");
            }
            WorldCreationContext worldCreationContext = this.getUiState().getSettings();
            RegistryOps<JsonElement> dynamicOps = RegistryOps.create(JsonOps.INSTANCE, worldCreationContext.worldgenLoadContext());
            DataResult<JsonElement> dataResult = WorldGenSettings.encode(dynamicOps, worldCreationContext.options(), worldCreationContext.selectedDimensions()).setLifecycle(Lifecycle.stable());
            RegistryOps<JsonElement> dynamicOps2 = RegistryOps.create(JsonOps.INSTANCE, dataLoadContext.datapackWorldgen());
            WorldGenSettings worldGenSettings = dataResult.flatMap(jsonElement -> WorldGenSettings.CODEC.parse(dynamicOps2, jsonElement)).getOrThrow(false, Util.prefix("Error parsing worldgen settings after loading data packs: ", LOGGER::error));
            return new WorldLoader.DataLoadOutput<>(new DataPackReloadCookie(worldGenSettings, dataLoadContext.dataConfiguration()), dataLoadContext.datapackDimensions());
        }, (closeableResourceManager, reloadableServerResources, layeredRegistryAccess, dataPackReloadCookie) -> {
            closeableResourceManager.close();
            return new WorldCreationContext(dataPackReloadCookie.worldGenSettings(), layeredRegistryAccess, reloadableServerResources, dataPackReloadCookie.dataConfiguration());
        }, Util.backgroundExecutor(), this.minecraft).thenAcceptAsync(this.getUiState()::setSettings, this.minecraft).handle((void_, throwable) -> {
            if (throwable != null) {
                LOGGER.warn("Failed to validate datapack", (Throwable)throwable);
                this.minecraft.setScreen(new ConfirmScreen(bl -> {
                    if (bl) {
                        consumer.accept(this.getUiState().getSettings().dataConfiguration());
                    } else {
                        consumer.accept(WorldDataConfiguration.DEFAULT);
                    }
                }, Component.translatable("dataPack.validation.failed"), CommonComponents.EMPTY, Component.translatable("dataPack.validation.back"), Component.translatable("dataPack.validation.reset")));
            } else {
                this.minecraft.setScreen(this);
            }
            return null;
        });
    }

    private static WorldLoader.InitConfig createDefaultLoadConfig(PackRepository packRepository, WorldDataConfiguration worldDataConfiguration) {
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, worldDataConfiguration, false, true);
        return new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, 2);
    }

    private void removeTempDataPackDir() {
        if (this.tempDataPackDir != null) {
            try (Stream<Path> stream = Files.walk(this.tempDataPackDir);){
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException iOException) {
                        LOGGER.warn("Failed to remove temporary file {}", path, iOException);
                    }
                });
            } catch (IOException iOException) {
                LOGGER.warn("Failed to list temporary dir {}", this.tempDataPackDir);
            }
            this.tempDataPackDir = null;
        }
    }

    private static void copyBetweenDirs(Path path, Path path2, Path path3) {
        try {
            Util.copyBetweenDirs(path, path2, path3);
        } catch (IOException iOException) {
            LOGGER.warn("Failed to copy datapack file from {} to {}", path3, path2);
            throw new UncheckedIOException(iOException);
        }
    }

    private Optional<LevelStorageSource.LevelStorageAccess> createNewWorldDirectory() throws IOException {
        String string;
        block12: {
            LevelStorageSource.LevelStorageAccess levelStorageAccess;
            block11: {
                string = this.getUiState().getTargetFolder();
                levelStorageAccess = this.minecraft.getLevelSource().createAccess(string);
                if (this.tempDataPackDir != null) break block11;
                return Optional.of(levelStorageAccess);
            }
            Stream<Path> stream = Files.walk(this.tempDataPackDir);
            try {
                Path path3 = levelStorageAccess.getLevelPath(LevelResource.DATAPACK_DIR);
                FileUtil.createDirectoriesSafe(path3);
                stream.filter(path -> !path.equals(this.tempDataPackDir)).forEach(path2 -> copyBetweenDirs(this.tempDataPackDir, path3, path2));
                if (stream == null) break block12;
            } catch (Throwable throwable) {
                try {
                    try {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    } catch (IOException | UncheckedIOException exception) {
                        LOGGER.warn("Failed to copy datapacks to world {}", string, exception);
                        levelStorageAccess.close();
                    }
                } catch (IOException | UncheckedIOException exception2) {
                    LOGGER.warn("Failed to create access for {}", string, exception2);
                }
            }
            stream.close();
        }
        SystemToast.onPackCopyFailure(this.minecraft, string);
        this.popScreen();
        return Optional.empty();
    }
    public void popScreen() {
        this.minecraft.setScreen(parent);
        this.removeTempDataPackDir();
    }
    @Nullable
    public static Path createTempDataPackDirFromExistingWorld(Path path, Minecraft minecraft) {
        MutableObject mutableObject = new MutableObject();
        try (Stream<Path> stream = Files.walk(path);){
            stream.filter(path2 -> !path2.equals(path)).forEach(path2 -> {
                Path path3 = (Path)mutableObject.getValue();
                if (path3 == null) {
                    try {
                        path3 = Files.createTempDirectory(TEMP_WORLD_PREFIX);
                    } catch (IOException iOException) {
                        LOGGER.warn("Failed to create temporary dir");
                        throw new UncheckedIOException(iOException);
                    }
                    mutableObject.setValue(path3);
                }
                LegacyCreateWorldScreen.copyBetweenDirs(path, path3, path2);
            });
        } catch (IOException | UncheckedIOException exception) {
            LOGGER.warn("Failed to copy datapacks from world {}", path, exception);
            SystemToast.onPackCopyFailure(minecraft, path.toString());
            return null;
        }
        return (Path)mutableObject.getValue();
    }

    @Nullable
    public Pair<Path, PackRepository> getDataPackSelectionSettings(WorldDataConfiguration worldDataConfiguration) {
        Path path = this.getTempDataPackDir();
        if (path != null) {
            if (this.tempDataPackRepository == null) {
                this.tempDataPackRepository = ServerPacksSource.createPackRepository(path, minecraft.directoryValidator());
                this.tempDataPackRepository.reload();
            }
            this.tempDataPackRepository.setSelected(worldDataConfiguration.dataPacks().getEnabled());
            return Pair.of(path, this.tempDataPackRepository);
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    record DataPackReloadCookie(WorldGenSettings worldGenSettings, WorldDataConfiguration dataConfiguration) {
    }
}


