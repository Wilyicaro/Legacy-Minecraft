package wily.legacy.mixin.base.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;

@Mixin(Entity.class)
public abstract class ClientEntityVisibilityMixin {
    @Inject(method = "isInvisible", at = @At("RETURN"), cancellable = true)
    private void isInvisible(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && (Object) this instanceof Player player && Legacy4JClient.isHostInvisible(player)) {
            cir.setReturnValue(true);
        }
    }
}
