package wily.legacy.mixin.base;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.LegacyItemUtil;

@Mixin(Cat.class)
public class CatMixin {
    @Shadow
    private void setCollarColor(DyeColor dyeColor) {
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void mobInteract(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        Cat cat = (Cat) (Object) this;
        ItemStack itemStack = player.getItemInHand(interactionHand);
        DyeColor color = LegacyItemUtil.getDyeColorOrNull(itemStack.getItem());
        if (color == null || itemStack.getItem() == LegacyItemUtil.getDyeItem(color) || !cat.isTame() || !cat.isOwnedBy(player) || cat.getCollarColor() == color) {
            return;
        }
        if (!cat.level().isClientSide()) {
            setCollarColor(color);
            itemStack.shrink(1);
            cat.setPersistenceRequired();
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
