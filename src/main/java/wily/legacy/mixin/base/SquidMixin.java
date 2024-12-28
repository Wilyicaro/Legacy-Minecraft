package wily.legacy.mixin.base;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Squid.class)
public abstract class SquidMixin extends PathfinderMob {
    @Shadow protected abstract Vec3 rotateVector(Vec3 vec3);

    protected SquidMixin(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void aiStep(CallbackInfo ci){
        if (!level().isClientSide || !isUnderWater() || getDeltaMovement().length() <= 0.1) return;
        Vec3 vec3 = this.rotateVector(new Vec3(0.0, -0.5, -0.5));
        this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z,  0.0,0.0,0.0);
    }
}
