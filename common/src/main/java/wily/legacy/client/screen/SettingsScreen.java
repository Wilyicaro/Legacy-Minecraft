package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.client.LegacyOptions;

import java.util.Arrays;
import java.util.Optional;

public class SettingsScreen extends RenderableVListScreen {

    protected SettingsScreen(Screen parent) {
        super(parent,Component.translatable("legacy.menu.settings"), r->{});
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.game_options"), ()-> {
            PanelVListScreen screen = new PanelVListScreen(this,250,162,Component.translatable("legacy.menu.game_options"),minecraft.options.autoJump(),minecraft.options.toggleCrouch(),minecraft.options.toggleSprint(),((LegacyOptions)minecraft.options).autoSaveWhenPause(),((LegacyOptions)minecraft.options).autoSaveInterval(),minecraft.options.bobView(),minecraft.options.sensitivity(),minecraft.options.mouseWheelSensitivity());
            if (minecraft.level != null && minecraft.hasSingleplayerServer()) screen.renderableVList.addRenderable(new LegacySliderButton<>(0,0, 220,16, b -> b.getDefaultMessage(Component.translatable("options.difficulty"),b.getValue().getDisplayName()),()-> Tooltip.create(minecraft.level.getDifficulty().getInfo()),minecraft.level.getDifficulty(),()-> Arrays.asList(Difficulty.values()), b->minecraft.getConnection().send(new ServerboundChangeDifficultyPacket(b.objectValue))));
            else screen.renderableVList.addRenderable(Button.builder(Component.translatable("options.online"), button -> this.minecraft.setScreen(OnlineOptionsScreen.createOnlineOptionsScreen(this.minecraft, this, minecraft.options))).build());
            return screen;
        }).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.audio"),()->new PanelVListScreen(this,250,218,Component.translatable("legacy.menu.audio"), Arrays.stream(SoundSource.values()).map(s-> minecraft.options.getSoundSourceOptionInstance(s).createButton(minecraft.options,0,0,0)).toArray(AbstractWidget[]::new))).build());
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
            PackSelector selector = PackSelector.resources(0,0,230,45);
            PanelVListScreen  s = new PanelVListScreen(this,250,215,Component.translatable("legacy.menu.graphics"),optionInstance,minecraft.options.graphicsMode(), minecraft.options.renderDistance(), minecraft.options.prioritizeChunkUpdates(), minecraft.options.simulationDistance(), minecraft.options.ambientOcclusion(), minecraft.options.framerateLimit(), minecraft.options.enableVsync(), minecraft.options.bobView(), minecraft.options.gamma(), ((LegacyOptions)minecraft.options).legacyGamma(),minecraft.options.cloudStatus(), minecraft.options.fullscreen(), minecraft.options.particles(), minecraft.options.mipmapLevels(), minecraft.options.entityShadows(), minecraft.options.screenEffectScale(), minecraft.options.entityDistanceScaling(), minecraft.options.fov(),minecraft.options.fovEffectScale(),minecraft.options.glintSpeed(), minecraft.options.glintStrength()){
                public void onClose() {
                    super.onClose();
                    selector.applyChanges(true);
                }
            };
            s.renderableVList.addRenderable(selector);
            return s;
        }).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.user_interface"),()-> new PanelVListScreen(this,250,200,Component.translatable("legacy.menu.user_interface"), ((LegacyOptions)minecraft.options).displayHUD(),minecraft.options.showAutosaveIndicator(),((LegacyOptions)minecraft.options).showVanillaRecipeBook(),minecraft.options.attackIndicator(), minecraft.options.guiScale(),((LegacyOptions)minecraft.options).hudDistance(),((LegacyOptions)minecraft.options).hudOpacity(),((LegacyOptions)minecraft.options).inGameTooltips(),((LegacyOptions)minecraft.options).legacyCreativeTab(),((LegacyOptions)minecraft.options).animatedCharacter(),((LegacyOptions)minecraft.options).classicCrafting(), minecraft.options.operatorItemsTab(), minecraft.options.narrator(), minecraft.options.showSubtitles(), minecraft.options.highContrast(), minecraft.options.notificationDisplayTime(), minecraft.options.screenEffectScale(), minecraft.options.fovEffectScale(), minecraft.options.darknessEffectScale(), minecraft.options.damageTiltStrength(), minecraft.options.glintSpeed(), minecraft.options.glintStrength(), minecraft.options.hideLightningFlash(), minecraft.options.darkMojangStudiosBackground(), minecraft.options.panoramaSpeed(), minecraft.options.narratorHotkey(),minecraft.options.chatVisibility(), minecraft.options.chatColors(), minecraft.options.chatLinks(), minecraft.options.chatLinksPrompt(), minecraft.options.chatOpacity(), minecraft.options.textBackgroundOpacity(), minecraft.options.chatScale(), minecraft.options.chatLineSpacing(), minecraft.options.chatDelay(), minecraft.options.chatWidth(), minecraft.options.chatHeightFocused(), minecraft.options.chatHeightUnfocused(), minecraft.options.narrator(), minecraft.options.autoSuggestions(), minecraft.options.hideMatchedNames(), minecraft.options.reducedDebugInfo(), minecraft.options.onlyShowSecureChat())).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.reset_defaults"),()->new ConfirmationScreen(this,Component.translatable("legacy.menu.reset_settings"),Component.translatable("legacy.menu.reset_message"), b1->{
            LegacyMinecraftClient.resetVanillaOptions(minecraft);
            minecraft.setScreen(this);
        })).build());
    }
}
