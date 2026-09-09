package wily.legacy.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CraftingRecipeAlternatives implements RecipeInfo<CraftingRecipe> {
    private final List<RecipeInfo<CraftingRecipe>> recipes;
    private RecipeInfo<CraftingRecipe> selected;
    private boolean previewLocked;

    private CraftingRecipeAlternatives(List<RecipeInfo<CraftingRecipe>> recipes) {
        this.recipes = recipes.stream().sorted(Comparator.comparing(r -> r.getId().toString())).toList();
        selected = this.recipes.getFirst();
    }

    public static List<RecipeInfo<CraftingRecipe>> group(List<RecipeInfo<CraftingRecipe>> recipes) {
        List<List<RecipeInfo<CraftingRecipe>>> groups = new ArrayList<>();
        for (RecipeInfo<CraftingRecipe> recipe : recipes) {
            List<RecipeInfo<CraftingRecipe>> group = recipe.isOverride() ? null : groups.stream().filter(g -> !g.getFirst().isOverride() && ItemStack.isSameItemSameComponents(g.getFirst().getResultItem(), recipe.getResultItem())).findFirst().orElse(null);
            if (group == null) {
                group = new ArrayList<>();
                groups.add(group);
            }
            if (group.stream().noneMatch(r -> r.getId().equals(recipe.getId()))) group.add(recipe);
        }
        return groups.stream().map(g -> g.size() == 1 ? g.getFirst() : new CraftingRecipeAlternatives(g)).toList();
    }

    public boolean update(Predicate<RecipeInfo<CraftingRecipe>> canCraft, long time) {
        if (canCraft.test(selected)) return false;
        RecipeInfo<CraftingRecipe> next = recipes.stream().filter(canCraft).findFirst().orElse(null);
        if (next != null) previewLocked = false;
        else if (!previewLocked) next = recipes.get((int) (time / 800 % recipes.size()));
        if (next == null || next == selected) return false;
        selected = next;
        return true;
    }

    private List<RecipeInfo<CraftingRecipe>> availableRecipes(Predicate<RecipeInfo<CraftingRecipe>> canCraft) {
        List<RecipeInfo<CraftingRecipe>> available = recipes.stream().filter(canCraft).toList();
        return available.isEmpty() ? recipes : available;
    }

    public boolean canCycle(Predicate<RecipeInfo<CraftingRecipe>> canCraft) {
        return availableRecipes(canCraft).size() > 1;
    }

    public boolean cycle(Predicate<RecipeInfo<CraftingRecipe>> canCraft) {
        List<RecipeInfo<CraftingRecipe>> available = availableRecipes(canCraft);
        if (available.size() < 2) return false;
        selected = available.get((available.indexOf(selected) + 1) % available.size());
        previewLocked = true;
        return true;
    }

    @Override
    public CraftingRecipe get() {
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
