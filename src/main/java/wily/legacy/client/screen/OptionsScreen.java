package wily.legacy.client.screen;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.LegacyComponents;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class OptionsScreen extends PanelVListScreen {
    public Screen advancedOptionsScreen;
    public OptionsScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component) {
        super(panelConstructor, component);
        this.parent = parent;
    }
    public OptionsScreen(Screen parent, Section section) {
        this(parent, section.panelConstructor(), section.title());
        section.elements().forEach(c->c.accept(this));
        withAdvancedOptions(section.advancedOptions);
    }
    public OptionsScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component, Renderable... renderables) {
        this(parent,panelConstructor, component);
        renderableVList.addRenderables(renderables);
    }
    public OptionsScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component, OptionInstance<?>... optionInstances) {
        this(parent,panelConstructor, component);
        renderableVList.addOptions(optionInstances);
    }
    public OptionsScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component, Stream<OptionInstance<?>> optionInstances) {
        this(parent,panelConstructor, component);
        renderableVList.addOptions(-1,optionInstances);
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

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()-> advancedOptionsScreen == null || getFocused() instanceof Assort.Selector ? null : ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()-> LegacyComponents.SHOW_ADVANCED_OPTIONS);
    }

    public record Section(Component title, Function<Screen,Panel> panelConstructor,  List<Consumer<OptionsScreen>> elements, Function<OptionsScreen,Screen> advancedOptions, BiFunction<Screen,Section,OptionsScreen> sectionBuilder){
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

        public static final Section GAME_OPTIONS = add(new Section(Component.translatable("legacy.menu.game_options"),s->Panel.centered(s, 250,162), new ArrayList<>(List.of(o-> o.renderableVList.addOptions(mc.options.autoJump(),mc.options.bobView(), LegacyOption.flyingViewRolling,LegacyOption.hints,LegacyOption.autoSaveInterval),o->o.renderableVList.addRenderables(RenderableVListScreen.openScreenButton(Component.translatable("options.language"), () -> new LegacyLanguageScreen(o, mc.getLanguageManager())).build(),RenderableVListScreen.openScreenButton(Component.translatable("legacy.menu.mods"), () -> new ModsScreen(o)).build()),o-> {if (mc.level == null && !mc.hasSingleplayerServer()) o.renderableVList.addOptions(LegacyOption.createWorldDifficulty);})),s-> Section.ADVANCED_GAME_OPTIONS.build(s)));
        public static final Section ADVANCED_GAME_OPTIONS = new Section(GAME_OPTIONS.title(),s->Panel.centered(s,250,172),LegacyOption.selectedControlType,LegacyOption.lockControlTypeChange,LegacyOption.unfocusedInputs,LegacyOption.autoSaveWhenPaused,LegacyOption.directSaveLoad,LegacyOption.legacyCreativeBlockPlacing,LegacyOption.cursorMode,mc.options.realmsNotifications(), mc.options.allowServerListing());
        public static final Section AUDIO = add(new Section(Component.translatable("legacy.menu.audio"),s->Panel.centered(s,250,88,0,-30), new ArrayList<>(List.of(o->o.renderableVList.addOptions(-1, Streams.concat(Arrays.stream(SoundSource.values()).filter(s->s.ordinal() <= 1).sorted(Comparator.comparingInt(s->s == SoundSource.MUSIC ? 0 : 1)).map(mc.options::getSoundSourceOptionInstance), Stream.of(LegacyOption.caveSounds,LegacyOption.minecartSounds))))), s-> Section.ADVANCED_AUDIO.build(s)));
        public static final Section ADVANCED_AUDIO = new Section(AUDIO.title(),s->Panel.centered(s,250,198,0,30), new ArrayList<>(List.of(o->o.renderableVList.addOptions(-1, Stream.concat(Stream.of(mc.options.soundDevice()), Arrays.stream(SoundSource.values()).filter(ss->ss.ordinal() > 1).map(mc.options::getSoundSourceOptionInstance))))));
        public static final Section GRAPHICS = add(new Section(Component.translatable("legacy.menu.graphics"),s->Panel.centered(s, 250,172), new ArrayList<>(List.of(o->o.renderableVList.addOptions(mc.options.cloudStatus(),mc.options.graphicsMode()),o->o.renderableVList.addLinkedOptions(-1,LegacyOption.displayLegacyGamma, OptionInstance::get, LegacyOption.legacyGamma),o->o.renderableVList.addOptions(mc.options.gamma(),mc.options.ambientOcclusion()))), s-> Section.ADVANCED_GRAPHICS.build(s),(p, s)-> {
            Assort.Selector selector = Assort.Selector.resources(0,0,230,45,true);
            OptionsScreen screen = new OptionsScreen(p, s){
                @Override
                public void addControlTooltips(ControlTooltip.Renderer renderer) {
                    super.addControlTooltips(renderer);
                    selector.addControlTooltips(this,renderer);
                }
                @Override
                public void onClose() {
                    super.onClose();
                    selector.applyChanges(true);
                }
            };
            screen.renderableVList.addRenderable(selector);
            return screen;
        }));
        public static final Section ADVANCED_GRAPHICS = new Section(GRAPHICS.title(),s->Panel.centered(s, 250,215,0,20), new ArrayList<>(List.of(o->o.renderableVList.addOptions(createResolutionOptionInstance(), mc.options.biomeBlendRadius(), mc.options.renderDistance(), mc.options.prioritizeChunkUpdates(), mc.options.simulationDistance(),LegacyOption.overrideTerrainFogStart,LegacyOption.terrainFogStart,LegacyOption.terrainFogEnd, mc.options.framerateLimit(), mc.options.enableVsync()/*? if >=1.21.2 {*/,mc.options.inactivityFpsLimit()/*?}*/,LegacyOption.displayNameTagBorder,LegacyOption.itemLightingInHand,LegacyOption.loyaltyLines,LegacyOption.merchantTradingIndicator,LegacyOption.legacyDrownedAnimation,LegacyOption.vehicleCameraRotation/*? if >=1.21.2 {*/,mc.options.rotateWithMinecart()/*?}*/,LegacyOption.defaultParticlePhysics, mc.options.fullscreen(), mc.options.particles(), mc.options.mipmapLevels(), mc.options.entityShadows(), mc.options.screenEffectScale(), mc.options.entityDistanceScaling(), mc.options.fov(), mc.options.fovEffectScale(), mc.options.darknessEffectScale(), mc.options.glintSpeed(), mc.options.glintStrength()))));
        public static final Section USER_INTERFACE = add(new Section(Component.translatable("legacy.menu.user_interface"),s->Panel.centered(s,250,200,0,18), new ArrayList<>(List.of(o->o.renderableVList.addOptions(LegacyOption.displayHUD,LegacyOption.displayHand,mc.options.showAutosaveIndicator(),LegacyOption.showVanillaRecipeBook,LegacyOption.tooltipBoxes,LegacyOption.hudOpacity,mc.options.attackIndicator(),LegacyOption.hudScale,LegacyOption.hudDistance),o-> o.renderableVList.addLinkedOptions(-1,LegacyOption.autoResolution, b-> !b.get(),LegacyOption.interfaceResolution),o->o.renderableVList.addOptions(LegacyOption.inGameTooltips,LegacyOption.animatedCharacter,LegacyOption.smoothAnimatedCharacter,LegacyOption.interfaceSensitivity,LegacyOption.classicCrafting,LegacyOption.vanillaTabs, LegacyOption.searchCreativeTab, mc.options.operatorItemsTab()),o-> o.getRenderableVList().addLinkedOptions(-1,LegacyOption.legacyItemTooltips, OptionInstance::get, LegacyOption.legacyItemTooltipScaling),o-> o.getRenderableVList().addOptions(LegacyOption.vignette, mc.options.narrator(), mc.options.showSubtitles(), mc.options.highContrast()))), s-> Section.ADVANCED_USER_INTERFACE.build(s)));
        public static final Section ADVANCED_USER_INTERFACE = new Section(USER_INTERFACE.title(), USER_INTERFACE.panelConstructor(), LegacyOption.legacyCreativeTab, LegacyOption.invertedCrosshair, LegacyOption.selectedItemTooltipLines, LegacyOption.itemTooltipEllipsis, LegacyOption.selectedItemTooltipSpacing, LegacyOption.legacyOverstackedItems, LegacyOption.displayMultipleControlsFromAction, mc.options.notificationDisplayTime(), mc.options.damageTiltStrength(), mc.options.glintSpeed(), mc.options.glintStrength(), mc.options.hideLightningFlash(), mc.options.darkMojangStudiosBackground(), mc.options.panoramaSpeed()/*? if >1.20.1 {*/, mc.options.narratorHotkey()/*?}*/,mc.options.chatVisibility(), mc.options.chatColors(), mc.options.chatLinks(), mc.options.chatLinksPrompt(),  mc.options.backgroundForChatOnly(), mc.options.chatOpacity(), mc.options.textBackgroundOpacity(), mc.options.chatScale(), mc.options.chatLineSpacing(), mc.options.chatDelay(), mc.options.chatWidth(), mc.options.chatHeightFocused(), mc.options.chatHeightUnfocused(), mc.options.narrator(), mc.options.autoSuggestions(), mc.options.hideMatchedNames(), mc.options.reducedDebugInfo(), mc.options.onlyShowSecureChat());

        public static Section add(Section section){
            list.add(section);
            return section;
        }

        public Section(Component title, Function<Screen,Panel> panelConstructor,  List<Consumer<OptionsScreen>> elements, Function<OptionsScreen,Screen> advancedOptions){
            this(title,panelConstructor,elements,advancedOptions, OptionsScreen::new);
        }
        public Section(Component title, Function<Screen,Panel> panelConstructor,  List<Consumer<OptionsScreen>> elements){
            this(title,panelConstructor,elements,s->null);
        }
        public Section(Component title, Function<Screen,Panel> panelConstructor, Function<OptionsScreen,Screen> advancedOptions, OptionInstance<?>... optionInstances){
            this(title,panelConstructor,new ArrayList<>(List.of(o->o.renderableVList.addOptions(optionInstances))),advancedOptions);
        }
        public Section(Component title, Function<Screen,Panel> panelConstructor,  OptionInstance<?>... optionInstances){
            this(title,panelConstructor,s->null,optionInstances);
        }
        public OptionsScreen build(Screen parent){
            return sectionBuilder.apply(parent,this);
        }
    }
}
