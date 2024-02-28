package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.LegacyMinecraftClient;

@Mixin(InputConstants.class)
public class InputConstantsMixin {
    @ModifyArg(method = "grabOrReleaseMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetInputMode(JII)V"), index = 2)
    private static int grabOrReleaseMouse(int value) {
        
        LegacyMinecraftClient.controllerHandler.isCursorDisabled = false;
        return value == GLFW.GLFW_CURSOR_NORMAL ? GLFW.GLFW_CURSOR_HIDDEN : value;
    }
}
