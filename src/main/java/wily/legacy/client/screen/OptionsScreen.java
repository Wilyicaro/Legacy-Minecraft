package wily.legacy.client.screen;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryOptions;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.controller.ControllerBinding;
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
    public Screen advancedOptionsScreen;

    public OptionsScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component) {
        super(parent, panelConstructor, component);
    }

    public OptionsScreen(Screen parent, Section section) {
        this(parent,LegacyOptions.advancedOptionsMode.get() == LegacyOptions.AdvancedOptionsMode.MERGE && section.advancedSection.isPresent() ? section.advancedSection.get().panelConstructor() : section.panelConstructor(), section.title());
        section.elements().forEach(c->c.accept(this));
        section.advancedSection.ifPresent(s-> {
            switch (LegacyOptions.advancedOptionsMode.get()){
                case DEFAULT -> withAdvancedOptions(s.build(this));
                case MERGE -> s.elements().forEach(c->c.accept(this));
            }
        });
    }

    public OptionsScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component, Renderable... renderables) {
        this(parent, panelConstructor, component);
        renderableVList.addRenderables(renderables);
    }

    public OptionsScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component, FactoryConfig<?>... options) {
        this(parent, panelConstructor, component);
        renderableVList.addOptions(options);
    }

    public OptionsScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component, Stream<FactoryConfig<?>> options) {
        this(parent, panelConstructor, component);
        renderableVList.addOptions(options);
    }

    public OptionsScreen withAdvancedOptions(Function<OptionsScreen,Screen> advancedOptionsFunction){
        return withAdvancedOptions(advancedOptionsFunction.apply(this));
    }

    public OptionsScreen withAdvancedOptions(Screen screen){
        advancedOptionsScreen = screen;
        return this;
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (super.keyPressed(i, j, k)) return true;
        if (i == InputConstants.KEY_O && advancedOptionsScreen != null){
            minecraft.setScreen(advancedOptionsScreen);
            return true;
        }
        return false;
    }

    public static void setupSelectorControlTooltips(ControlTooltip.Renderer renderer, Screen screen){
        renderer.add(()-> ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{getKeyIcon(InputConstants.KEY_LSHIFT), PLUS_ICON,getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT)}) : null, ()-> ControlTooltip.getKeyMessage(InputConstants.MOUSE_BUTTON_LEFT,screen));
        renderer.add(ControlTooltip.EXTRA::get, ()-> ControlTooltip.getKeyMessage(InputConstants.KEY_X,screen));
        renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(), ()-> ControlTooltip.getKeyMessage(InputConstants.KEY_O,screen));
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        setupSelectorControlTooltips(renderer, this);
        renderer.replace(6, i-> i, c-> c == null ? advancedOptionsScreen == null ? null : LegacyComponents.SHOW_ADVANCED_OPTIONS : c);
    }

    public record Section(Component title, Function<Screen,Panel> panelConstructor, List<Consumer<OptionsScreen>> elements, ArbitrarySupplier<Section> advancedSection, BiFunction<Screen,Section,OptionsScreen> sectionBuilder) implements ScreenSection<OptionsScreen>{
        private static final Minecraft mc = Minecraft.getInstance();
        public static final List<Section> list = new ArrayList<>();

        public static OptionInstance<?> createResolutionOptionInstance(){
            Monitor monitor = mc.getWindow().findBestMonitor();
            int j = monitor == null ? -1: mc.getWindow().getPreferredFullscreenVideoMode().map(monitor::getVideoModeIndex).orElse(-1);
            return new OptionInstance<>("options.fullscreen.resolution", OptionInstance.noTooltip(), (component, integer) -> {
                if (monitor == null)
                    return Component.translatable("options.fullscreen.unavailable");
                else if (integer == -1) {
                    return Options.genericValueLabel(component, Component.translatable("options.fullscreen.current"));
                }
                VideoMode videoMode = monitor.getMode(integer);
                return Options.genericValueLabel(component, /*? if >1.20.1 {*/Component.translatable("options.fullscreen.entry", videoMode.getWidth(), videoMode.getHeight(), videoMode.getRefreshRate(), videoMode.getRedBits() + videoMode.getGreenBits() + videoMode.getBlueBits())/*?} else {*//*Component.literal(videoMode.toString())*//*?}*/);
            }, new OptionInstance.IntRange(-1, monitor != null ? monitor.getModeCount() - 1 : -1), j, integer -> {
                if (monitor == null)
                    return;
                mc.getWindow().setPreferredFullscreenVideoMode(integer == -1 ? Optional.empty() : Optional.of(monitor.getMode(integer)));
            });
        }

        public static final Section GAME_OPTIONS = add(new Section(
                Component.translatable("legacy.menu.game_options"),
                s->Panel.centered(s, 250,162),
                new ArrayList<>(List.of(
                        o-> o.renderableVList.addOptions(
                                LegacyOptions.of(mc.options.autoJump()),
                                LegacyOptions.of(mc.options.bobView()),
                                LegacyOptions.flyingViewRolling,
                                LegacyOptions.hints,
                                LegacyOptions.autoSaveInterval),
                        o->o.renderableVList.addRenderables(
                                RenderableVListScreen.openScreenButton(Component.translatable("options.language"), () -> new LegacyLanguageScreen(o, mc.getLanguageManager())).build(),
                                RenderableVListScreen.openScreenButton(Component.translatable("legacy.menu.mods"), () -> new ModsScreen(o)).build()),
                        o-> {if (mc.level == null && !mc.hasSingleplayerServer())
                            o.renderableVList.addOptions(LegacyOptions.createWorldDifficulty);})),
                ()-> Section.ADVANCED_GAME_OPTIONS));
        public static final Section ADVANCED_GAME_OPTIONS = new Section(
                Component.translatable("legacy.menu.settings.advanced_options",GAME_OPTIONS.title()),
                s->Panel.centered(s,250,172),
                new ArrayList<>(List.of(
                        o-> o.renderableVList.addOptions(
                                LegacyOptions.selectedControlType,
                                LegacyOptions.lockControlTypeChange,
                                LegacyOptions.unfocusedInputs,
                                LegacyOptions.autoSaveWhenPaused,
                                LegacyOptions.directSaveLoad,
                                LegacyOptions.saveCache,
                                LegacyOptions.legacyCreativeBlockPlacing,
                                LegacyOptions.cursorMode,
                                LegacyOptions.invertedFrontCameraPitch,
                                LegacyOptions.skipIntro,
                                LegacyOptions.skipInitialSaveWarning,
                                LegacyOptions.vanillaTutorial,
                                LegacyOptions.of(mc.options.realmsNotifications()),
                                LegacyOptions.of(mc.options.allowServerListing())),
                        o-> {if (mc.level == null) LegacyCommonOptions.COMMON_STORAGE.configMap.values().forEach(c-> o.getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c, b-> c.sync())));},
                        o-> o.renderableVList.addRenderables(
                                RenderableVListScreen.openScreenButton(LegacyComponents.RESET_KNOWN_BLOCKS_TITLE,()-> ConfirmationScreen.createResetKnownListingScreen(o, LegacyComponents.RESET_KNOWN_BLOCKS_TITLE,LegacyComponents.RESET_KNOWN_BLOCKS_MESSAGE, Legacy4JClient.knownBlocks)).build(),
                                RenderableVListScreen.openScreenButton(LegacyComponents.RESET_KNOWN_ENTITIES_TITLE,()-> ConfirmationScreen.createResetKnownListingScreen(o, LegacyComponents.RESET_KNOWN_ENTITIES_TITLE,LegacyComponents.RESET_KNOWN_ENTITIES_MESSAGE, Legacy4JClient.knownEntities)).build()))));
        public static final Section AUDIO = add(new Section(
                Component.translatable("legacy.menu.audio"),
                s->Panel.centered(s,250,88,0,-30),
                new ArrayList<>(List.of(
                        o->o.renderableVList.addOptions(Streams.concat(
                                Arrays.stream(SoundSource.values()).filter(s->s.ordinal() <= 1).sorted(Comparator.comparingInt(s->s == SoundSource.MUSIC ? 0 : 1)).map(s->LegacyOptions.of(mc.options.getSoundSourceOptionInstance(s))),
                                Stream.of(LegacyOptions.caveSounds,LegacyOptions.minecartSounds))))),
                ()-> Section.ADVANCED_AUDIO));
        public static final Section ADVANCED_AUDIO = new Section(
                Component.translatable("legacy.menu.settings.advanced_options",AUDIO.title()),
                s->Panel.centered(s,250,198,0,30),
                new ArrayList<>(List.of(
                        o-> o.renderableVList.addOptions(
                                LegacyOptions.of(mc.options.soundDevice()),
                                LegacyOptions.backSound,
                                LegacyOptions.hoverFocusSound),
                        o->o.renderableVList.addOptions(Arrays.stream(SoundSource.values()).filter(ss->ss.ordinal() > 1).map(mc.options::getSoundSourceOptionInstance).map(LegacyOptions::of)))));
        public static final Section GRAPHICS = add(new Section(
                Component.translatable("legacy.menu.graphics"),
                s->Panel.centered(s, 250,222, 0, 24),
                new ArrayList<>(List.of(
                        o->o.renderableVList.addOptions(
                                LegacyOptions.of(mc.options.cloudStatus()),
                                LegacyOptions.of(mc.options.graphicsMode())),
                        o->o.renderableVList.addLinkedOptions(
                                LegacyOptions.displayLegacyGamma, FactoryConfig::get,
                                LegacyOptions.legacyGamma),
                        o->o.renderableVList.addOptions(
                                LegacyOptions.of(mc.options.gamma()),
                                LegacyOptions.of(mc.options.ambientOcclusion())))),
                ()-> Section.ADVANCED_GRAPHICS,(p, s)-> {
                    GlobalPacks.Selector globalPackSelector = GlobalPacks.Selector.resources(0,0,230,45,false);
                    PackAlbum.Selector selector = PackAlbum.Selector.resources(0,0,230,45,false);
                    OptionsScreen screen = new OptionsScreen(p, s){
                        int selectorTooltipVisibility = 0;
                        boolean finishedAnimation = false;
                        @Override
                        public void onClose() {
                            super.onClose();
                            globalPackSelector.applyChanges();
                            selector.applyChanges(true);
                        }

                        @Override
                        protected void panelInit() {
                            super.panelInit();
                            panel.x-=Math.round(Math.min(10,getSelectorTooltipVisibility()) / 10f * 80);
                        }

                        private float getSelectorTooltipVisibility(){
                            return selectorTooltipVisibility == 0 ? selectorTooltipVisibility : selectorTooltipVisibility + FactoryAPIClient.getPartialTick();
                        }

                        @Override
                        public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
                            super.renderDefaultBackground(guiGraphics, i, j, f);
                            if (selectorTooltipVisibility > 0){
                                if (getFocused() != globalPackSelector) selector.renderTooltipBox(guiGraphics, panel, Math.round((1 - (Math.min(10, getSelectorTooltipVisibility())) / 10f) * -161));
                                else globalPackSelector.renderTooltipBox(guiGraphics, panel, Math.round((1 - (Math.min(10, getSelectorTooltipVisibility())) / 10f) * -161));
                                guiGraphics.pose().translate(0, 0, 0.03f);
                            }
                        }

                        @Override
                        public void tick() {
                            if (((getFocused() == selector || getFocused() == globalPackSelector) || selectorTooltipVisibility > 0) && selectorTooltipVisibility < 10){
                                selectorTooltipVisibility++;
                            }

                            if (!finishedAnimation && selectorTooltipVisibility > 0){
                                repositionElements();
                                if(selectorTooltipVisibility == 10) finishedAnimation = true;
                            }
                            super.tick();
                        }
                    };
                    screen.renderableVList.addRenderables(globalPackSelector, selector);
                    return screen;
                }));
        public static final Section ADVANCED_GRAPHICS = new Section(
                Component.translatable("legacy.menu.settings.advanced_options",GRAPHICS.title()),
                s->Panel.centered(s, 250,215,0,20),
                new ArrayList<>(List.of(
                        o->o.renderableVList.addOptions(
                                LegacyOptions.of(createResolutionOptionInstance()),
                                LegacyOptions.of(mc.options.biomeBlendRadius()),
                                LegacyOptions.of(mc.options.renderDistance()),
                                LegacyOptions.of(mc.options.prioritizeChunkUpdates()),
                                LegacyOptions.of(mc.options.simulationDistance()),
                                LegacyOptions.overrideTerrainFogStart,
                                LegacyOptions.terrainFogStart,
                                LegacyOptions.overrideTerrainFogEnd,
                                LegacyOptions.terrainFogEnd,
                                LegacyOptions.legacySkyShape,
                                LegacyOptions.fastLeavesWhenBlocked,
                                LegacyOptions.fastLeavesCustomModels,
                                LegacyOptions.mapsWithCoords,
                                LegacyOptions.of(mc.options.framerateLimit()),
                                LegacyOptions.of(mc.options.enableVsync())
                                /*? if >=1.21.2 {*//*,LegacyOptions.of(mc.options.inactivityFpsLimit())*//*?}*/,
                                LegacyOptions.displayNameTagBorder,
                                LegacyOptions.itemLightingInHand,
                                LegacyOptions.loyaltyLines,
                                LegacyOptions.merchantTradingIndicator,
                                LegacyOptions.legacyBabyVillagerHead,
                                LegacyOptions.legacyDrownedAnimation,
                                LegacyOptions.legacyEntityFireTint,
                                LegacyOptions.vehicleCameraRotation
                                /*? if >=1.21.2 {*//*,LegacyOptions.create(mc.options.rotateWithMinecart())*//*?}*/,
                                LegacyOptions.defaultParticlePhysics,
                                LegacyOptions.legacyItemPickup,
                                LegacyOptions.headFollowsTheCamera,
                                LegacyOptions.enhancedPistonMovingRenderer,
                                FactoryOptions.RANDOM_BLOCK_ROTATIONS,
                                LegacyOptions.of(mc.options.fullscreen()),
                                LegacyOptions.of(mc.options.particles()),
                                LegacyOptions.of(mc.options.mipmapLevels()),
                                FactoryOptions.NEAREST_MIPMAP_SCALING,
                                LegacyOptions.of(mc.options.entityShadows()),
                                LegacyOptions.of(mc.options.screenEffectScale()),
                                LegacyOptions.of(mc.options.entityDistanceScaling()),
                                LegacyOptions.of(mc.options.fov()),
                                LegacyOptions.of(mc.options.fovEffectScale()),
                                LegacyOptions.of(mc.options.darknessEffectScale()),
                                LegacyOptions.of(mc.options.glintSpeed()),
                                LegacyOptions.legacyEvokerFangs,
                                LegacyOptions.of(mc.options.glintStrength())),
                        o-> Legacy4JClient.MIXIN_CONFIGS_STORAGE.configMap.values().forEach(c-> o.getRenderableVList().addRenderable(LegacyConfigWidgets.createWidget(c))))));
        public static final Section USER_INTERFACE = add(new Section(
                Component.translatable("legacy.menu.user_interface"),
                s->Panel.centered(s,250,200,0,18),
                new ArrayList<>(List.of(
                        o->o.renderableVList.addOptions(
                                LegacyOptions.displayHUD,
                                LegacyOptions.displayHand,
                                LegacyOptions.of(mc.options.showAutosaveIndicator()),
                                LegacyOptions.showVanillaRecipeBook,
                                LegacyOptions.tooltipBoxes,
                                LegacyOptions.hudOpacity,
                                LegacyOptions.of(mc.options.attackIndicator()),
                                LegacyOptions.hudScale,
                                LegacyOptions.hudDistance),
                        o-> o.renderableVList.addLinkedOptions(
                                LegacyOptions.autoResolution, b-> !b.get(),
                                LegacyOptions.interfaceResolution),
                        o->o.renderableVList.addOptions(
                                LegacyOptions.inGameTooltips,
                                LegacyOptions.animatedCharacter,
                                LegacyOptions.smoothAnimatedCharacter,
                                LegacyOptions.interfaceSensitivity,
                                LegacyOptions.classicCrafting,
                                LegacyOptions.classicTrading,
                                LegacyOptions.classicStonecutting,
                                LegacyOptions.classicLoom,
                                LegacyOptions.forceMixedCrafting,
                                LegacyOptions.modCraftingTabs,
                                LegacyOptions.vanillaTabs,
                                LegacyOptions.searchCreativeTab,
                                LegacyOptions.of(mc.options.operatorItemsTab())),
                        o-> o.getRenderableVList().addLinkedOptions(
                                LegacyOptions.legacyItemTooltips, FactoryConfig::get,
                                LegacyOptions.legacyItemTooltipScaling),
                        o-> o.getRenderableVList().addOptions(
                                LegacyOptions.vignette,
                                LegacyOptions.of(mc.options.narrator()),
                                LegacyOptions.of(mc.options.showSubtitles()),
                                LegacyOptions.of(mc.options.highContrast())))),
                ()-> Section.ADVANCED_USER_INTERFACE));
        public static final Section ADVANCED_USER_INTERFACE = new Section(
                Component.translatable("legacy.menu.settings.advanced_options",USER_INTERFACE.title()),
                USER_INTERFACE.panelConstructor(),
                LegacyOptions.legacyCreativeTab,
                LegacyOptions.legacyHearts,
                LegacyOptions.invertedCrosshair,
                LegacyOptions.selectedItemTooltipLines,
                LegacyOptions.itemTooltipEllipsis,
                LegacyOptions.selectedItemTooltipSpacing,
                LegacyOptions.legacyOverstackedItems,
                LegacyOptions.displayMultipleControlsFromAction,
                LegacyOptions.advancedHeldItemTooltip,
                LegacyOptions.autoSaveCountdown,
                LegacyOptions.displayControlTooltips,
                LegacyOptions.controlTooltipDisplay,
                LegacyOptions.hudDelay,
                LegacyOptions.systemMessagesAsOverlay,
                LegacyOptions.legacyIntroAndReloading,
                LegacyOptions.titleScreenFade,
                LegacyOptions.titleScreenVersionText,
                LegacyOptions.legacyPanorama,
                LegacyOptions.menusWithBackground,
                LegacyOptions.legacyLoadingAndConnecting,
                LegacyOptions.of(mc.options.notificationDisplayTime()),
                LegacyOptions.of(mc.options.damageTiltStrength()),
                LegacyOptions.of(mc.options.glintSpeed()),
                LegacyOptions.of(mc.options.glintStrength()),
                LegacyOptions.of(mc.options.hideLightningFlash()),
                LegacyOptions.of(mc.options.darkMojangStudiosBackground()),
                LegacyOptions.of(mc.options.panoramaSpeed())
                /*? if >1.20.1 {*/, LegacyOptions.of(mc.options.narratorHotkey())/*?}*/,
                LegacyOptions.of(mc.options.chatVisibility()),
                LegacyOptions.of(mc.options.chatColors()),
                LegacyOptions.of(mc.options.chatLinks()),
                LegacyOptions.of(mc.options.chatLinksPrompt()),
                LegacyOptions.of(mc.options.backgroundForChatOnly()),
                LegacyOptions.of(mc.options.chatOpacity()),
                LegacyOptions.of(mc.options.textBackgroundOpacity()),
                LegacyOptions.of(mc.options.chatScale()),
                LegacyOptions.of(mc.options.chatLineSpacing()),
                LegacyOptions.of(mc.options.chatDelay()),
                LegacyOptions.of(mc.options.chatWidth()),
                LegacyOptions.of(mc.options.chatHeightFocused()),
                LegacyOptions.of(mc.options.chatHeightUnfocused()),
                LegacyOptions.of(mc.options.narrator()),
                LegacyOptions.of(mc.options.autoSuggestions()),
                LegacyOptions.of(mc.options.hideMatchedNames()),
                LegacyOptions.of(mc.options.reducedDebugInfo()),
                LegacyOptions.of(mc.options.onlyShowSecureChat()));

        public static Section add(Section section){
            list.add(section);
            return section;
        }

        public Section(Component title, Function<Screen,Panel> panelConstructor,  List<Consumer<OptionsScreen>> elements, ArbitrarySupplier<Section> advancedSection){
            this(title, panelConstructor, elements, advancedSection, OptionsScreen::new);
        }

        public Section(Component title, Function<Screen,Panel> panelConstructor, List<Consumer<OptionsScreen>> elements){
            this(title, panelConstructor, elements, ArbitrarySupplier.empty());
        }

        public Section(Component title, Function<Screen,Panel> panelConstructor, ArbitrarySupplier<Section> advancedSection, FactoryConfig<?>... options){
            this(title, panelConstructor, new ArrayList<>(List.of(o->o.renderableVList.addOptions(options))), advancedSection);
        }

        public Section(Component title, Function<Screen,Panel> panelConstructor, FactoryConfig<?>... optionInstances){
            this(title, panelConstructor, ArbitrarySupplier.empty(), optionInstances);
        }

        public OptionsScreen build(Screen parent){
            return sectionBuilder.apply(parent,this);
        }
    }
}
