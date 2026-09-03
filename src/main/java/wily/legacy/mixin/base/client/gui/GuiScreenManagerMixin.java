package wily.legacy.mixin.base.client.gui;

//? if >=26.2 {
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.control.tooltip.ControlTooltipRenderer;
import wily.legacy.client.screen.LegacyAdvancementsScreen;
import wily.legacy.client.screen.LegacyLoading;
import wily.legacy.client.screen.OverlayPanelScreen;
import wily.legacy.util.client.LegacyGuiElements;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

// 26.2 split Minecraft's screen state out into Gui: setScreen, buildInitialScreens and the
// advancements keybind live here now, so these hooks moved off MinecraftMixin.
@Mixin(Gui.class)
public abstract class GuiScreenManagerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Nullable
    private Screen screen;

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Unique
    private Screen legacy$oldScreen;

    @ModifyArg(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 0))
    private Screen legacy$advancementsScreen(Screen arg) {
        return LegacyOptions.legacyAdvancements.get() ? new LegacyAdvancementsScreen(null) : arg;
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void legacy$setScreen(Screen screen, CallbackInfo ci) {
        legacy$oldScreen = this.screen;
        if (screen instanceof PauseScreen && minecraft.player != null && minecraft.player.isUsingItem()) {
            if (minecraft.gameMode != null) minecraft.gameMode.releaseUsingItem(minecraft.player);
            else minecraft.player.stopUsingItem();
        }
        Screen replacement = Legacy4JClient.getReplacementScreen(screen);
        if (replacement != screen) {
            ci.cancel();
            setScreen(replacement);
            return;
        }
        if (this.screen == null && minecraft.level != null && screen != null && !(screen instanceof LegacyLoading) && (screen instanceof PauseScreen || !screen.isPauseScreen()))
            LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
        if (screen == null && minecraft.level != null) {
            LegacyGuiElements.lastGui = Util.getMillis();
            ControlTooltip.Listener.of(minecraft.gui.hud).setupControlTooltips();
            ControlTooltipRenderer.GUI_EVENT.invoker.accept(minecraft.gui, ControlTooltip.Listener.of(minecraft.gui.hud).getControlTooltips());
        }
    }

    @WrapWithCondition(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
    private boolean legacy$removedScreen(Screen instance, Screen newScreen) {
        return !(newScreen instanceof OverlayPanelScreen s) || s.parent != instance;
    }

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
    private void legacy$addedScreen(Screen screen, CallbackInfo ci) {
        ControlTooltip.Listener.of(screen).setupControlTooltips();
        ControlTooltipRenderer.SCREEN_EVENT.invoker.accept(screen, ControlTooltip.Listener.of(screen).getControlTooltips());
        LegacyTipManager.resetTipOffset(true);
    }

    @WrapWithCondition(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(II)V"))
    private boolean legacy$initScreen(Screen instance, int i, int j) {
        if (legacy$oldScreen instanceof OverlayPanelScreen s && s.parent == instance) {
            instance.resize(i, j);
            return false;
        }
        return true;
    }

    @ModifyVariable(method = "buildInitialScreens", at = @At(value = "STORE"))
    private Runnable legacy$addInitialScreens(Runnable run) {
        return () -> {
            run.run();
            if (this.screen != null) setScreen(LegacyRenderUtil.getInitialScreen());
        };
    }
}
//?}