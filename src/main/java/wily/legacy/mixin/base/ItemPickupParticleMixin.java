package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;

@Mixin(ItemPickupParticle.class)
public abstract class ItemPickupParticleMixin extends Particle {
    @Shadow private int life;
    @Unique
    private int lifetime = 3;

    protected ItemPickupParticleMixin(ClientLevel clientLevel, double d, double e, double f) {
        super(clientLevel, d, e, f);
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void init(CallbackInfo info) {
        if (LegacyOptions.legacyItemPickup.get()) lifetime += level.random.nextInt(8);
    }

    @ModifyVariable(method = "render", at = @At("STORE"), index = 4)
    public float render(float original, @Local(ordinal = 0, argsOnly = true) float partialTick) {
        return LegacyOptions.legacyItemPickup.get() ? ((float) this.life + partialTick) / lifetime : original;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        if (LegacyOptions.legacyItemPickup.get()) {
            ci.cancel();
            ++this.life;
            if (this.life == lifetime) {
                this.remove();
            }
        }
    }

}
