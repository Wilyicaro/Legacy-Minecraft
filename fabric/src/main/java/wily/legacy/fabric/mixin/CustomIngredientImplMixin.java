package wily.legacy.fabric.mixin;

import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;

@Mixin(CustomIngredientImpl.class)
public abstract class CustomIngredientImplMixin {
    @Shadow public abstract ItemStack[] getItems();

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof CustomIngredientImpl i && Arrays.equals(i.getItems(), getItems());
    }
}
