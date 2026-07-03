package wily.legacy.mixin.base;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;

@Mixin(MultilineTextField.class)
public class MultilineTextFieldMixin {
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/MultilineTextField;seekCursorLine(I)V", shift = At.Shift.AFTER), cancellable = true)
    private void keyDownUpReturn(int key, CallbackInfoReturnable<Boolean> cir) {
        if (Legacy4JClient.controllerManager.isControllerTheLastInput()) cir.setReturnValue(false);
    }
}
