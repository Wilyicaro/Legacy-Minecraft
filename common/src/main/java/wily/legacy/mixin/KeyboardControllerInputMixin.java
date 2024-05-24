package wily.legacy.mixin;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.ScreenUtil;

@Mixin(KeyboardInput.class)
public class KeyboardControllerInputMixin extends Input {
    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/player/KeyboardInput;forwardImpulse:F", ordinal = 0))
    private void calculateImpulse(KeyboardInput instance, float value) {
        BindingState.Axis leftStick = (BindingState.Axis) Legacy4JClient.controllerManager.getButtonState(ControllerBinding.LEFT_STICK);
        forwardImpulse = leftStick.pressed && (up || down) ? ScreenUtil.getLegacyOptions().smoothMovement().get() ? -leftStick.getSmoothY() : (leftStick.y > 0 ? -1 : 1) : value;
    }
    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/player/KeyboardInput;leftImpulse:F", ordinal = 0))
    private void calculateImpulseLeft(KeyboardInput instance, float value) {
        BindingState.Axis leftStick = (BindingState.Axis) Legacy4JClient.controllerManager.getButtonState(ControllerBinding.LEFT_STICK);
        leftImpulse = leftStick.pressed && (left || right)  ? ScreenUtil.getLegacyOptions().smoothMovement().get() ?  -leftStick.getSmoothX() : (leftStick.x > 0 ? -1 : 1)  : value;
    }
}
