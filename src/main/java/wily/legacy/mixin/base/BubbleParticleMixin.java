package wily.legacy.mixin.base;

import net.minecraft.client.particle.BubbleParticle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;

@Mixin(BubbleParticle.class)
public class BubbleParticleMixin {
    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/BubbleParticle;zd:D", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), cancellable = true)
    public void tick(CallbackInfo ci) {
        if (LegacyOptions.bubblesOutsideWater.get()) ci.cancel();
    }
}
