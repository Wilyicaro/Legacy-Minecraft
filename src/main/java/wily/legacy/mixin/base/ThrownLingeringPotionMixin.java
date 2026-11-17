package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.entity.PlayerTrustOwnedSource;

@Mixin(ThrownLingeringPotion.class)
public class ThrownLingeringPotionMixin {
    @WrapOperation(method = "onHitAsPotion", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDD)Lnet/minecraft/world/entity/AreaEffectCloud;"))
    private AreaEffectCloud legacy$preserveOwner(Level level, double x, double y, double z, Operation<AreaEffectCloud> original) {
        AreaEffectCloud cloud = original.call(level, x, y, z);
        PlayerTrustOwnedSource potion = (PlayerTrustOwnedSource) this;
        ((PlayerTrustOwnedSource) cloud).setResponsiblePlayer(potion.getResponsiblePlayer());
        return cloud;
    }
}
