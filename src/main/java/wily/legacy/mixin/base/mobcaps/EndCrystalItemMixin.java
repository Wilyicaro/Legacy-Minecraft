package wily.legacy.mixin.base.mobcaps;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(EndCrystalItem.class)
public class EndCrystalItemMixin {
    @Inject(method = "useOn", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDD)Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;"), cancellable = true)
    private void gateEndCrystalPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel) || ConsoleMobCaps.canPlaceEndCrystal(serverLevel)) {
            return;
        }

        if (context.getPlayer() != null) {
            ConsoleMobCaps.sendFailure(context.getPlayer(), ConsoleMobCaps.maxEndCrystalsMessage());
        }
        cir.setReturnValue(InteractionResult.FAIL);
    }
}
