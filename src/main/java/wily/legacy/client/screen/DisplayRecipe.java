//? if <1.21.2 {
/*package wily.legacy.client.screen;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import wily.legacy.util.ScreenUtil;

import java.util.List;

public class DisplayRecipe extends GhostRecipe {
    public final List<Slot> ingredientSlots = Lists.newArrayList();

    @Override
    public void clear() {
        super.clear();
        ingredientSlots.clear();
    }
    public void addIngredient(Ingredient ingredient, Slot slot) {
        addIngredient(ingredient, slot.x, slot.y);
        ingredientSlots.add(slot);
    }
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int leftPos, int topPos, boolean bigResultSlot, float f) {
        if (!Screen.hasControlDown()) {
            this.time += f;
        }
        for (int k = 0; k < this.ingredients.size(); ++k) {
            GhostIngredient ghostIngredient = this.ingredients.get(k);
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(ingredientSlots.get(k));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(ghostIngredient.getX() + leftPos, ghostIngredient.getY() + topPos, 0);
            holder.applyOffset(guiGraphics);
            guiGraphics.pose().scale(holder.getSelectableWidth() / 16f,holder.getSelectableHeight() / 16f,holder.getSelectableHeight() / 16f);

            guiGraphics.fill(0, 0, 16, 16, 0x30FF0000);
            ItemStack itemStack = ghostIngredient.getItem();
            guiGraphics.renderFakeItem(itemStack, 0, 0);
            guiGraphics.fill(RenderType.guiGhostRecipeOverlay(), 0, 0, 16,16, 0x30FFFFFF);
            if (k == 0)
                guiGraphics.renderItemDecorations(minecraft.font, itemStack, 0, 0);
            guiGraphics.pose().popPose();
        }
    }
}
*///?}
