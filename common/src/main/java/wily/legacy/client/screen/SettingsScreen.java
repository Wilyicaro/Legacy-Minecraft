package wily.legacy.client.screen;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ControlType;
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
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.game_options"), ()-> prepareGameOptionsScreen(new PanelVListScreen(this,250,182,Component.translatable("legacy.menu.game_options"),minecraft.options.autoJump(),minecraft.options.bobView(),((LegacyOptions)minecraft.options).flyingViewRolling(),minecraft.options.toggleCrouch(),minecraft.options.toggleSprint(),((LegacyOptions)minecraft.options).hints(),((LegacyOptions)minecraft.options).displayNameTagBorder(),((LegacyOptions)minecraft.options).autoSaveInterval(),((LegacyOptions)minecraft.options).directSaveLoad(),((LegacyOptions)minecraft.options).legacyCreativeBlockPlacing(),((LegacyOptions)minecraft.options).cursorMode(),minecraft.options.sensitivity(),minecraft.options.realmsNotifications(), minecraft.options.allowServerListing()),minecraft)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.audio"),()->prepareAudioScreen(new PanelVListScreen(this,250,218,Component.translatable("legacy.menu.audio"), Streams.concat(Arrays.stream(SoundSource.values()).sorted(Comparator.comparingInt(s->s == SoundSource.MUSIC ? 0 : 1)).map(minecraft.options::getSoundSourceOptionInstance), Stream.of(((LegacyOptions)minecraft.options).caveSounds(),((LegacyOptions)minecraft.options).minecartSounds())).map(s-> s.createButton(minecraft.options,0,0,0)).toArray(AbstractWidget[]::new)))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.graphics"),()-> prepareGraphicsScreen(new PanelVListScreen(this,250,215,Component.translatable("legacy.menu.graphics"),minecraft.options.biomeBlendRadius(),minecraft.options.graphicsMode(), minecraft.options.renderDistance(), minecraft.options.prioritizeChunkUpdates(), minecraft.options.simulationDistance(),((LegacyOptions)minecraft.options).overrideTerrainFogStart(),((LegacyOptions)minecraft.options).terrainFogStart(),((LegacyOptions)minecraft.options).terrainFogEnd(), minecraft.options.ambientOcclusion(), minecraft.options.framerateLimit(), minecraft.options.enableVsync(), minecraft.options.bobView(), minecraft.options.gamma(),minecraft.options.cloudStatus(), minecraft.options.fullscreen(), minecraft.options.particles(), minecraft.options.mipmapLevels(), minecraft.options.entityShadows(), minecraft.options.screenEffectScale(), minecraft.options.entityDistanceScaling(), minecraft.options.fov(),minecraft.options.fovEffectScale(),minecraft.options.darknessEffectScale(),minecraft.options.glintSpeed(), minecraft.options.glintStrength()){
            @Override
            public void addControlTooltips(Renderer renderer) {
                super.addControlTooltips(renderer);
                renderer.add(()-> ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{getKeyIcon(InputConstants.KEY_LSHIFT), PLUS_ICON,getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT)}) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> getFocused() instanceof PackSelector ? getAction("legacy.action.resource_packs_screen") : null);
            }
        })).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.user_interface"),()-> prepareUserInterfaceScreen(new PanelVListScreen(this,250,200,Component.translatable("legacy.menu.user_interface"),((LegacyOptions)minecraft.options).displayHUD(),((LegacyOptions)minecraft.options).displayHand(),minecraft.options.showAutosaveIndicator(),((LegacyOptions)minecraft.options).showVanillaRecipeBook(),((LegacyOptions)minecraft.options).tooltipBoxes(),((LegacyOptions)minecraft.options).selectedControlIcons(),minecraft.options.attackIndicator(),((LegacyOptions)minecraft.options).hudScale(),((LegacyOptions)minecraft.options).hudOpacity(),((LegacyOptions)minecraft.options).hudDistance(),((LegacyOptions)minecraft.options).inGameTooltips(),((LegacyOptions)minecraft.options).legacyItemTooltips(),((LegacyOptions)minecraft.options).vignette(),((LegacyOptions)minecraft.options).forceYellowText(),((LegacyOptions)minecraft.options).animatedCharacter(),((LegacyOptions)minecraft.options).smoothAnimatedCharacter(),((LegacyOptions)minecraft.options).interfaceSensitivity(),((LegacyOptions)minecraft.options).classicCrafting(), ((LegacyOptions)minecraft.options).legacyCreativeTab(),((LegacyOptions)minecraft.options).vanillaTabs(), minecraft.options.operatorItemsTab(), minecraft.options.narrator(), minecraft.options.showSubtitles(), minecraft.options.highContrast(), minecraft.options.notificationDisplayTime(), minecraft.options.damageTiltStrength(), minecraft.options.glintSpeed(), minecraft.options.glintStrength(), minecraft.options.hideLightningFlash(), minecraft.options.darkMojangStudiosBackground(), minecraft.options.panoramaSpeed(),minecraft.options.chatVisibility(), minecraft.options.chatColors(), minecraft.options.chatLinks(), minecraft.options.chatLinksPrompt(),  minecraft.options.backgroundForChatOnly(), minecraft.options.chatOpacity(), minecraft.options.textBackgroundOpacity(), minecraft.options.chatScale(), minecraft.options.chatLineSpacing(), minecraft.options.chatDelay(), minecraft.options.chatWidth(), minecraft.options.chatHeightFocused(), minecraft.options.chatHeightUnfocused(), minecraft.options.narrator(), minecraft.options.autoSuggestions(), minecraft.options.hideMatchedNames(), minecraft.options.reducedDebugInfo(), minecraft.options.onlyShowSecureChat()))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.reset_defaults"),()->new ConfirmationScreen(this,Component.translatable("legacy.menu.reset_settings"),Component.translatable("legacy.menu.reset_message"), b1->{
            Legacy4JClient.resetVanillaOptions(minecraft);
            minecraft.setScreen(this);
        })).build());
    }
    public PanelVListScreen prepareGameOptionsScreen(PanelVListScreen s, Minecraft minecraft){
        s.renderableVList.addRenderable(openScreenButton(Component.translatable("options.language"), () -> new LegacyLanguageScreen(s, minecraft.getLanguageManager())).build());
        s.renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.mods"), () -> new ModsScreen(s)).build());
        if (minecraft.level == null && !minecraft.hasSingleplayerServer()) s.renderableVList.addOptions(((LegacyOptions)minecraft.options).createWorldDifficulty());
        return s;
    }
    public PanelVListScreen prepareAudioScreen(PanelVListScreen screen){
        return screen;
    }
    public PanelVListScreen prepareGraphicsScreen(PanelVListScreen screen){
        Monitor monitor = minecraft.getWindow().findBestMonitor();
        int j = monitor == null ? -1: minecraft.getWindow().getPreferredFullscreenVideoMode().map(monitor::getVideoModeIndex).orElse(-1);
        screen.renderableVList.addOptions(0,new OptionInstance<>("options.fullscreen.resolution", OptionInstance.noTooltip(), (component, integer) -> {
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
        }));
        screen.renderableVList.addLinkedOptions(14,((LegacyOptions)minecraft.options).displayLegacyGamma(), OptionInstance::get, ((LegacyOptions)minecraft.options).legacyGamma());
        PackSelector selector = PackSelector.resources(0,0,230,45,true);
        screen.renderableVList.addRenderable(selector);
        screen.onClose = s-> selector.applyChanges(true);
        return screen;
    }
    public PanelVListScreen prepareUserInterfaceScreen(PanelVListScreen screen){
        screen.renderableVList.addLinkedOptions(9,((LegacyOptions)minecraft.options).autoResolution(), b-> !b.get(),((LegacyOptions)minecraft.options).interfaceResolution());
        return screen;
    }

}
