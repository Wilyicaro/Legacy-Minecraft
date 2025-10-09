package wily.legacy.mixin.base.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyItemPickupParticle;
import wily.legacy.client.LegacyOptions;

@Mixin(ItemPickupParticle.class)
public abstract class ItemPickupParticleMixin extends Particle implements LegacyItemPickupParticle {
    @Shadow private int life;
    @Shadow private double targetY;
    @Shadow @Final private Entity target;
    @Unique
    private int lifetime = 3;

    protected ItemPickupParticleMixin(ClientLevel clientLevel, double d, double e, double f) {
        super(clientLevel, d, e, f);
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void init(CallbackInfo info) {
        if (LegacyOptions.legacyItemPickup.get()) lifetime += level.random.nextInt(8);
    }

    @Override
    public int getPickupLifetime() {
        return lifetime;
    }

    @Override
    public int getPickupLife() {
        return life;
    }

    @Inject(method = "updatePosition", at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/ItemPickupParticle;targetY:D", shift = At.Shift.AFTER))
    private void updatePosition(CallbackInfo ci) {
        if (LegacyOptions.legacyItemPickup.get()) targetY = target.getY();
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
