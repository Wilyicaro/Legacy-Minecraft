package wily.legacy.mixin.base.mobcaps;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(ArmorStandItem.class)
public class ArmorStandItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void gateArmorStandPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel) || ConsoleMobCaps.canPlaceArmorStand(serverLevel)) {
            return;
        }

        Player player = context.getPlayer();
        if (player != null) {
            ConsoleMobCaps.sendFailure(player, ConsoleMobCaps.maxArmorStandsMessage());
        }
        cir.setReturnValue(InteractionResult.FAIL);
    }
}
