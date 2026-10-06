package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.control.ControllerManager;
import wily.legacy.client.control.tooltip.ControlTooltipRenderer;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "xpos", at = @At("HEAD"), cancellable = true)
    private void xpos(CallbackInfoReturnable<Double> cir) {
        if (ControllerManager.getInstance().isCursorDisabled) cir.setReturnValue(-1d);
    }

    @Inject(method = "xpos", at = @At("HEAD"), cancellable = true)
    private void ypos(CallbackInfoReturnable<Double> cir) {
        if (ControllerManager.getInstance().isCursorDisabled) cir.setReturnValue(-1d);
    }

    @Inject(method = {"onMove", "onScroll"}, at = @At("HEAD"), cancellable = true)
    private void onMove(long l, double d, double e, CallbackInfo ci) {
        onChange(l, ci);
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onPress(long l, MouseButtonInfo mouseButtonInfo, int i, CallbackInfo ci) {
        onChange(l, ci);
    }

    //? if forge {
    /*@WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/event/ForgeEventFactoryClient;onScreenMouseClicked(Lnet/minecraft/client/gui/screens/Screen;DDLnet/minecraft/client/input/MouseButtonEvent;Z)Z", remap = false))
    private boolean onPress(Screen screen, double mouseX, double mouseY, MouseButtonEvent event, boolean repeate, Operation<Boolean> original) {
        ControlTooltipRenderer.of(screen).press(event, true);
        return original.call(screen, mouseX, mouseY, event, repeate);
    }

    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/event/ForgeEventFactoryClient;onScreenMouseReleased(Lnet/minecraft/client/gui/screens/Screen;DDLnet/minecraft/client/input/MouseButtonEvent;)Z", remap = false))
    private boolean onRelease(Screen screen, double mouseX, double mouseY, MouseButtonEvent event, Operation<Boolean> original) {
        ControlTooltipRenderer.of(screen).press(event, false);
        return original.call(screen, mouseX, mouseY, event);
    }
    *///?} else {
    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    private boolean onPress(Screen instance, MouseButtonEvent event, boolean b, Operation<Boolean> original) {
        ControlTooltipRenderer.of(instance).press(event, true);
        return original.call(instance, event, b);
    }

    @WrapOperation(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean onRelease(Screen instance, MouseButtonEvent event, Operation<Boolean> original) {
        ControlTooltipRenderer.of(instance).press(event, false);
        return original.call(instance, event);
    }
    //?}

    @Inject(method = "releaseMouse", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(Lcom/mojang/blaze3d/platform/Window;IDD)V", shift = At.Shift.AFTER))
    private void releaseMouse(CallbackInfo ci) {
        ControllerManager.getInstance().enableCursorAndScheduleReset();
        ControllerManager.getInstance().updateCursorInputMode();
    }

    @Unique
    private void onChange(long window, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().handle()) {
            if (!ControllerManager.getInstance().isControllerSimulatingInput)
                ControllerManager.getInstance().setControllerTheLastInput(false);
            if (ControllerManager.getInstance().isCursorDisabled) {
                if (!ControllerManager.getInstance().getCursorMode().isNever())
                    ControllerManager.getInstance().enableCursor();
                else ci.cancel();
            }
        }
    }

    //? if >=1.21.5 {
    @Inject(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("HEAD"), cancellable = true)
    private void getScaledXPos(CallbackInfoReturnable<Double> cir) {
        if (ControllerManager.getInstance().isCursorDisabled) cir.setReturnValue(-1d);
    }

    @Inject(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("HEAD"), cancellable = true)
    private void getScaledYPos(CallbackInfoReturnable<Double> cir) {
        if (ControllerManager.getInstance().isCursorDisabled) cir.setReturnValue(-1d);
    }
    //?}
}
