package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputWithModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.KeyboardScreen;

@Mixin(InputWithModifiers.class)
public interface InputWithModifiersMixin {
    //TODO use @Overwrite if Forge still doesn't support mixin in interfaces...
    @Inject(method = "hasShiftDown",at = @At("HEAD"), cancellable = true)
    private void hasShiftDown(CallbackInfoReturnable<Boolean> cir) {
        if (Legacy4JClient.controllerManager.simulateShift || Minecraft.getInstance().screen instanceof KeyboardScreen s && s.shift) cir.setReturnValue(true);
    }
}
