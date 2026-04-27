package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryConfigWidgets;
import wily.factoryapi.base.client.FactoryOptions;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.util.LegacyComponents;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.ControlTooltip.getKeyIcon;

public class OptionsScreen extends PanelVListScreen {
    private static boolean buildingMergedLegacyAdvancedOptions = false;
    public Screen advancedOptionsScreen;

    public OptionsScreen(Screen parent, Panel.Constructor<OptionsScreen> panelConstructor, Component component) {
        super(parent, panelConstructor.cast(), component);
    }

    public OptionsScreen(Screen parent, Section section) {
        this(parent, section.panelConstructor(), section.title());
        section.elements().forEach(c -> c.accept(this));
        section.advancedSection.ifPresent(s -> {
            switch (LegacyOptions.advancedOptionsMode.get()) {
                case DEFAULT -> withAdvancedOptions(s.build(this));
                case MERGE -> {
                    if (LegacyOptions.legacySettingsMenus.get()) withAdvancedOptions(buildMergedAdvancedOptionsScreen(this, section, s));
                    else s.elements().forEach(c -> c.accept(this));
                }
            }
        });
    }

    private static Screen buildMergedAdvancedOptionsScreen(Screen parent, Section baseSection, Section advancedSection) {
        List<Consumer<OptionsScreen>> mergedElements = new ArrayList<>(baseSection.elements());
        mergedElements.addAll(advancedSection.elements());
        Section mergedSection = new Section(advancedSection.title(), advancedSection.panelConstructor(), mergedElements, ArbitrarySupplier.empty(), baseSection.sectionBuilder());
        buildingMergedLegacyAdvancedOptions = true;
        try {
            return mergedSection.build(parent);
        } finally {
            buildingMergedLegacyAdvancedOptions = false;
        }
    }

    private static boolean isMergedLegacyGraphicsSection(Section section) {
        return LegacyOptions.legacySettingsMenus.get() && section.panelConstructor() == Section.ADVANCED_GRAPHICS.panelConstructor();
    }

    private static boolean useLegacySettingsMenusOptions() {
        return LegacyOptions.legacySettingsMenus.get() && !buildingMergedLegacyAdvancedOptions;
    }

    public static void setupSelectorControlTooltips(ControlTooltip.Renderer renderer, Screen screen) {
        renderer.add(() -> ControlType.getActiveType().isKbm() ? CompoundComponentIcon.of(getKeyIcon(InputConstants.KEY_LSHIFT), PLUS_ICON, getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT)) : null, () -> ControlTooltip.getKeyMessage(InputConstants.MOUSE_BUTTON_LEFT, screen));
        renderer.add(ControlTooltip.EXTRA::get, () -> ControlTooltip.getKeyMessage(InputConstants.KEY_X, screen));
        renderer.add(ControlTooltip.OPTION::get, () -> ControlTooltip.getKeyMessage(InputConstants.KEY_O, screen));
    }

    public OptionsScreen withAdvancedOptions(Function<OptionsScreen, Screen> advancedOptionsFunction) {
        return withAdvancedOptions(advancedOptionsFunction.apply(this));
    }

    public OptionsScreen withAdvancedOptions(Screen screen) {
        advancedOptionsScreen = screen;
        return this;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (super.keyPressed(keyEvent)) return true;
        if (keyEvent.key() == InputConstants.KEY_O && advancedOptionsScreen != null) {
            minecraft.setScreen(advancedOptionsScreen);
            return true;
        }
        return false;
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        if (shouldAddSelectorControlTooltips()) setupSelectorControlTooltips(renderer, this);
        if (renderer.tooltips.size() > 6) {
            renderer.replace(6, i -> i, c -> c == null && !LegacyOptions.hideAdvancedOptionsTooltip.get() && !LegacyOptions.legacySettingsMenus.get() ? advancedOptionsScreen == null ? null : LegacyComponents.SHOW_ADVANCED_OPTIONS : c);
        }
    }

    protected boolean shouldAddSelectorControlTooltips() {
        return true;
    }

    public void updateWidgets(boolean forceMessageUpdate) {
        for (Renderable renderable : getRenderableVList().renderables) {
            if (renderable instanceof LegacySliderButton<?> slider && !slider.updateValue() && forceMessageUpdate) slider.updateMessage();
            else if (renderable instanceof TickBox tickBox && !tickBox.updateValue() && forceMessageUpdate) tickBox.updateMessage();
        }
        if (advancedOptionsScreen instanceof OptionsScreen optionsScreen)
            optionsScreen.updateWidgets(forceMessageUpdate);
    }

    public void updateWidgetMessages() {
        for (Renderable renderable : getRenderableVList().renderables) {
            if (renderable instanceof LegacySliderButton<?> slider) slider.updateMessage();
            else if (renderable instanceof TickBox tickBox) tickBox.updateMessage();
        }
        if (advancedOptionsScreen instanceof OptionsScreen optionsScreen)
            optionsScreen.updateWidgetMessages();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (LegacyOptions.legacySettingsMenus.get()) guiGraphics.deferredTooltip = null;
    }

    protected int getLegacyPanelHeight(int baseHeight, boolean shrinkOnly) {
        if (!LegacyOptions.legacySettingsMenus.get()) return baseHeight;

        int contentHeight = 20;
        int entryCount = 0;
        for (Renderable renderable : getRenderableVList().renderables) {
            if (renderable instanceof LayoutElement element) {
                contentHeight += element.getHeight();
                entryCount++;
            }
        }
        if (entryCount > 1) contentHeight += (entryCount - 1) * 3;
        return shrinkOnly ? Math.min(baseHeight, contentHeight) : Math.max(baseHeight, contentHeight);
    }

    private static TickBox createRenderCloudsTickBox() {
        Minecraft minecraft = Minecraft.getInstance();
        return new TickBox(0, 0, 200, minecraft.options.cloudStatus().get() != CloudStatus.OFF,
                b -> Component.translatable("legacy.options.renderClouds"),
                b -> null,
                t -> {
                    minecraft.options.cloudStatus().set(t.selected ? CloudStatus.FANCY : CloudStatus.OFF);
                    minecraft.options.save();
                },
                () -> minecraft.options.cloudStatus().get() != CloudStatus.OFF);
    }

    private static void addDifficultyOption(OptionsScreen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null && minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            screen.renderableVList.addRenderable(new LegacySliderButton<>(0, 0, 230, 16,
                    b -> b.getDefaultMessage(Component.translatable("options.difficulty"), b.getObjectValue().getDisplayName()),
                    b -> Tooltip.create(b.getObjectValue().getInfo()),
                    minecraft.level.getDifficulty(),
                    () -> Arrays.asList(Difficulty.values()),
                    b -> minecraft.player.connection.sendCommand("difficulty " + b.getObjectValue().getKey()),
                    () -> minecraft.level.getDifficulty()));
            return;
        }
        if (minecraft.level == null && !minecraft.hasSingleplayerServer())
            screen.renderableVList.addRenderable(LegacyConfigWidgets.createWidget(LegacyOptions.createWorldDifficulty));
    }

    private static TickBox createLegacySettingsMenusTickBox(OptionsScreen screen) {
        return new TickBox(0, 0, 200,
                LegacyOptions.legacySettingsMenus.get(),
                b -> LegacyOptions.legacySettingsMenus.getDisplay().name(),
                b -> FactoryConfigWidgets.getCachedTooltip(LegacyOptions.legacySettingsMenus.getDisplay().tooltip().apply(b)),
                t -> {
                    if (!t.selected) {
                        disableLegacySettingsMenus(screen.parent);
                        return;
                    }
                    t.selected = false;
                    screen.minecraft.setScreen(createLegacySettingsMenusWarningScreen(screen));
                },
                LegacyOptions.legacySettingsMenus::get);
    }

    private static TickBox createDisplayAdvancedOptionsTooltipTickBox() {
        return new TickBox(0, 0, 200,
                !LegacyOptions.hideAdvancedOptionsTooltip.get(),
                b -> LegacyOptions.hideAdvancedOptionsTooltip.getDisplay().name(),
                b -> FactoryConfigWidgets.getCachedTooltip(LegacyOptions.hideAdvancedOptionsTooltip.getDisplay().tooltip().apply(!b)),
                t -> FactoryConfig.saveOptionAndConsume(LegacyOptions.hideAdvancedOptionsTooltip, !t.selected, v -> {}),
                () -> !LegacyOptions.hideAdvancedOptionsTooltip.get());
    }

    private static TickBox createDisplayPackManagementTooltipsTickBox() {
        return new TickBox(0, 0, 200,
                LegacyOptions.displayPackManagementTooltips.get(),
                b -> LegacyOptions.displayPackManagementTooltips.getDisplay().name(),
                b -> FactoryConfigWidgets.getCachedTooltip(LegacyOptions.displayPackManagementTooltips.getDisplay().tooltip().apply(b)),
                t -> FactoryConfig.saveOptionAndConsume(LegacyOptions.displayPackManagementTooltips, t.selected, v -> {}),
                LegacyOptions.displayPackManagementTooltips::get);
    }

    static AbstractWidget createLegacyGraphicsPresetWidget(boolean active) {
        AbstractWidget widget = LegacyConfigWidgets.createWidget(LegacyOptions.showOptionsPresetInLegacyGraphics);
        widget.active = active;
        return widget;
    }

    static ConfirmationScreen createLegacySettingsMenusWarningScreen(Screen parent, Consumer<ConfirmationScreen> okAction) {
        return new ConfirmationScreen(parent,
                ConfirmationScreen::getPanelWidth,
                () -> ConfirmationScreen.getBaseHeight() + (LegacyOptions.getUIMode().isSD() ? 19 : 22),
                Component.translatable("legacy.menu.legacy_settings_menus_warning"),
                Component.translatable("legacy.menu.legacy_settings_menus_warning.message"),
                okAction) {
            @Override
            protected void addButtons() {
                renderableVList.addRenderable(createLegacyGraphicsPresetWidget(true));
                super.addButtons();
            }
        };
    }

    private static ConfirmationScreen createLegacySettingsMenusWarningScreen(OptionsScreen screen) {
        return createLegacySettingsMenusWarningScreen(screen, s -> enableLegacySettingsMenus(screen.parent));
    }

    static void enableLegacySettingsMenus(Screen screen) {
        FactoryConfig.saveOptionAndConsume(LegacyOptions.displayChatIndicators, false, v ->
                FactoryConfig.saveOptionAndConsume(LegacyOptions.displayPackManagementTooltips, false, v1 ->
                        FactoryConfig.saveOptionAndConsume(LegacyOptions.advancedOptionsMode, LegacyOptions.AdvancedOptionsMode.MERGE, v2 ->
                                FactoryConfig.saveOptionAndConsume(LegacyOptions.legacySettingsMenus, true, v3 -> reopenLegacySettingsMenusScreen(screen)))));
    }

    static void disableLegacySettingsMenus(Screen screen) {
        FactoryConfig.saveOptionAndConsume(LegacyOptions.legacySettingsMenus, false, v -> {
            if (LegacyOptions.advancedOptionsMode.get() == LegacyOptions.AdvancedOptionsMode.MERGE) {
                FactoryConfig.saveOptionAndConsume(LegacyOptions.advancedOptionsMode, LegacyOptions.AdvancedOptionsMode.DEFAULT, v1 -> reopenLegacySettingsMenusScreen(screen));
                return;
            }
            reopenLegacySettingsMenusScreen(screen);
        });
    }

    private static boolean shouldShowLegacyGraphicsPresetInGraphics() {
        return useLegacySettingsMenusOptions() && LegacyOptions.showOptionsPresetInLegacyGraphics.get();
    }

    private static void reopenLegacySettingsMenusScreen(Screen screen) {
        Minecraft.getInstance().setScreen(Section.ADVANCED_USER_INTERFACE.build(refreshLegacySettingsParent(screen)));
    }

    private static Screen refreshLegacySettingsParent(Screen screen) {
        if (screen instanceof SettingsScreen settingsScreen) return new SettingsScreen(refreshLegacySettingsParent(settingsScreen.parent));
        if (screen instanceof HelpAndOptionsScreen helpAndOptionsScreen) return new HelpAndOptionsScreen(refreshLegacySettingsParent(helpAndOptionsScreen.parent));
        if (screen instanceof TitleScreen) return new TitleScreen();
        return screen;
    }

    private static void showOptionsPresetWarningIfNeeded(Screen parent, Minecraft minecraft) {
        if (!LegacyOptions.optionsPreset.get().isNone() && !LegacyOptions.optionsPreset.get().get().isApplied()) {
            minecraft.setScreen(new OptionsPresetScreen(parent, LegacyOptions.optionsPreset.get().get()));
        }
    }

    public record Section(Component title, Panel.Constructor<OptionsScreen> panelConstructor,
                          List<Consumer<OptionsScreen>> elements, ArbitrarySupplier<Section> advancedSection,
                          BiFunction<Screen, Section, OptionsScreen> sectionBuilder) implements ScreenSection<OptionsScreen> {
        public static final List<Section> list = new ArrayList<>();
        private static final Minecraft mc = Minecraft.getInstance();

        public Section(Component title, Panel.Constructor<OptionsScreen> panelConstructor, List<Consumer<OptionsScreen>> elements, ArbitrarySupplier<Section> advancedSection) {
            this(title, panelConstructor, elements, advancedSection, OptionsScreen::new);
        }

        public Section(Component title, Panel.Constructor<OptionsScreen> panelConstructor, List<Consumer<OptionsScreen>> elements) {
            this(title, panelConstructor, elements, ArbitrarySupplier.empty());
        }

        public Section(Component title, Panel.Constructor<OptionsScreen> panelConstructor, ArbitrarySupplier<Section> advancedSection, FactoryConfig<?>... options) {
            this(title, panelConstructor, new ArrayList<>(List.of(o -> o.renderableVList.addOptions(options))), advancedSection);
        }

        public Section(Component title, Panel.Constructor<OptionsScreen> panelConstructor, FactoryConfig<?>... optionInstances) {
            this(title, panelConstructor, ArbitrarySupplier.empty(), optionInstances);
        }

        public static OptionInstance<?> createResolutionOptionInstance(OptionsScreen screen) {
            Monitor monitor = mc.getWindow().findBestMonitor();
            int j = monitor == null ? -1 : mc.getWindow().getPreferredFullscreenVideoMode().map(monitor::getVideoModeIndex).orElse(-1);
            return new OptionInstance<>("options.fullscreen.resolution", OptionInstance.noTooltip(), (component, integer) -> {
                if (monitor == null)
                    return Component.translatable("options.fullscreen.unavailable");
                else if (integer == -1) {
                    return Options.genericValueLabel(component, Component.translatable("options.fullscreen.current"));
                }
                VideoMode videoMode = monitor.getMode(integer);
                return Options.genericValueLabel(component, Component.translatable("options.fullscreen.entry", videoMode.getWidth(), videoMode.getHeight(), videoMode.getRefreshRate(), videoMode.getRedBits() + videoMode.getGreenBits() + videoMode.getBlueBits()));
            }, new OptionInstance.IntRange(-1, monitor != null ? monitor.getModeCount() - 1 : -1), j, integer -> {
                if (monitor == null)
                    return;
                mc.getWindow().setPreferredFullscreenVideoMode(integer == -1 ? Optional.empty() : Optional.of(monitor.getMode(integer)));
                FactoryAPIClient.SECURE_EXECUTOR.executeNowIfPossible(mc.getWindow()::changeFullscreenVideoMode, () -> screen != mc.screen);
            });
        }

        public static Section add(Section section) {
            list.add(section);
            return section;
        }

        public OptionsScreen build(Screen parent) {
            return sectionBuilder.apply(parent, this);
        }

        public static final Section GAME_OPTIONS = add(new Section(
                Component.translatable("legacy.menu.game_options"),
                s -> LegacyOptions.legacySettingsMenus.get()
                        ? Panel.createPanel(s,
                                p -> p.appearance(250, ((OptionsScreen) s).getLegacyPanelHeight(162, false)),
                                p -> p.pos(p.centeredLeftPos(s), (s.height - 162) / 2 + 12))
                        : Panel.centered(s, 250, 162),
                new ArrayList<>(List.of(
                        o -> {
                            if (useLegacySettingsMenusOptions()) o.renderableVList.addOptions(
                                    LegacyOptions.of(mc.options.autoJump()),
                                    LegacyOptions.of(mc.options.bobView()),
                                    LegacyOptions.flyingViewRolling,
                                    LegacyOptions.hints,
                                    LegacyOptions.deathMessages);
                            else o.renderableVList.addOptions(
                                    LegacyOptions.of(mc.options.autoJump()),
                                    LegacyOptions.of(mc.options.bobView()),
                                    LegacyOptions.flyingViewRolling,
                                    LegacyOptions.hints,
                                    LegacyOptions.autoSaveInterval);
                        },
                        o -> {
                            if (useLegacySettingsMenusOptions()) {
                                o.renderableVList.addRenderable(
                                        RenderableVListScreen.openScreenButton(Component.translatable("options.language"), () -> new LegacyLanguageScreen(o, mc.getLanguageManager())).build());
                            } else if (mc.level == null && !mc.hasSingleplayerServer()) {
                                o.renderableVList.addOptions(LegacyOptions.createWorldDifficulty);
                            }
                        },
                        o -> {
                            if (useLegacySettingsMenusOptions()) {
                                o.renderableVList.addOptions(
                                        LegacyOptions.autoSaveInterval,
                                        LegacyOptions.combinedLookSensitivity());
                            } else {
                                o.renderableVList.addRenderables(
                                        RenderableVListScreen.openScreenButton(Component.translatable("options.language"), () -> new LegacyLanguageScreen(o, mc.getLanguageManager())).build(),
                                        RenderableVListScreen.openScreenButton(Component.translatable("legacy.menu.mods"), () -> new ModsScreen(o)).build());
                            }
                        },
                        o -> {
                            if (useLegacySettingsMenusOptions()) addDifficultyOption(o);
                        })),
                () -> Section.ADVANCED_GAME_OPTIONS));
        public static final Section ADVANCED_GAME_OPTIONS = new Section(
                Component.translatable("legacy.menu.settings.advanced_options", GAME_OPTIONS.title()),
                s -> Panel.centered(s, 250, 172),
                new ArrayList<>(List.of(
                        o -> o.renderableVList.addOptionsCategory(
                                Component.translatable("legacy.menu.in_game_settings"),
                                LegacyOptions.unfocusedInputs,
                                LegacyOptions.invertedFrontCameraPitch,
                                LegacyOptions.headFollowsTheCamera,
                                LegacyOptions.vehicleCameraRotation,
                                LegacyOptions.create(mc.options.rotateWithMinecart()),
                                LegacyOptions.legacyCreativeBlockPlacing,
                                LegacyOptions.mapsWithCoords,
                                LegacyOptions.vanillaTutorial,
                                LegacyOptions.forceLegacyFlight,
                                LegacyOptions.forceLegacySwimming),
                        o -> {
                            if (mc.level == null)
                                LegacyCommonOptions.COMMON_STORAGE.configMap.values().forEach(c -> o.renderableVList.addRenderable(LegacyConfigWidgets.createWidget(c, b -> c.sync())));
                        },
                        o -> o.renderableVList.addOptionsCategory(
                                Component.translatable("legacy.menu.user_interface_settings"),
                                LegacyOptions.skipIntro,
                                LegacyOptions.skipInitialSaveWarning,
                                LegacyOptions.lockControlTypeChange,
                                LegacyOptions.selectedControlType,
                                LegacyOptions.cursorMode,
                                LegacyOptions.defaultShowCraftableRecipes),
                        o -> o.renderableVList.addOptionsCategory(
                                Component.translatable("options.accessibility.title"),
                                LegacyOptions.of(mc.options.notificationDisplayTime()),
                                LegacyOptions.of(mc.options.panoramaSpeed()),
                                LegacyOptions.of(mc.options.narrator()),
                                LegacyOptions.of(mc.options.narratorHotkey()),
                                LegacyOptions.of(mc.options.darkMojangStudiosBackground()),
                                LegacyOptions.of(mc.options.highContrast()),
                                LegacyOptions.of(mc.options.hideLightningFlash()),
                                LegacyOptions.of(mc.options.damageTiltStrength()),
                                LegacyOptions.of(mc.options.screenEffectScale()),
                                LegacyOptions.of(mc.options.fovEffectScale()),
                                LegacyOptions.of(mc.options.darknessEffectScale()),
                                LegacyOptions.of(mc.options.glintSpeed()),
                                LegacyOptions.of(mc.options.glintStrength())),
                        o -> o.renderableVList.addOptionsCategory(
                                Component.translatable("legacy.menu.save_settings"),
                                LegacyOptions.autoSaveWhenPaused,
                                LegacyOptions.directSaveLoad,
                                LegacyOptions.saveCache),
                        o -> o.renderableVList.addOptionsCategory(
                                Component.translatable("legacy.menu.misc"),
                                LegacyOptions.of(mc.options.realmsNotifications()),
                                LegacyOptions.of(mc.options.allowServerListing())),
                        o -> o.renderableVList.addRenderables(
                                RenderableVListScreen.openScreenButton(LegacyComponents.RESET_KNOWN_BLOCKS_TITLE, () -> ConfirmationScreen.createResetKnownListingScreen(o, LegacyComponents.RESET_KNOWN_BLOCKS_TITLE, LegacyComponents.RESET_KNOWN_BLOCKS_MESSAGE, Legacy4JClient.knownBlocks)).build(),
                                RenderableVListScreen.openScreenButton(LegacyComponents.RESET_KNOWN_ENTITIES_TITLE, () -> ConfirmationScreen.createResetKnownListingScreen(o, LegacyComponents.RESET_KNOWN_ENTITIES_TITLE, LegacyComponents.RESET_KNOWN_ENTITIES_MESSAGE, Legacy4JClient.knownEntities)).build()))));
        public static final Section AUDIO = add(new Section(
                Component.translatable("legacy.menu.audio"),
                s -> Panel.centered(s, 250, 88, 0, -12),
                new ArrayList<>(List.of(
                        o -> o.renderableVList.addOptions(
                                LegacyOptions.ofSound(mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC), "soundCategory.music"),
                                LegacyOptions.ofSound(mc.options.getSoundSourceOptionInstance(SoundSource.MASTER), "legacy.menu.sound"),
                                LegacyOptions.caveSounds, LegacyOptions.minecartSounds))),
                () -> Section.ADVANCED_AUDIO));
        public static final Section ADVANCED_AUDIO = new Section(
                Component.translatable("legacy.menu.settings.advanced_options", AUDIO.title()),
                s -> Panel.centered(s, 250, 198, 0, 30),
                new ArrayList<>(List.of(
                        o -> o.renderableVList.addOptions(
                                LegacyOptions.of(mc.options.soundDevice()),
                                LegacyOptions.backSound,
                                LegacyOptions.hoverFocusSound,
                                LegacyOptions.inventoryHoverFocusSound,
                                LegacyOptions.of(mc.options.showSubtitles()),
                                LegacyOptions.of(mc.options.directionalAudio()),
                                LegacyOptions.of(mc.options.musicFrequency()),
                                LegacyOptions.of(mc.options.musicToast())),
                        o -> o.renderableVList.addOptions(Arrays.stream(SoundSource.values()).filter(ss -> ss.ordinal() > 1).map(ss -> (FactoryConfig<?>) LegacyOptions.ofSound(mc.options.getSoundSourceOptionInstance(ss), "soundCategory." + ss.getName()))))));
        public static final Section GRAPHICS = add(new Section(
                Component.translatable("legacy.menu.graphics"),
                s -> LegacyOptions.legacySettingsMenus.get()
                        ? Panel.createPanel(s,
                                p -> p.appearance(250, ((OptionsScreen) s).getLegacyPanelHeight(96, true)),
                                p -> p.pos(p.centeredLeftPos(s), (s.height - 88) / 2 - 30))
                        : Panel.centered(s, 250, 222, 0, 24),
                new ArrayList<>(List.of(
                        o -> {
                            if (useLegacySettingsMenusOptions()) o.renderableVList.addRenderable(createRenderCloudsTickBox());
                            else o.renderableVList.addOptions(LegacyOptions.of(mc.options.cloudStatus()), LegacyOptions.optionsPreset);
                        },
                        o -> {
                            if (useLegacySettingsMenusOptions()) o.renderableVList.addOptions(
                                    LegacyOptions.customSkinAnimation,
                                    LegacyOptions.legacyGamma);
                            else o.renderableVList.addLinkedOptions(
                                    LegacyOptions.displayLegacyGamma, FactoryConfig::get,
                                    LegacyOptions.legacyGamma);
                        },
                        o -> {
                            if (shouldShowLegacyGraphicsPresetInGraphics()) o.renderableVList.addOptions(LegacyOptions.optionsPreset);
                        },
                        o -> {
                            if (!useLegacySettingsMenusOptions()) o.renderableVList.addOptions(
                                    LegacyOptions.of(mc.options.gamma()),
                                    LegacyOptions.of(mc.options.ambientOcclusion()));
        })),
                () -> Section.ADVANCED_GRAPHICS, (p, s) -> {
            if (LegacyOptions.legacySettingsMenus.get() && !isMergedLegacyGraphicsSection(s)) return new OptionsScreen(p, s) {
                @Override
                public void onClose() {
                    super.onClose();
                    showOptionsPresetWarningIfNeeded(parent, minecraft);
                }
            };
            GlobalPacks.Selector globalPackSelector = GlobalPacks.Selector.resources(0, 0, 230, 45, false);
            PackAlbum.Selector selector = PackAlbum.Selector.globalResources(0, 0, 230, 45, false);
            OptionsScreen screen = new OptionsScreen(p, s) {
                int selectorTooltipVisibility = 0;
                boolean finishedAnimation = false;

                @Override
                public void onClose() {
                    super.onClose();
                    showOptionsPresetWarningIfNeeded(parent, minecraft);
                    globalPackSelector.applyChanges();
                    selector.applyChanges(true);
                }

                @Override
                public boolean mouseScrolled(double d, double e, double f, double g) {
                    return shouldDisplayPackManagementTooltips() && selectorTooltipVisibility > 0 && (globalPackSelector.scrollableRenderer.mouseScrolled(g) || selector.scrollableRenderer.mouseScrolled(g)) || super.mouseScrolled(d, e, f, g);
                }

                @Override
                protected boolean shouldAddSelectorControlTooltips() {
                    return shouldDisplayPackManagementTooltips();
                }

                @Override
                protected void panelInit() {
                    super.panelInit();
                    if (shouldDisplayPackManagementTooltips()) {
                        panel.x -= Math.round(Math.min(10, getSelectorTooltipVisibility()) / 20f * PackAlbum.Selector.getDefaultWidth());
                    }
                }

                private float getSelectorTooltipVisibility() {
                    if (!shouldDisplayPackManagementTooltips()) {
                        return 0;
                    }
                    return selectorTooltipVisibility == 0 ? selectorTooltipVisibility : selectorTooltipVisibility + FactoryAPIClient.getPartialTick();
                }

                @Override
                public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
                    super.renderDefaultBackground(guiGraphics, i, j, f);
                    if (shouldDisplayPackManagementTooltips() && selectorTooltipVisibility > 0) {
                        if (getFocused() != globalPackSelector)
                            selector.renderTooltipBox(guiGraphics, panel, Math.round((1 - (Math.min(10, getSelectorTooltipVisibility())) / 10f) * -PackAlbum.Selector.getDefaultWidth()));
                        else
                            globalPackSelector.renderTooltipBox(guiGraphics, panel, Math.round((1 - (Math.min(10, getSelectorTooltipVisibility())) / 10f) * -PackAlbum.Selector.getDefaultWidth()));
                        guiGraphics.nextStratum();
                    }
                }


                @Override
                public void tick() {
                    if (shouldDisplayPackManagementTooltips() && ((getFocused() == selector || getFocused() == globalPackSelector) || selectorTooltipVisibility > 0) && selectorTooltipVisibility < 10) {
                        selectorTooltipVisibility++;
                    }

                    if (shouldDisplayPackManagementTooltips() && !finishedAnimation && selectorTooltipVisibility > 0) {
                        repositionElements();
                        if (selectorTooltipVisibility == 10) finishedAnimation = true;
                    }
                    super.tick();
                }

                private boolean shouldDisplayPackManagementTooltips() {
                    return LegacyOptions.displayPackManagementTooltips.get();
                }
            };
            screen.renderableVList.addRenderables(globalPackSelector, selector);
            return screen;
        }));
        public static final Section ADVANCED_GRAPHICS = new Section(
                Component.translatable("legacy.menu.settings.advanced_options", GRAPHICS.title()),
                s -> Panel.centered(s, 250, 215, 0, 20),
                new ArrayList<>(List.of(
                        o -> {
                            if (useLegacySettingsMenusOptions() && !shouldShowLegacyGraphicsPresetInGraphics()) o.renderableVList.addOptions(LegacyOptions.optionsPreset);
                        },
                        o -> o.renderableVList.addOptionsCategory(
                                Component.translatable("options.videoTitle"),
                                LegacyOptions.of(createResolutionOptionInstance(o)),
                                LegacyOptions.of(mc.options.fullscreen()),
                                LegacyOptions.of(mc.options.graphicsPreset()),
                                LegacyOptions.of(mc.options.enableVsync()),
                                LegacyOptions.of(mc.options.framerateLimit()),
                                LegacyOptions.of(mc.options.fov()),
                                LegacyOptions.of(mc.options.inactivityFpsLimit()),
                                LegacyOptions.of(mc.options.renderDistance()),
                                LegacyOptions.of(mc.options.simulationDistance()),
                                LegacyOptions.of(mc.options.cloudRange()),
                                LegacyOptions.of(mc.options.prioritizeChunkUpdates()),
                                LegacyOptions.of(mc.options.biomeBlendRadius()),
                                LegacyOptions.of(mc.options.entityDistanceScaling()),
                                LegacyOptions.of(mc.options.entityShadows())),
                        o -> o.renderableVList.addCategory(Component.translatable("legacy.menu.legacy_settings")),
                        o -> o.renderableVList.addLinkedOptions(
                                LegacyOptions.overrideTerrainFogStart,
                                FactoryConfig::get,
                                LegacyOptions.terrainFogStart),
                        o -> o.renderableVList.addLinkedOptions(
                                LegacyOptions.overrideTerrainFogEnd,
                                FactoryConfig::get,
                                LegacyOptions.terrainFogEnd),
                        o -> o.renderableVList.addLinkedOptions(
                                LegacyOptions.lceClouds,
                                FactoryConfig::get,
                                LegacyOptions.legacyCloudHeightAndTexture),
                        o -> o.renderableVList.addOptions(
                                LegacyOptions.legacySkyShape,
                                LegacyOptions.fastLeavesWhenBlocked,
                                LegacyOptions.fastLeavesCustomModels,
                                LegacyOptions.displayNameTagBorder,
                                LegacyOptions.itemLightingInHand,
                                LegacyOptions.enhancedItemTranslucency,
                                LegacyOptions.loyaltyLines,
                                LegacyOptions.merchantTradingIndicator,
                                LegacyOptions.legacyBabyVillagerHead,
                                LegacyOptions.legacyFireworks,
                                LegacyOptions.legacyEvokerFangs,
                                LegacyOptions.legacyDrownedAnimation,
                                LegacyOptions.legacyZombieAggressionAnimation,
                                LegacyOptions.legacyEntityFireTint,
                                LegacyOptions.legacyItemPickup,
                                LegacyOptions.enhancedPistonMovingRenderer,
                                LegacyOptions.legacyPotionsBar,
                                FactoryOptions.RANDOM_BLOCK_ROTATIONS,
                                LegacyOptions.defaultParticlePhysics,
                                LegacyOptions.of(mc.options.particles()),
                                LegacyOptions.bubblesOutsideWater),
                        o -> o.renderableVList.addLinkedOptions(
                                FactoryOptions.NEAREST_MIPMAP_SCALING,
                                b -> !b.get(),
                                LegacyOptions.of(mc.options.mipmapLevels())),
                        o -> o.renderableVList.addCategory(Component.translatable("legacy.menu.mixins")),
                        o -> Legacy4JClient.MIXIN_CONFIGS_STORAGE.configMap.values().forEach(c -> o.getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c))))),
                ArbitrarySupplier.empty(),
                (p, section) -> {
                    if (!LegacyOptions.legacySettingsMenus.get()) return new OptionsScreen(p, section);
                    return new OptionsScreen(p, section) {
                        @Override
                        public void onClose() {
                            super.onClose();
                            showOptionsPresetWarningIfNeeded(parent, minecraft);
                        }
                    };
                });
        public static final Section USER_INTERFACE = add(new Section(
                Component.translatable("legacy.menu.user_interface"),
                s -> LegacyOptions.legacySettingsMenus.get()
                        ? Panel.createPanel(s,
                                p -> p.appearance(250, ((OptionsScreen) s).getLegacyPanelHeight(170, false) - 4),
                                p -> p.pos(p.centeredLeftPos(s), (s.height - 170) / 2 + 18))
                        : Panel.centered(s, 250, 184, 0, 18),
                new ArrayList<>(List.of(
                        o -> {
                            if (useLegacySettingsMenusOptions()) o.renderableVList.addOptions(
                                    LegacyOptions.displayHUD,
                                    LegacyOptions.displayHand,
                                    LegacyOptions.displayGameMessages,
                                    LegacyOptions.of(mc.options.showAutosaveIndicator()),
                                    LegacyOptions.hudOpacity,
                                    LegacyOptions.inGameTooltips,
                                    LegacyOptions.animatedCharacter);
                            else o.renderableVList.addOptions(
                                    LegacyOptions.displayHUD,
                                    LegacyOptions.displayHand,
                                    LegacyOptions.of(mc.options.showAutosaveIndicator()),
                                    LegacyOptions.showVanillaRecipeBook,
                                    LegacyOptions.tooltipBoxes,
                                    LegacyOptions.of(mc.options.attackIndicator()),
                                    LegacyOptions.hudSize,
                                    LegacyOptions.hudOpacity,
                                    LegacyOptions.hudDistance,
                                    LegacyOptions.of(mc.options.guiScale()),
                                    LegacyOptions.uiMode);
                            if (!useLegacySettingsMenusOptions()) {
                                o.getRenderableVList().addRenderable(createDisplayAdvancedOptionsTooltipTickBox());
                                o.getRenderableVList().addRenderable(createDisplayPackManagementTooltipsTickBox());
                            }
                        },
                        o -> o.renderableVList.addMultSliderOption(LegacyOptions.interfaceSensitivity, 2),
                        o -> {
                            if (useLegacySettingsMenusOptions()) o.renderableVList.addOptions(
                                    LegacyOptions.inGameOnlineIds,
                                    LegacyOptions.classicCrafting,
                                    LegacyOptions.hudSize);
                            else {
                                o.getRenderableVList().addLinkedOptions(
                                        LegacyOptions.legacyItemTooltips,
                                        FactoryConfig::get,
                                        LegacyOptions.legacyItemTooltipScaling);
                            }
                        },
                        o -> {
                            if (!useLegacySettingsMenusOptions()) o.renderableVList.addOptions(
                                    LegacyOptions.inGameTooltips,
                                    LegacyOptions.animatedCharacter,
                                    LegacyOptions.smoothAnimatedCharacter,
                                    LegacyOptions.classicCrafting,
                                    LegacyOptions.classicStonecutting,
                                    LegacyOptions.classicLoom,
                                    LegacyOptions.classicTrading,
                                    LegacyOptions.forceMixedCrafting,
                                    LegacyOptions.modCraftingTabs,
                                    LegacyOptions.vanillaTabs,
                                    LegacyOptions.searchCreativeTab,
                                    LegacyOptions.of(mc.options.operatorItemsTab()),
                                    LegacyOptions.vignette,
                                    LegacyOptions.displayControlTooltips);
                        })),
                () -> Section.ADVANCED_USER_INTERFACE));
        public static final Section ADVANCED_USER_INTERFACE = new Section(
                Component.translatable("legacy.menu.settings.advanced_options", USER_INTERFACE.title()),
                s -> Panel.centered(s, 250, 184, 0, 18),
                new ArrayList<>(List.of(
                        o -> o.renderableVList.addOptionsCategory(
                                Component.translatable("legacy.menu.in_game_settings"),
                                LegacyOptions.invertedCrosshair,
                                LegacyOptions.legacyCreativeTab,
                                LegacyOptions.legacyAdvancements,
                                LegacyOptions.legacyLeaderboards,
                                LegacyOptions.legacyOverstackedItems,
                                LegacyOptions.legacyHearts,
                                LegacyOptions.legacyFont),
                        o -> o.renderableVList.addMultSliderOption(LegacyOptions.hudDelay, 2),
                        o -> o.renderableVList.addOptions(
                                LegacyOptions.screenshotToasts,
                                LegacyOptions.systemMessagesAsOverlay,
                                LegacyOptions.autoSaveCountdown,
                                LegacyOptions.advancedHeldItemTooltip,
                                LegacyOptions.itemTooltipEllipsis,
                                LegacyOptions.selectedItemTooltipLines,
                                LegacyOptions.selectedItemTooltipSpacing,
                                LegacyOptions.controlTooltipDisplay
                        ),
                        o -> {
                            o.renderableVList.addCategory(Component.translatable("legacy.menu.menu_settings"));
                            o.renderableVList.addRenderable(createLegacySettingsMenusTickBox(o));
                            o.renderableVList.addRenderable(createLegacyGraphicsPresetWidget(LegacyOptions.legacySettingsMenus.get()));
                            o.renderableVList.addOptions(
                                    LegacyOptions.titleScreenFade,
                                    LegacyOptions.titleScreenVersionText,
                                    LegacyOptions.menusWithBackground,
                                    LegacyOptions.legacyIntroAndReloading,
                                    LegacyOptions.legacyLoadingAndConnecting,
                                    LegacyOptions.legacyPanorama,
                                    LegacyOptions.displayRealmsButton);
                            if (FactoryAPI.isModLoaded("sodium")) {
                                o.renderableVList.addOptions(LegacyOptions.hideSodiumSettings);
                            }
                            o.renderableVList.addOptions(
                                    LegacyOptions.hideExperimentalWorldWarning,
                                    LegacyOptions.fakeAutosaveScreen,
                                    LegacyOptions.fakeManualSaveScreen);
                        },
                        o -> o.renderableVList.addOptionsCategory(
                                Component.translatable("options.chat.title"),
                                LegacyOptions.of(mc.options.reducedDebugInfo()),
                                LegacyOptions.announceAdvancements,
                                LegacyOptions.displayChatIndicators,
                                LegacyOptions.of(mc.options.chatVisibility()),
                                LegacyOptions.of(mc.options.chatOpacity()),
                                LegacyOptions.of(mc.options.textBackgroundOpacity()),
                                LegacyOptions.of(mc.options.chatScale()),
                                LegacyOptions.of(mc.options.chatLineSpacing()),
                                LegacyOptions.of(mc.options.chatDelay()),
                                LegacyOptions.of(mc.options.chatWidth()),
                                LegacyOptions.of(mc.options.chatHeightFocused()),
                                LegacyOptions.of(mc.options.chatHeightUnfocused()),
                                LegacyOptions.of(mc.options.chatColors()),
                                LegacyOptions.of(mc.options.chatLinks()),
                                LegacyOptions.of(mc.options.chatLinksPrompt()),
                                LegacyOptions.of(mc.options.backgroundForChatOnly()),
                                LegacyOptions.of(mc.options.autoSuggestions()),
                                LegacyOptions.of(mc.options.hideMatchedNames()),
                                LegacyOptions.of(mc.options.onlyShowSecureChat())))));


    }
}
