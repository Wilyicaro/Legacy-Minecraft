package wily.legacy.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.inventory.LegacyIngredient;

@Mixin(Ingredient.class)
public abstract class IngredientMixin implements LegacyIngredient {
    @Shadow public abstract ItemStack[] getItems();

    @Override
    public ResourceLocation getId() {
        return LegacyIngredient.DEFAULT_ID;
    }

    @Override
    public int getCount() {
        return 1;
    }
}
