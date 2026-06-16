//? if <1.21.2 {
package wily.legacy.mixin.base.client.elytra;

import net.minecraft.client.model.ElytraModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.ScreenUtil;

@Mixin(ElytraModel.class)
public class ElytraModelMixin {
    @Redirect(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"))
    private boolean setupAnim(LivingEntity entity) {
        return !ScreenUtil.suppressInventoryElytraPose && entity.isFallFlying();
    }
}
//?}
