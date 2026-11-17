package wily.legacy.mixin.base;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void onHitBlock(BlockHitResult hitResult, CallbackInfo ci) {
        FireworkRocketEntity firework = (FireworkRocketEntity) (Object) this;
        if (firework.isShotAtAngle() && firework.getOwner() == null && hitResult.isInside()) ci.cancel();
    }
}
