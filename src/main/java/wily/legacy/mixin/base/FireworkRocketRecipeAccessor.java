package wily.legacy.mixin.base;

import net.minecraft.world.item.crafting.FireworkRocketRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkRocketRecipe.class)
public interface FireworkRocketRecipeAccessor {
    @Accessor("PAPER_INGREDIENT")
    static Ingredient getPaperIngredient() {
        return null;
    }
    @Accessor("GUNPOWDER_INGREDIENT")
    static Ingredient getGunpowderIngredient() {
        return null;
    }
}
