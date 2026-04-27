package wily.legacy.mixin.base;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public interface FireworkRocketRecipeAccessor {
    static Ingredient getPaperIngredient() {
        return Ingredient.of(Items.PAPER);
    }

    static Ingredient getGunpowderIngredient() {
        return Ingredient.of(Items.GUNPOWDER);
    }
}
