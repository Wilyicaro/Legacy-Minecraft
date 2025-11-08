package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    public void keyPress(long l, int i, KeyEvent keyEvent, CallbackInfo ci) {
        if (l == Minecraft.getInstance().getWindow().handle() && !Legacy4JClient.controllerManager.isControllerSimulatingInput)
            Legacy4JClient.controllerManager.setControllerTheLastInput(false);
    }

    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
    public boolean screenKeyPress(Screen instance, KeyEvent keyEvent, Operation<Boolean> original) {
        return Minecraft.getInstance().getOverlay() == null && original.call(instance, keyEvent);
    }

    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z"))
    public boolean screenKeyRelease(Screen instance, KeyEvent keyEvent, Operation<Boolean> original) {
        return Minecraft.getInstance().getOverlay() == null && original.call(instance, keyEvent);
    }
}
