package wily.legacy.mixin.base;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.LegacyItemUtil;

@Mixin(Ingredient.class)
public class IngredientMixin {
    @Shadow
    @Final
    private HolderSet<Item> values;

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void test(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (itemStack.isEmpty() || values.size() != 1) {
            return;
        }
        DyeColor color = LegacyItemUtil.getDyeColorOrNull(itemStack.getItem());
        if (color != null && itemStack.getItem() != LegacyItemUtil.getDyeItem(color) && values.contains(LegacyItemUtil.getDyeItem(color).builtInRegistryHolder())) {
            cir.setReturnValue(true);
        }
    }
}
