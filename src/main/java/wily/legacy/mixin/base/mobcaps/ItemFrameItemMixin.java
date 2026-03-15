package wily.legacy.mixin.base.mobcaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemFrameItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(ItemFrameItem.class)
public class ItemFrameItemMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void gateItemFramePlacement(Player player, Direction direction, ItemStack stack, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!(player.level() instanceof ServerLevel serverLevel) || ConsoleMobCaps.canPlaceHanging(serverLevel)) {
            return;
        }

        ConsoleMobCaps.sendFailure(player, ConsoleMobCaps.maxHangingMessage());
        cir.setReturnValue(false);
    }
}
