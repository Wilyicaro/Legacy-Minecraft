package wily.legacy.mixin.base;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOption;

@Mixin(Particle.class)
public class ParticleMixin {
    @Shadow protected boolean hasPhysics;

    @Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V", at = @At("RETURN"))
    private void init(ClientLevel clientLevel, double d, double e, double f, CallbackInfo ci){
        if (!LegacyOption.defaultParticlePhysics.get()) hasPhysics = false;
    }
}
