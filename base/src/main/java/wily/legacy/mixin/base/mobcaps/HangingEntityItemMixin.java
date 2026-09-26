package wily.legacy.mixin.base.mobcaps;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(HangingEntityItem.class)
public class HangingEntityItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void gateHangingPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null || !(context.getLevel() instanceof ServerLevel serverLevel) || ConsoleMobCaps.canPlaceHanging(serverLevel)) {
            return;
        }

        ConsoleMobCaps.sendFailure(player, ConsoleMobCaps.maxHangingMessage());
        cir.setReturnValue(InteractionResult.FAIL);
    }
}
