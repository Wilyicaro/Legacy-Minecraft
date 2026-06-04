package wily.legacy.mixin.base;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.LegacyItemUtil;

@Mixin(Wolf.class)
public class WolfMixin {
    @Shadow
    private void setCollarColor(DyeColor dyeColor) {
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void mobInteract(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf wolf = (Wolf) (Object) this;
        ItemStack itemStack = player.getItemInHand(interactionHand);
        DyeColor color = LegacyItemUtil.getDyeColorOrNull(itemStack.getItem());
        if (color == null || itemStack.getItem() == LegacyItemUtil.getDyeItem(color) || !wolf.isTame() || !wolf.isOwnedBy(player) || wolf.getCollarColor() == color) {
            return;
        }
        if (!wolf.level().isClientSide()) {
            setCollarColor(color);
            itemStack.shrink(1);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
