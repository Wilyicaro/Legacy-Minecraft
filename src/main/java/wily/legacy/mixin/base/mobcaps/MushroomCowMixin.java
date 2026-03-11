package wily.legacy.mixin.base.mobcaps;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin {
    @Shadow
    public abstract boolean readyForShearing();

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void gateMooshroomShearing(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!player.getItemInHand(hand).is(Items.SHEARS) || !readyForShearing() || ConsoleMobCaps.canShearMooshroom(serverLevel)) {
            return;
        }

        ConsoleMobCaps.sendFailure(player, ConsoleMobCaps.cantShearMooshroomMessage());
        cir.setReturnValue(InteractionResult.FAIL);
    }
}
