package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.screen.DisplayRecipe;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.util.ScreenUtil;

import java.util.Iterator;
import java.util.List;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {
    @Mutable
    @Shadow @Final protected GhostRecipe ghostRecipe;

    @Shadow protected RecipeBookMenu<?> menu;
    @Shadow protected Minecraft minecraft;
    @Inject(method = "init", at = @At(value = "RETURN"))
    private void init(int i, int j, Minecraft minecraft, boolean bl, RecipeBookMenu<?> recipeBookMenu, CallbackInfo ci){
        ghostRecipe = new DisplayRecipe();
    }
    @Redirect(method = "setupGhostRecipe", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/GhostRecipe;addIngredient(Lnet/minecraft/world/item/crafting/Ingredient;II)V"))
    public void setupGhostRecipe(GhostRecipe instance, Ingredient ingredient, int i, int j,RecipeHolder<?> recipeHolder, List<Slot> list) {
        ((DisplayRecipe)ghostRecipe).addIngredient(ingredient, list.get(0));
    }

    @Redirect(method = "addItemToSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/GhostRecipe;addIngredient(Lnet/minecraft/world/item/crafting/Ingredient;II)V"))
    public void addItemToSlot(GhostRecipe instance, Ingredient ingredient, int i, int j,Iterator<Ingredient> iterator, int index) {
        ((DisplayRecipe)ghostRecipe).addIngredient(ingredient, menu.slots.get(index));
    }
    @Inject(method = "renderGhostRecipeTooltip", at = @At("HEAD"), cancellable = true)
    private void renderGhostRecipeTooltip(GuiGraphics guiGraphics, int leftPos, int topPos, int k, int l, CallbackInfo info) {
        info.cancel();
        ItemStack itemStack = null;
        for (int m = 0; m < this.ghostRecipe.size(); ++m) {
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(((DisplayRecipe) ghostRecipe).ingredientSlots.get(m));
            int n = holder.getX() + leftPos;
            int o = holder.getY() + topPos;
            if (k < n || l < o || k >= n + holder.getSelectableWidth() || l >= o + holder.getSelectableHeight()) continue;
            itemStack = ghostRecipe.get(m).getItem();
        }
        if (itemStack != null && minecraft.screen != null) {
            guiGraphics.renderComponentTooltip(this.minecraft.font, Screen.getTooltipFromItem(this.minecraft, itemStack), k, l);
        }
    }
}
