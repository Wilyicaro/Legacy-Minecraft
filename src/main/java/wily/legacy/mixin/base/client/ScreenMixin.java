package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.NavigationElement;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.LegacyLoading;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {
    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected Minecraft minecraft;

    public Screen self() {
        return (Screen) (Object) this;
    }

    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"))
    private void renderWithTooltip(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(LegacyTipManager.getTipXOffset(), 0);
    }

    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("RETURN"))
    private void renderWithTooltipReturn(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        guiGraphics.pose().translate(-LegacyTipManager.getTipXOffset(), 0);
        ControlTooltip.Renderer.of(this).render(guiGraphics, i, j, f);
        guiGraphics.pose().popMatrix();
    }

    @Inject(method = "changeFocus", at = @At("HEAD"))
    private void changeFocus(ComponentPath componentPath, CallbackInfo ci) {
        if (componentPath instanceof ComponentPath.Path path)
            NavigationElement.of(path.childPath().component()).playFocusSound(path);
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        if (!(self() instanceof LegacyLoading))
            LegacySoundUtil.playBackSound();
    }

    //? if >1.20.1 {
    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    public void renderTransparentBackground(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();
        if (self() instanceof AbstractContainerScreen<?> && !LegacyOptions.menusWithBackground.get()) return;
        LegacyRenderUtil.renderTransparentBackground(graphics);
    }

    //?}
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    public void renderBackground(GuiGraphics guiGraphics, /*? if >1.20.1 {*/int i, int j, float f,/*?}*/ CallbackInfo ci) {
        ci.cancel();
        if (UIAccessor.of(self()).getBoolean("hasBackground", true) && (!(self() instanceof AbstractContainerScreen<?>) || LegacyOptions.menusWithBackground.get())) {
            LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(self()), guiGraphics, false);
        }
    }

    //? if >=1.20.5 {
    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
    public void renderPanorama(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        ci.cancel();
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(self()), guiGraphics, true, false, !(self() instanceof TitleScreen));
    }

    //?}
    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (Legacy4JClient.keyToggleCursor.matches(keyEvent)) Legacy4JClient.controllerManager.toggleCursor();
    }

    @Redirect(method = "rebuildWidgets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;clearFocus()V"))
    public void rebuildWidgets(Screen instance) {
    }

    @Inject(method = "setInitialFocus(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At(value = "HEAD"), cancellable = true)
    public void setInitialFocus(GuiEventListener guiEventListener, CallbackInfo ci) {
        if (getFocused() == null) setFocused(guiEventListener);
        ci.cancel();
    }

    //? if <1.20.5 {
    /*@Inject(method = "rebuildWidgets",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init()V",shift = At.Shift.AFTER))
    public void rebuildWidgetsInitialFocus(CallbackInfo ci) {
        Legacy4JClient.postScreenInit((Screen) (Object)this);
    }

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init()V", shift = At.Shift.AFTER))
    public void init(Minecraft minecraft, int i, int j, CallbackInfo ci) {
        Legacy4JClient.postScreenInit((Screen) (Object)this);
    }
    *///?} else {
    @Inject(method = "rebuildWidgets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;setInitialFocus()V", shift = At.Shift.AFTER))
    public void rebuildWidgetsInitialFocus(CallbackInfo ci) {
        Legacy4JClient.postScreenInit((Screen) (Object) this);
    }

    @Inject(method = "setInitialFocus()V", at = @At(value = "HEAD"), cancellable = true)
    public void setInitialFocus(CallbackInfo ci) {
        ci.cancel();
        Legacy4JClient.postScreenInit((Screen) (Object) this);
    }

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;setInitialFocus()V", shift = At.Shift.AFTER))
    public void init(Minecraft minecraft, int i, int j, CallbackInfo ci) {
        Legacy4JClient.postScreenInit((Screen) (Object) this);
    }
    //?}

}
