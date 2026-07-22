package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin implements Explosion {
    private static final double LEGACY_OCCLUDED_KNOCKBACK = 0.25;

    @Unique
    private List<BlockPos> legacy$explodedPositions = List.of();

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private Vec3 center;

    @Shadow
    @Final
    private ExplosionDamageCalculator damageCalculator;

    @Shadow
    @Final
    private float radius;

    @ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"))
    private List<BlockPos> legacy$captureExplodedPositions(List<BlockPos> positions) {
        legacy$explodedPositions = positions;
        return positions;
    }

    @WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean legacy$hurtEntity(Entity entity, ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        return legacy$intersectsBlast(entity) && original.call(entity, level, source, amount);
    }

    @ModifyArg(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"), index = 0)
    private double legacy$getKnockbackPower(double power, @Local Entity entity) {
        if (legacy$intersectsBlast(entity)) return power;
        double doubleRadius = radius * 2.0F;
        if (doubleRadius <= 0.0) return power;
        double dist = Math.sqrt(entity.distanceToSqr(center)) / doubleRadius;
        double resistance = entity instanceof LivingEntity livingEntity ? livingEntity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE) : 0.0;
        return (1.0 - dist) * damageCalculator.getKnockbackMultiplier(entity) * (1.0 - resistance) * LEGACY_OCCLUDED_KNOCKBACK;
    }

    @Unique
    private boolean legacy$intersectsBlast(Entity entity) {
        AABB bounds = entity.getBoundingBox();
        for (BlockPos position : legacy$explodedPositions) {
            if (bounds.intersects(position)) return true;
        }
        return false;
    }

    @Inject(method = "interactWithBlocks", at = @At("RETURN"))
    private void explode(List<BlockPos> list, CallbackInfo ci) {
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
                double d7 = 0.5D / (d6 / (double) this.radius + 0.1D);
                d7 = d7 * (double) (level.random.nextFloat() * level.random.nextFloat() + 0.3F);
                d3 = d3 * d7;
                d4 = d4 * d7;
                d5 = d5 * d7;
                if (d <= 18)
                    level.sendParticles(player, ParticleTypes.POOF, false, false, (d0 + center.x) / 2.0D, (d1 + center.y) / 2.0D, (d2 + center.z) / 2.0D, 0, d3, d4, d5, 1);
                level.sendParticles(player, ParticleTypes.SMOKE, false, false, d0, d1, d2, 0, d3, d4, d5, 1);
            }
        }
    }
}
