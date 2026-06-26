package wily.legacy.mixin.base;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;

@Mixin(Ingredient.class)
public class IngredientMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void test(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (itemStack == null || itemStack.isEmpty()) return;
        DyeColor color = Legacy4J.getDyeColorOrNull(itemStack.getItem());
        if (color == null) return;
        Item dyeItem = Legacy4J.getDyeItem(color);
        if (itemStack.getItem() == dyeItem) return;
        if (((Ingredient) (Object) this).test(dyeItem.getDefaultInstance())) cir.setReturnValue(true);
    }
}
