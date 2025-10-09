package wily.legacy.mixin.base.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
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
        if (l == Minecraft.getInstance().getWindow().handle() && !Legacy4JClient.controllerManager.isControllerSimulatingInput) Legacy4JClient.controllerManager.setControllerTheLastInput(false);
    }
}
