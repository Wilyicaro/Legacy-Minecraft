package wily.legacy.mixin.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
//? if >1.20.1 {
import net.minecraft.world.item.crafting.RecipeHolder;
 //?}
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.21.2 {
/*import wily.legacy.client.screen.DisplayRecipe;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
*///?}
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.ScreenUtil;

import java.util.Iterator;
import java.util.List;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {
    //? if <1.21.2 {
    /*@Mutable
    @Shadow @Final protected GhostRecipe ghostRecipe;
    *///?}


    @Shadow protected Minecraft minecraft;
    @Shadow protected /*? if <1.20.5 {*/ /*RecipeBookMenu<?> *//*?} else if <1.21.2 {*/ /*RecipeBookMenu<?, ?> *//*?} else {*/ @Final RecipeBookMenu/*?}*/ menu;

    @Shadow private int xOffset;

    @Shadow private boolean widthTooNarrow;

    @Redirect(method = "initVisuals", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;xOffset:I", opcode = Opcodes.PUTFIELD))
    private void initVisuals(RecipeBookComponent instance, int value){
        xOffset = this.widthTooNarrow ? 0 : minecraft.screen instanceof LegacyMenuAccess<?> a ? a.getMenuRectangle().width() / 2 - 2: 86;
    }

    //? if <1.21.2 {
    /*@Inject(method = "init", at = @At(value = "RETURN"))
    private void init(int i, int j, Minecraft minecraft, boolean bl, /^? if <1.20.5 {^/ /^RecipeBookMenu<?> ^//^?} else {^/ RecipeBookMenu<?, ?> /^?}^/ recipeBookMenu, CallbackInfo ci){
        ghostRecipe = new DisplayRecipe();
    }
    @Redirect(method = "setupGhostRecipe", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/GhostRecipe;addIngredient(Lnet/minecraft/world/item/crafting/Ingredient;II)V"))
    public void setupGhostRecipe(GhostRecipe instance, Ingredient ingredient, int i, int j, /^? if >1.20.1 {^/RecipeHolder<?>/^?} else {^//^Recipe<?>^//^?}^/ recipe, List<Slot> list) {
        ((DisplayRecipe)ghostRecipe).addIngredient(ingredient, list.get(0));
    }
    //? if <1.20.5 {
    /^@Redirect(method = "addItemToSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/GhostRecipe;addIngredient(Lnet/minecraft/world/item/crafting/Ingredient;II)V"))
    public void addItemToSlot(GhostRecipe instance, Ingredient ingredient, int i, int j, Iterator<Ingredient> iterator, int index) {
        ((DisplayRecipe)ghostRecipe).addIngredient(ingredient, menu.slots.get(index));
    }
    ^///?} else {
    @Redirect(method = "addItemToSlot(Lnet/minecraft/world/item/crafting/Ingredient;IIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/GhostRecipe;addIngredient(Lnet/minecraft/world/item/crafting/Ingredient;II)V"))
    public void addItemToSlot(GhostRecipe instance, Ingredient ingredient, int i, int j,Ingredient ingredient1, int i1) {
        ((DisplayRecipe)ghostRecipe).addIngredient(ingredient, menu.slots.get(i1));
    }
    //?}
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
    *///?}
}
