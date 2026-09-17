package wily.legacy.client.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CollapsedRecipeInfo<T> implements RecipeInfo<T> {
    private final List<RecipeInfo<T>> recipes;
    private RecipeInfo<T> selected;
    private boolean previewLocked;

    private CollapsedRecipeInfo(List<RecipeInfo<T>> recipes) {
        this.recipes = recipes.stream().sorted(Comparator.comparing(r -> r.getId().toString())).toList();
        selected = this.recipes.getFirst();
    }

    public static <T> List<RecipeInfo<T>> group(RecipeInfoFilter filter, Iterable<RecipeInfo<T>> recipes) {
        List<List<RecipeInfo<T>>> groups = new ArrayList<>();
        for (RecipeInfo<T> recipe : recipes) {
            if (!filter.test(recipe)) continue;

            List<RecipeInfo<T>> group = recipe.isOverride() ? null : groups.stream().filter(g -> !g.getFirst().isOverride() && filter.test(g.getFirst())).findFirst().orElse(null);
            if (group == null) {
                group = new ArrayList<>();
                groups.add(group);
            }
            if (group.stream().noneMatch(r -> r.getId().equals(recipe.getId()))) group.add(recipe);
        }
        return groups.stream().map(g -> g.size() == 1 ? g.getFirst() : new CollapsedRecipeInfo<>(g)).toList();
    }

    public boolean update(Predicate<RecipeInfo<T>> canCraft, long time) {
        if (canCraft.test(selected)) return false;
        RecipeInfo<T> next = recipes.stream().filter(canCraft).findFirst().orElse(null);
        if (next != null) previewLocked = false;
        else if (!previewLocked) next = recipes.get((int) (time / 800 % recipes.size()));
        if (next == null || next == selected) return false;
        selected = next;
        return true;
    }

    private List<RecipeInfo<T>> availableRecipes(Predicate<RecipeInfo<T>> canCraft) {
        List<RecipeInfo<T>> available = recipes.stream().filter(canCraft).toList();
        return available.isEmpty() ? recipes : available;
    }

    public boolean canCycle(Predicate<RecipeInfo<T>> canCraft) {
        return availableRecipes(canCraft).size() > 1;
    }

    public boolean cycle(Predicate<RecipeInfo<T>> canCraft) {
        List<RecipeInfo<T>> available = availableRecipes(canCraft);
        if (available.size() < 2) return false;
        selected = available.get((available.indexOf(selected) + 1) % available.size());
        previewLocked = true;
        return true;
    }

    @Override
    public T get() {
        return selected.get();
    }

    @Override
    public Identifier getId() {
        return selected.getId();
    }

    @Override
    public List<Optional<Ingredient>> getOptionalIngredients() {
        return selected.getOptionalIngredients();
    }

    @Override
    public ItemStack getResultItem() {
        return selected.getResultItem();
    }

    @Override
    public Component getName() {
        return selected.getName();
    }

    @Override
    public Component getDescription() {
        return selected.getDescription();
    }
}
