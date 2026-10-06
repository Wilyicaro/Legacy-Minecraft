package wily.legacy.client.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import wily.factoryapi.base.RegisterListing;

import java.util.*;
import java.util.function.Supplier;

public interface RecipeInfo<T> extends RegisterListing.Holder<T> {
    static <T> RecipeInfo<T> create(ResourceKey<Recipe<?>> id, T value, List<Optional<Ingredient>> ings, ItemStack result) {
        return create(id.identifier(), value, ings, result, () -> null);
    }

    static <T> RecipeInfo<T> create(Identifier id, T value, List<Optional<Ingredient>> ings, ItemStack result) {
        return create(id, value, ings, result, () -> null);
    }

    static <T> RecipeInfo<T> create(Identifier id, T value, List<Optional<Ingredient>> ings, ItemStack result, Supplier<Component> description) {
        return create(id, value, ings, result, result.getHoverName(), description);
    }

    static <T> RecipeInfo<T> create(Identifier id, T value, List<Optional<Ingredient>> ings, ItemStack result, Component name, Supplier<Component> description) {
        return new RecipeInfo<>() {

            @Override
            public T get() {
                return value;
            }

            @Override
            public Identifier getId() {
                return id;
            }

            @Override
            public List<Optional<Ingredient>> getOptionalIngredients() {
                return ings;
            }

            public ItemStack getResultItem() {
                return result;
            }

            @Override
            public Component getName() {
                return name;
            }

            @Override
            public Component getDescription() {
                return description.get();
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj) || obj instanceof RecipeHolder<?> h && h.id().equals(getId()) || obj instanceof RecipeInfo<?> h1 && h1.getId().equals(getId());
            }
        };
    }

    default boolean isInvalid() {
        return get() instanceof Recipe<?> r && r.isSpecial() || getOptionalIngredients().isEmpty() || getResultItem().isEmpty();
    }

    default boolean isOverride() {
        return get() == null;
    }

    List<Optional<Ingredient>> getOptionalIngredients();

    ItemStack getResultItem();

    Component getName();

    Component getDescription();

}
