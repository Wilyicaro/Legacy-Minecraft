//? if <1.21.2 {
package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Explosion.class)
public abstract class ServerExplosionMixin {
    private static final float LEGACY_OCCLUDED_KNOCKBACK = 0.25F;

    @Shadow
    @Final
    private double x;

    @Shadow
    @Final
    private double y;

    @Shadow
    @Final
    private double z;

    @ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F"))
    private float legacy$occludedKnockback(float exposure) {
        return exposure == 0.0F ? LEGACY_OCCLUDED_KNOCKBACK : exposure;
    }

    @WrapOperation(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean legacy$hurtEntity(Entity entity, DamageSource source, float amount, Operation<Boolean> original) {
        return legacy$getExposure(entity) > 0.0F && original.call(entity, source, amount);
    }

    private float legacy$getExposure(Entity entity) {
        return Explosion.getSeenPercent(new Vec3(x, y, z), entity);
    }
}
//?}

//? if >=1.21.2 {
/*package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    private static final float LEGACY_OCCLUDED_KNOCKBACK = 0.25F;

    @Shadow @Final private ServerLevel level;

    @Shadow @Final private Vec3 center;

    @Shadow @Final private float radius;

    @ModifyExpressionValue(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F"))
    private float legacy$occludedKnockback(float exposure) {
        return exposure == 0.0F ? LEGACY_OCCLUDED_KNOCKBACK : exposure;
    }

    @WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean legacy$hurtEntity(Entity entity, ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        return legacy$getExposure(entity) > 0.0F && original.call(entity, level, source, amount);
    }

    private float legacy$getExposure(Entity entity) {
        return ServerExplosion.getSeenPercent(center, entity);
    }

    @Inject(method = "interactWithBlocks", at = @At("RETURN"))
    private void explode(List<BlockPos> list, CallbackInfo ci){
        for (ServerPlayer player : level.players()) {
            double d = Math.sqrt(player.distanceToSqr(center));
            if (d >= 21) return;
            for (BlockPos blockPos : list) {
                if (level.random.nextInt(d >= 15 ? 40 : 10) != 0) continue;
                double d0 = blockPos.getX() + level.random.nextFloat();
                double d1 = blockPos.getY() + level.random.nextFloat();
                double d2 = blockPos.getZ() + level.random.nextFloat();
                double d3 = d0 - center.x;
                double d4 = d1 - center.y;
                double d5 = d2 - center.z;
                double d6 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
                d3 = d3 / d6;
                d4 = d4 / d6;
                d5 = d5 / d6;
                double d7 = 0.5D / (d6 / (double)this.radius + 0.1D);
                d7 = d7 * (double)(level.random.nextFloat() * level.random.nextFloat() + 0.3F);
                d3 = d3 * d7;
                d4 = d4 * d7;
                d5 = d5 * d7;
                if (d <= 18) level.sendParticles(player, ParticleTypes.POOF, false,/^? if >=1.21.4 {^//^false, ^//^?}^/ (d0 + center.x) / 2.0D, (d1 + center.y) / 2.0D, (d2 + center.z) / 2.0D, 0, d3, d4, d5, 1);
                level.sendParticles(player, ParticleTypes.SMOKE, false,/^? if >=1.21.4 {^//^false, ^//^?}^/ d0, d1, d2, 0, d3, d4, d5,1);
            }
        }
    }
}
*///?}
