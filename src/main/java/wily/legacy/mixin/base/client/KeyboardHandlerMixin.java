package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
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

    //? if forge {
    /*@WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;onScreenKeyPressed(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/input/KeyEvent;)Z", remap = false))
    public boolean screenKeyPress(Screen instance, KeyEvent keyEvent, Operation<Boolean> original) {
        return Minecraft.getInstance().getOverlay() == null && original.call(instance, keyEvent);
    }

    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;onScreenKeyReleased(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/input/KeyEvent;)Z", remap = false))
    public boolean screenKeyRelease(Screen instance, KeyEvent keyEvent, Operation<Boolean> original) {
        return Minecraft.getInstance().getOverlay() == null && original.call(instance, keyEvent);
    }

    @WrapOperation(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;onScreenCharTyped(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/input/CharacterEvent;)Z", remap = false))
    public boolean charTyped(Screen instance, CharacterEvent characterEvent, Operation<Boolean> original) {
        if (Legacy4JClient.controllerManager.blockNextCharType) {
            return Legacy4JClient.controllerManager.blockNextCharType = false;
        }
        return original.call(instance, characterEvent);
    }
    *///?} else {
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
    public boolean screenKeyPress(Screen instance, KeyEvent keyEvent, Operation<Boolean> original) {
        Legacy4JClient.controllerManager.blockNextCharType = false;
        if (Minecraft.getInstance().getOverlay() == null && original.call(instance, keyEvent)) {
            Legacy4JClient.controllerManager.blockNextCharType = true;
            return true;
        }
        return false;
    }

    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z"))
    public boolean screenKeyRelease(Screen instance, KeyEvent keyEvent, Operation<Boolean> original) {
        return Minecraft.getInstance().getOverlay() == null && original.call(instance, keyEvent);
    }

    @WrapOperation(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z"))
    public boolean charTyped(Screen instance, CharacterEvent characterEvent, Operation<Boolean> original) {
        if (Legacy4JClient.controllerManager.blockNextCharType) {
            return Legacy4JClient.controllerManager.blockNextCharType = false;
        }
        return original.call(instance, characterEvent);
    }
    //?}
}
