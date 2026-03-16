package wily.legacy.mixin.base.mobcaps;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(BoatItem.class)
public class BoatItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void gateBoatPlacement(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(level instanceof ServerLevel serverLevel) || ConsoleMobCaps.canPlaceBoat(serverLevel)) {
            return;
        }

        ConsoleMobCaps.sendFailure(player, ConsoleMobCaps.maxBoatsMessage());
        cir.setReturnValue(InteractionResult.FAIL);
    }
}
