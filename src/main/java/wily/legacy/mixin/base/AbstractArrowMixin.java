package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Unique
    private static final double LEGACY_PISTON_TRIDENT_EXTRA_REACH = 0.5;
    @Unique
    private static final double LEGACY_PISTON_TRIDENT_HIT_MARGIN = 1.25;

    @Unique
    private Vec3 pistonMoveStart;

    @Shadow
    protected abstract boolean canHitEntity(Entity entity);

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;shouldFall()Z"))
    private boolean move(boolean original, MoverType moverType) {
        return original && !(moverType == MoverType.PISTON && (Object) this instanceof ThrownTrident);
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void moveHead(MoverType moverType, Vec3 vec3, CallbackInfo ci) {
        pistonMoveStart = moverType == MoverType.PISTON && (Object) this instanceof ThrownTrident ? ((AbstractArrow) (Object) this).position() : null;
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void moveTail(MoverType moverType, Vec3 vec3, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (!(arrow instanceof ThrownTrident) || moverType != MoverType.PISTON || arrow.level().isClientSide() || pistonMoveStart == null) return;
        Vec3 position = arrow.position();
        Vec3 sweep = position.subtract(pistonMoveStart);
        if (sweep.lengthSqr() <= 0) return;
        Vec3 extra = sweep.normalize().scale(LEGACY_PISTON_TRIDENT_EXTRA_REACH);
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(arrow, pistonMoveStart.subtract(extra), position.add(extra), arrow.getBoundingBox().expandTowards(pistonMoveStart.subtract(position)).inflate(LEGACY_PISTON_TRIDENT_HIT_MARGIN), e -> canHitEntity(e) && (!(e instanceof LivingEntity living) || living.invulnerableTime <= 10), LEGACY_PISTON_TRIDENT_HIT_MARGIN);
        if (hitResult != null && arrow.isAlive() && !arrow.isNoPhysics()) ((ProjectileInvoker) this).legacy$hitTargetOrDeflectSelf(hitResult);
    }
}
