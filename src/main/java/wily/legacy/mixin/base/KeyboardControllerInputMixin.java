package wily.legacy.mixin.base;

//? if <1.21.2 {
import net.minecraft.client.player.Input;
//?} else {
/*import net.minecraft.client.player.ClientInput;
*///?}
import net.minecraft.client.player.KeyboardInput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;

@Mixin(KeyboardInput.class)
public class KeyboardControllerInputMixin extends /*? if <1.21.2 {*/Input/*?} else {*//*ClientInput*//*?}*/ {
    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/player/KeyboardInput;forwardImpulse:F", ordinal = 0))
    private void calculateImpulse(KeyboardInput instance, float value) {
        BindingState.Axis leftStick = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.LEFT_STICK);
        forwardImpulse = leftStick.pressed && (/*? if <1.21.2 {*/up || down/*?} else {*//*keyPresses.forward() || keyPresses.backward()*//*?}*/ ) ? LegacyOptions.smoothMovement.get() && (LegacyOptions.forceSmoothMovement.get() || FactoryAPIClient.hasModOnServer) ? -leftStick.getSmoothY() : (leftStick.y > 0 ? -1 : 1) : value;
    }
    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/player/KeyboardInput;leftImpulse:F", ordinal = 0))
    private void calculateImpulseLeft(KeyboardInput instance, float value) {
        BindingState.Axis leftStick = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.LEFT_STICK);
        leftImpulse = leftStick.pressed && (/*? if <1.21.2 {*/left || right/*?} else {*//*keyPresses.left() || keyPresses.right()*//*?}*/) ? LegacyOptions.smoothMovement.get() && (LegacyOptions.forceSmoothMovement.get() || FactoryAPIClient.hasModOnServer) ?  -leftStick.getSmoothX() : (leftStick.x > 0 ? -1 : 1) : value;
    }
}
