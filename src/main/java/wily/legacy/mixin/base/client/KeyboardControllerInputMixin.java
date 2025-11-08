package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;

@Mixin(KeyboardInput.class)
public class KeyboardControllerInputMixin extends ClientInput {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/KeyboardInput;calculateImpulse(ZZ)F", ordinal = 0))
    private float calculateForwardImpulse(float original) {
        BindingState.Axis leftStick = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.LEFT_STICK);
        return leftStick.pressed && (keyPresses.forward() || keyPresses.backward()) ? LegacyOptions.smoothMovement.get() && (LegacyOptions.forceSmoothMovement.get() || Legacy4JClient.hasModOnServer()) ? -leftStick.getSmoothY() : (leftStick.y > 0 ? -1 : 1) : original;
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/KeyboardInput;calculateImpulse(ZZ)F", ordinal = 1))
    private float calculateLeftImpulse(float original) {
        BindingState.Axis leftStick = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.LEFT_STICK);
        return leftStick.pressed && (keyPresses.left() || keyPresses.right()) ? LegacyOptions.smoothMovement.get() && (LegacyOptions.forceSmoothMovement.get() || Legacy4JClient.hasModOnServer()) ? -leftStick.getSmoothX() : (leftStick.x > 0 ? -1 : 1) : original;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec2;normalized()Lnet/minecraft/world/phys/Vec2;"))
    private Vec2 normalize(Vec2 instance, Operation<Vec2> original) {
        return Legacy4JClient.controllerManager.isControllerTheLastInput() && (LegacyOptions.forceSmoothMovement.get() || Legacy4JClient.hasModOnServer()) ? instance : original.call(instance);
    }
}
