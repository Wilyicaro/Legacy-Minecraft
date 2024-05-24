package wily.legacy.client.screen;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.ControllerBinding;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import static wily.legacy.client.screen.ControlTooltip.*;

public class SettingsScreen extends RenderableVListScreen {

    protected SettingsScreen(Screen parent) {
        super(parent,Component.translatable("legacy.menu.settings"), r->{});
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.game_options"), ()-> {
            PanelVListScreen s = new PanelVListScreen(this,250,182,Component.translatable("legacy.menu.game_options"),minecraft.options.autoJump(),minecraft.options.bobView(),minecraft.options.toggleCrouch(),minecraft.options.toggleSprint(),((LegacyOptions)minecraft.options).hints(),((LegacyOptions)minecraft.options).displayNameTagBorder(),((LegacyOptions)minecraft.options).directSaveLoad(),minecraft.options.sensitivity(),minecraft.options.realmsNotifications(), minecraft.options.allowServerListing());
            s.renderableVList.addRenderable(openScreenButton(Component.translatable("options.language"), () -> new LegacyLanguageScreen(s, this.minecraft.getLanguageManager())).build());
            s.renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.mods"), () -> new ModsScreen(s)).build());
            if (minecraft.level == null && !minecraft.hasSingleplayerServer()) s.renderableVList.addOptions(((LegacyOptions)minecraft.options).createWorldDifficulty());
            return s;
        }).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.audio"),()->new PanelVListScreen(this,250,218,Component.translatable("legacy.menu.audio"), Streams.concat(Arrays.stream(SoundSource.values()).sorted(Comparator.comparingInt(s->s == SoundSource.MUSIC ? 0 : 1)).map(minecraft.options::getSoundSourceOptionInstance), Stream.of(((LegacyOptions)minecraft.options).caveSounds(),((LegacyOptions)minecraft.options).minecartSounds())).map(s-> s.createButton(minecraft.options,0,0,0)).toArray(AbstractWidget[]::new))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.graphics"),()-> {
            Monitor monitor = minecraft.getWindow().findBestMonitor();
            int j = monitor == null ? -1: minecraft.getWindow().getPreferredFullscreenVideoMode().map(monitor::getVideoModeIndex).orElse(-1);
            OptionInstance<Integer> optionInstance = new OptionInstance<Integer>("options.fullscreen.resolution", OptionInstance.noTooltip(), (component, integer) -> {
                if (monitor == null)
                    return Component.translatable("options.fullscreen.unavailable");
                if (integer == -1) {
                    return Options.genericValueLabel(component, Component.translatable("options.fullscreen.current"));
                }
                VideoMode videoMode = monitor.getMode(integer);
                return Options.genericValueLabel(component, Component.translatable("options.fullscreen.entry", videoMode.getWidth(), videoMode.getHeight(), videoMode.getRefreshRate(), videoMode.getRedBits() + videoMode.getGreenBits() + videoMode.getBlueBits()));
            }, new OptionInstance.IntRange(-1, monitor != null ? monitor.getModeCount() - 1 : -1), j, integer -> {
                if (monitor == null)
                    return;
                minecraft.getWindow().setPreferredFullscreenVideoMode(integer == -1 ? Optional.empty() : Optional.of(monitor.getMode(integer)));
            });
            PackSelector selector = PackSelector.resources(0,0,230,45,true);
            PanelVListScreen  s = new PanelVListScreen(this,250,215,Component.translatable("legacy.menu.graphics"),optionInstance,minecraft.options.biomeBlendRadius(),minecraft.options.graphicsMode(), minecraft.options.renderDistance(), minecraft.options.prioritizeChunkUpdates(), minecraft.options.simulationDistance(),((LegacyOptions)minecraft.options).overrideTerrainFogStart(),((LegacyOptions)minecraft.options).terrainFogStart(),((LegacyOptions)minecraft.options).terrainFogEnd(), minecraft.options.ambientOcclusion(), minecraft.options.framerateLimit(), minecraft.options.enableVsync(), minecraft.options.bobView(), minecraft.options.gamma(), ((LegacyOptions)minecraft.options).legacyGamma(),minecraft.options.cloudStatus(), minecraft.options.fullscreen(), minecraft.options.particles(), minecraft.options.mipmapLevels(), minecraft.options.entityShadows(), minecraft.options.screenEffectScale(), minecraft.options.entityDistanceScaling(), minecraft.options.fov(),minecraft.options.fovEffectScale(),minecraft.options.darknessEffectScale(),minecraft.options.glintSpeed(), minecraft.options.glintStrength()){
                public void onClose() {
                    super.onClose();
                    selector.applyChanges(true);
                }
            };
            s.renderableVList.addRenderable(selector);
            s.controlTooltipRenderer.add(()-> getActiveType().isKeyboard() ? COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{getKeyIcon(InputConstants.KEY_LSHIFT,true), PLUS,getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT,true)}) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(true), ()-> s.getFocused() == selector ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.resource_packs_screen") : null);
            return s;
        }).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.user_interface"),()-> {
                    AbstractWidget interfaceResolutionWidget = ((LegacyOptions)minecraft.options).interfaceResolution().createButton(minecraft.options,0,0,0);
                    interfaceResolutionWidget.active = !((LegacyOptions)minecraft.options).autoResolution().get();
                    AbstractWidget autoResolutionWidget = ((LegacyOptions)minecraft.options).autoResolution().createButton(minecraft.options,0,0,0,b-> interfaceResolutionWidget.active = !b);
                    PanelVListScreen s = new PanelVListScreen(this,250,200,Component.translatable("legacy.menu.user_interface"), ((LegacyOptions)minecraft.options).displayHUD(),minecraft.options.showAutosaveIndicator(),((LegacyOptions)minecraft.options).showVanillaRecipeBook(),((LegacyOptions)minecraft.options).tooltipBoxes(),((LegacyOptions)minecraft.options).controllerIcons(),minecraft.options.attackIndicator(),((LegacyOptions)minecraft.options).hudScale(),((LegacyOptions)minecraft.options).hudOpacity(),((LegacyOptions)minecraft.options).inGameTooltips(),((LegacyOptions)minecraft.options).legacyItemTooltips(),((LegacyOptions)minecraft.options).vignette(),((LegacyOptions)minecraft.options).forceYellowText(),((LegacyOptions)minecraft.options).animatedCharacter(),((LegacyOptions)minecraft.options).smoothAnimatedCharacter(),((LegacyOptions)minecraft.options).interfaceSensitivity(),((LegacyOptions)minecraft.options).classicCrafting(), ((LegacyOptions)minecraft.options).legacyCreativeTab(),((LegacyOptions)minecraft.options).vanillaTabs(), minecraft.options.operatorItemsTab(), minecraft.options.narrator(), minecraft.options.showSubtitles(), minecraft.options.highContrast(), minecraft.options.notificationDisplayTime(), minecraft.options.damageTiltStrength(), minecraft.options.glintSpeed(), minecraft.options.glintStrength(), minecraft.options.hideLightningFlash(), minecraft.options.darkMojangStudiosBackground(), minecraft.options.panoramaSpeed(), minecraft.options.narratorHotkey(),minecraft.options.chatVisibility(), minecraft.options.chatColors(), minecraft.options.chatLinks(), minecraft.options.chatLinksPrompt(),  minecraft.options.backgroundForChatOnly(), minecraft.options.chatOpacity(), minecraft.options.textBackgroundOpacity(), minecraft.options.chatScale(), minecraft.options.chatLineSpacing(), minecraft.options.chatDelay(), minecraft.options.chatWidth(), minecraft.options.chatHeightFocused(), minecraft.options.chatHeightUnfocused(), minecraft.options.narrator(), minecraft.options.autoSuggestions(), minecraft.options.hideMatchedNames(), minecraft.options.reducedDebugInfo(), minecraft.options.onlyShowSecureChat());
                    s.renderableVList.renderables.add(8,interfaceResolutionWidget);
                    s.renderableVList.renderables.add(8,autoResolutionWidget);
                    return s;
                }
        ).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.reset_defaults"),()->new ConfirmationScreen(this,Component.translatable("legacy.menu.reset_settings"),Component.translatable("legacy.menu.reset_message"), b1->{
            Legacy4JClient.resetVanillaOptions(minecraft);
            minecraft.setScreen(this);
        })).build());
    }

}
