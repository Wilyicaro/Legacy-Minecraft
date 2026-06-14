//? if <=1.21.1 {
package wily.legacy.mixin.base;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TippedArrowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;

@Mixin(TippedArrowItem.class)
public class TippedArrowItemMixin {
    @Inject(method = "getDescriptionId", at = @At("HEAD"), cancellable = true)
    private void getDescriptionId(ItemStack stack, CallbackInfoReturnable<String> cir) {
        String descriptionId = Legacy4J.getDecayPotionDescriptionId(stack);
        if (descriptionId != null) cir.setReturnValue(descriptionId);
    }
}
//?}
