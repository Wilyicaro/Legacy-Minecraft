package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public class AbstractHorseMixin {
    @Inject(method = "isTamed", at = @At("HEAD"), cancellable = true)
    private void isTamed(CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof SkeletonHorse) cir.setReturnValue(true);
    }

    @ModifyExpressionValue(method = {"getControllingPassenger", "canJump", "onPlayerJump"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;isSaddled()Z"))
    private boolean skeletonHorseRidesWithoutSaddle(boolean saddled) {
        return saddled || (Object)this instanceof SkeletonHorse;
    }
}
