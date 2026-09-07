package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.util.LegacyTags;
import wily.legacy.world.PlayerTrustAdmin;

@Mixin(Projectile.class)
public abstract class ProjectileMixin extends Entity {
    protected ProjectileMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        Projectile projectile = (Projectile) (Object) this;
        PlayerTrustPolicy player = responsiblePlayer(projectile);
        return player != null && !player.canUseDoorsAndSwitches() || super.isIgnoringBlockTriggers();
    }

    @WrapOperation(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;onHitBlock(Lnet/minecraft/world/phys/BlockHitResult;)V"))
    private void legacy$protectProjectileBlockEffects(Projectile projectile, BlockHitResult hitResult, Operation<Void> original) {
        PlayerTrustPolicy player = responsiblePlayer(projectile);
        if (player != null && (projectile instanceof AbstractThrownPotion || projectile instanceof AbstractWindCharge || projectile instanceof SmallFireball) && !player.canBuildAndMine()) return;
        PlayerTrustAdmin.withResponsiblePlayer(player, () -> original.call(projectile, hitResult));
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void legacy$protectTriggeredBlock(BlockHitResult hitResult, CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        PlayerTrustPolicy player = responsiblePlayer(projectile);
        if (player == null) return;
        BlockState state = projectile.level().getBlockState(hitResult.getBlockPos());
        if (player.canTriggerBlock(state)) return;
        if (state.is(LegacyTags.DOORS_AND_SWITCHES)) projectile.discard();
        ci.cancel();
    }

    @Unique
    private PlayerTrustPolicy responsiblePlayer(Projectile projectile) {
        return projectile.level() instanceof ServerLevel level ? PlayerTrustAdmin.getResponsiblePlayer(level, projectile) : null;
    }
}
