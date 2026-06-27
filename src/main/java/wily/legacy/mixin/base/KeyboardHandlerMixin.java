package wily.legacy.mixin.base;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void keyPress(long l, int i, int j, int k, int m, CallbackInfo ci) {
        if (l == Minecraft.getInstance().getWindow().getWindow() && !Legacy4JClient.controllerManager.isControllerSimulatingInput) {
            Legacy4JClient.controllerManager.setControllerTheLastInput(false);
            Legacy4JClient.updateKeyboardToggleKeyPress(i, j, k);
            if (k != GLFW.GLFW_RELEASE && i == InputConstants.KEY_ESCAPE && !Legacy4JClient.consumeKeyboardActionKeyPress(i)) {
                ci.cancel();
            }
        }
    }
}
