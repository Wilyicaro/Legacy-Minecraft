package wily.legacy.mixin;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.util.ScreenUtil;

import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {
    @Shadow public int width;

    @Shadow public int height;


    @Inject(method = "changeFocus",at = @At("HEAD"))
    private void render(ComponentPath componentPath, CallbackInfo ci){
        ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(),1.0f);
    }
    @Inject(method = "onClose",at = @At("HEAD"))
    private void onClose(CallbackInfo ci){
        ScreenUtil.playSimpleUISound(LegacySoundEvents.BACK.get(),1.0f);
    }
    @Redirect(method = "renderTransparentBackground",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIII)V"))
    public void renderTransparentBackground(GuiGraphics graphics, int i, int j, int k, int l, int m, int n) {
        graphics.fillGradient(i,j,k,l, -1073741824, -805306368);
    }
    @Redirect(method = "renderBackground",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderDirtBackground(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    public void renderBackground(Screen instance, GuiGraphics guiGraphics) {
        ScreenUtil.renderDefaultBackground(guiGraphics,true,true);
    }
    @Inject(method = "keyPressed",at = @At("HEAD"))
    private void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir){
        if (Legacy4JClient.keyToggleCursor.matches(i,j)) Legacy4JClient.controllerManager.toggleCursor();
    }
    @Redirect(method = "rebuildWidgets",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;clearFocus()V"))
    public void rebuildWidgets(Screen instance) {
    }
    @Inject(method = "setInitialFocus",at = @At(value = "HEAD"))
    public void rebuildWidgets(GuiEventListener guiEventListener, CallbackInfo ci) {
        setFocused(guiEventListener);
    }
}
