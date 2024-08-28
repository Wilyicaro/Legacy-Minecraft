package wily.legacy.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
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
    public void render(PoseStack poseStack, Minecraft minecraft, int leftPos, int topPos, boolean bigResultSlot, float f) {
        if (!Screen.hasControlDown()) {
            this.time += f;
        }
        for (int k = 0; k < this.ingredients.size(); ++k) {
            GhostIngredient ghostIngredient = this.ingredients.get(k);
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(ingredientSlots.get(k));
            poseStack.pose().pushPose();
            poseStack.pose().translate(ghostIngredient.getX() + leftPos, ghostIngredient.getY() + topPos, 0);
            holder.applyOffset(poseStack);
            poseStack.pose().scale(holder.getSelectableWidth() / 16f,holder.getSelectableHeight() / 16f,holder.getSelectableHeight() / 16f);

            poseStack.fill(0, 0, 16, 16, 0x30FF0000);
            ItemStack itemStack = ghostIngredient.getItem();
            poseStack.renderFakeItem(itemStack, 0, 0);
            poseStack.fill(RenderType.guiGhostRecipeOverlay(), 0, 0, 16,16, 0x30FFFFFF);
            if (k == 0)
                poseStack.renderItemDecorations(minecraft.font, itemStack, 0, 0);
            poseStack.pose().popPose();
        }
    }
}
