package wily.legacy.client.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public record IdRecipeFilter(Identifier id) implements RecipeInfoFilter {
    public static final Codec<IdRecipeFilter> CODEC = Identifier.CODEC.xmap(IdRecipeFilter::new, IdRecipeFilter::id);

    @Override
    public <T> void addRecipes(Iterable<RecipeInfo<T>> validRecipes, Consumer<RecipeInfo<T>> recipeAdder) {
        RecipeInfoFilter.super.addRecipes(validRecipes, recipeAdder);
        CustomRecipeAdder<T> value = CustomRecipeAdder.ID_RECIPE_INFO_OVERRIDES.get(id);
        if (value != null) value.addRecipes(validRecipes, recipeAdder);
    }

    @Override
    public boolean onlyFirstMatch() {
        return true;
    }

    @Override
    public Codec<? extends RecipeInfoFilter> codec() {
        return CODEC;
    }

    @Override
    public boolean test(RecipeInfo<?> h) {
        return h.getId().equals(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
