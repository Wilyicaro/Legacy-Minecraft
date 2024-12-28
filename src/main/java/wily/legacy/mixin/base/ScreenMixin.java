package wily.legacy.mixin.base;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.KeyboardScreen;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.ScreenUtil;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {
    @Shadow public int width;

    @Shadow public int height;

    @Shadow protected Minecraft minecraft;

    public Screen self(){
        return (Screen) (Object)this;
    }

    @Inject(method = "renderWithTooltip",at = @At("HEAD"))
    private void renderWithTooltip(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci){
        LegacyTipManager.tipDiffPercentage = Math.max(-0.5f,Math.min(LegacyTipManager.tipDiffPercentage + (LegacyTipManager.getActualTip() == null ? -0.1f : 0.08f) * f,1.5f));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(LegacyTipManager.getTipXDiff(),0,0);
    }
    @Inject(method = "renderWithTooltip",at = @At("RETURN"))
    private void renderWithTooltipReturn(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci){
        guiGraphics.pose().translate(-LegacyTipManager.getTipXDiff(),0,0);
        guiGraphics.flush();
        ControlTooltip.Renderer.of(this).render(guiGraphics,i,j,f);
        guiGraphics.pose().popPose();
    }


    @Inject(method = "changeFocus",at = @At("HEAD"))
    private void changeFocus(ComponentPath componentPath, CallbackInfo ci){
        ScreenUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(),true);
    }
    @Inject(method = "onClose",at = @At("HEAD"))
    private void onClose(CallbackInfo ci){
        ScreenUtil.playSimpleUISound(LegacyRegistries.BACK.get(),1.0f);
    }
    //? if >1.20.1 {
    @Inject(method = "renderTransparentBackground",at = @At("HEAD"), cancellable = true)
    public void renderTransparentBackground(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();
        if (self() instanceof AbstractContainerScreen<?>) return;
        ScreenUtil.renderTransparentBackground(graphics);
    }
    //?} else {
    //?}
    @Inject(method = "renderBackground",at = @At("HEAD"), cancellable = true)
    public void renderBackground(GuiGraphics guiGraphics, /*? if >1.20.1 {*/int i, int j, float f,/*?}*/ CallbackInfo ci) {
        ScreenUtil.renderDefaultBackground(UIDefinition.Accessor.of(self()), guiGraphics, false);
        ci.cancel();
    }
    //? if >=1.20.5 {
    @Inject(method = "renderPanorama",at = @At("HEAD"), cancellable = true)
    public void renderPanorama(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderDefaultBackground(UIDefinition.Accessor.of(self()), guiGraphics, true, false, true);
    }
    //?}
    @Inject(method = "hasShiftDown",at = @At("HEAD"), cancellable = true)
    private static void hasShiftDown(CallbackInfoReturnable<Boolean> cir){
        if (Minecraft.getInstance().screen instanceof KeyboardScreen s && s.shift) cir.setReturnValue(true);
    }
    @Inject(method = "keyPressed",at = @At("HEAD"))
    private void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir){
        if (Legacy4JClient.keyToggleCursor.matches(i,j)) Legacy4JClient.controllerManager.toggleCursor();
    }
    @Redirect(method = "rebuildWidgets",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;clearFocus()V"))
    public void rebuildWidgets(Screen instance) {
    }
    @Inject(method = "setInitialFocus(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V",at = @At(value = "HEAD"), cancellable = true)
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
    @Inject(method = "rebuildWidgets",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;setInitialFocus()V",shift = At.Shift.AFTER))
    public void rebuildWidgetsInitialFocus(CallbackInfo ci) {
        Legacy4JClient.postScreenInit((Screen) (Object)this);
    }

    @Inject(method = "setInitialFocus()V",at = @At(value = "HEAD"), cancellable = true)
    public void setInitialFocus(CallbackInfo ci) {
        ci.cancel();
        Legacy4JClient.postScreenInit((Screen) (Object)this);
    }
    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;setInitialFocus()V", shift = At.Shift.AFTER))
    public void init(Minecraft minecraft, int i, int j, CallbackInfo ci) {
        Legacy4JClient.postScreenInit((Screen) (Object)this);
    }
    //?}

}
