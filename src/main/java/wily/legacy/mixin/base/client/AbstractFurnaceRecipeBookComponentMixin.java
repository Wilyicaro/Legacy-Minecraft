//? if <1.21.2 {
/*package wily.legacy.mixin.base;

import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
//? if >1.20.1 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.screen.DisplayRecipe;

import java.util.List;

@Mixin(AbstractFurnaceRecipeBookComponent.class)
public class AbstractFurnaceRecipeBookComponentMixin extends RecipeBookComponent{

    @Redirect(method = "setupGhostRecipe", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/GhostRecipe;addIngredient(Lnet/minecraft/world/item/crafting/Ingredient;II)V", ordinal = 0))
    public void setupGhostRecipeResultSlot(GhostRecipe instance, Ingredient ingredient, int i, int j, /^? if >1.20.1 {^/RecipeHolder<?>/^?} else {^//^Recipe<?>^//^?}^/ recipe, List<Slot> list) {
        ((DisplayRecipe)ghostRecipe).addIngredient(ingredient, list.get(2));
    }
    @Redirect(method = "setupGhostRecipe", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/GhostRecipe;addIngredient(Lnet/minecraft/world/item/crafting/Ingredient;II)V", ordinal = 1))
    public void setupGhostRecipeFuelSlot(GhostRecipe instance, Ingredient ingredient, int i, int j, /^? if >1.20.1 {^/RecipeHolder<?>/^?} else {^//^Recipe<?>^//^?}^/ recipe, List<Slot> list) {
        ((DisplayRecipe)ghostRecipe).addIngredient(ingredient, list.get(1));
    }
    @Redirect(method = "setupGhostRecipe", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/GhostRecipe;addIngredient(Lnet/minecraft/world/item/crafting/Ingredient;II)V", ordinal = 2))
    public void setupGhostRecipeInputSlot(GhostRecipe instance, Ingredient ingredient, int i, int j, /^? if >1.20.1 {^/RecipeHolder<?>/^?} else {^//^Recipe<?>^//^?}^/ recipe, List<Slot> list) {
        NonNullList<Ingredient> nonNullList = recipe/^? if >1.20.1 {^/.value()/^?}^/.getIngredients();
        ((DisplayRecipe)ghostRecipe).addIngredient(ingredient, list.get(nonNullList.indexOf(ingredient)));
    }

}
*///?}
