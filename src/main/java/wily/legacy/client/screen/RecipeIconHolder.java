package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.FactoryIngredient;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.client.*;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.network.ServerMenuCraftPayload;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class RecipeIconHolder<R> extends LegacyIconHolder implements ControlTooltip.ActionHolder {
    public static final Component NOT_ENOUGH_INGREDIENTS = Component.translatable("legacy.hint.not_enough_ingredients");
    protected final Minecraft minecraft = Minecraft.getInstance();
    public boolean compactMode = false;
    protected int selectionOffset = 0;
    protected List<RecipeInfo<R>> focusedRecipes;
    protected boolean allowCraftableRecipesToggle = true;
    boolean isHoveredTop = false;
    boolean isHoveredBottom = false;

    public RecipeIconHolder(int x, int y) {
        super(x, y, 27, 27);
        allowItemDecorations = false;
    }

    public static ItemStack getActualItem(Optional<Ingredient> ingredient) {
        return ingredient.isEmpty() || FactoryIngredient.of(ingredient.get()).getStacks().length == 0 ? ItemStack.EMPTY : FactoryIngredient.of(ingredient.get()).getStacks()[(int) ((Util.getMillis() / 800) % FactoryIngredient.of(ingredient.get()).getStacks().length)];
    }

    @Override
    public void render(GuiGraphics graphics, int i, int j, float f) {
        compactMode = LegacyOptions.getUIMode().isSD();
        isHoveredTop = isFocused() && !compactMode && getFocusedRecipes().size() > 2 && isMouseOver(i, j, -1);
        isHoveredBottom = isFocused() && !compactMode && getFocusedRecipes().size() >= 2 && isMouseOver(i, j, 1);
        itemIcon = isValidIndex() ? getFocusedRecipes().get(0).getResultItem() : ItemStack.EMPTY;
        super.render(graphics, i, j, f);
    }

    @Override
    public void renderItem(GuiGraphics graphics, int i, int j, float f) {
        if (!isValidIndex()) return;
        LegacyGuiItemRenderer.secureTranslucentRender(!canCraft(getFocusedRecipes().get(0)), 0.5f, (u) -> super.renderItem(graphics, i, j, f));
    }

    protected abstract boolean canCraft(RecipeInfo<R> rcp);

    public boolean canCraft() {
        return canCraft(getFocusedRecipe());
    }

    protected abstract List<RecipeInfo<R>> getRecipes();

    public abstract LegacyScrollRenderer getScrollRenderer();

    List<RecipeInfo<R>> getFocusedRecipes() {
        if (!isFocused() || !isValidIndex() || !canScroll()) focusedRecipes = null;
        else if (focusedRecipes == null) focusedRecipes = new ArrayList<>(getRecipes());
        return focusedRecipes == null ? getRecipes() : focusedRecipes;
    }

    public void invalidateFocused() {
        focusedRecipes = null;
    }

    @Override
    public void setFocused(boolean bl) {
        if (bl) {
            selectionOffset = 0;
            updateRecipeDisplay();
        }
        super.setFocused(bl);
    }

    @Override
    public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, int i, int j) {
        super.renderTooltip(minecraft, graphics, i, j);
        if (!isFocused()) return;
        if (getFocusedRecipes().size() <= 1) return;
        if (isHoveredTop)
            renderTooltip(minecraft, graphics, getFocusedRecipes().get(getFocusedRecipes().size() - 1).getResultItem(), i, j);
        if (isHoveredBottom) renderTooltip(minecraft, graphics, getFocusedRecipes().get(1).getResultItem(), i, j);
    }

    protected RecipeInfo<R> getFocusedRecipe() {
        if (selectionOffset > 0 && getFocusedRecipes().size() < 2 || selectionOffset < 0 && getFocusedRecipes().size() <= 2)
            selectionOffset = 0;
        return isValidIndex() ? getFocusedRecipes().get(getSelectionIndex()) : null;
    }

    protected ItemStack getFocusedResult() {
        return getFocusedRecipe() == null ? ItemStack.EMPTY : getFocusedRecipe().getResultItem();
    }

    public void updateRecipeDisplay() {
        updateRecipeDisplay(getFocusedRecipe());
    }

    protected abstract void toggleCraftableRecipes(InputWithModifiers input);

    public boolean controlCyclicNavigation(int i, int index, List<RecipeIconHolder<R>> craftingButtons, Stocker.Sizeable craftingOffset, LegacyScrollRenderer renderer, Screen screen) {
        if ((i == 263 && index == 0) || (i == 262 && index == craftingButtons.size() - 1)) {
            int oldOffset = craftingOffset.get();
            craftingOffset.add(i == 263 ? -1 : 1, true);
            if ((oldOffset == craftingOffset.max && i == 262) || (oldOffset == 0 && i == 263))
                screen.setFocused(craftingButtons.get(i == 263 ? craftingButtons.size() - 1 : 0));
            else {
                renderer.updateScroll(i == 263 ? ScreenDirection.LEFT : ScreenDirection.RIGHT);
                focusedRecipes = null;
            }
            LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == InputConstants.KEY_O && allowCraftableRecipesToggle) {
            focusedRecipes = null;
            selectionOffset = 0;
            toggleCraftableRecipes(keyEvent);
            updateRecipeDisplay();
            LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
            return true;
        }
        int oldSelection = selectionOffset;
        if ((keyEvent.isUp() || keyEvent.isDown()) && isValidIndex()) {
            if (!compactMode) {
                if (keyEvent.isUp() && (getRecipes().size() > 2 || selectionOffset == 1))
                    selectionOffset = Math.max(selectionOffset - 1, -1);
                if (keyEvent.isDown() && getRecipes().size() >= 2)
                    selectionOffset = Math.min(selectionOffset + 1, 1);
            }
            if (oldSelection != selectionOffset || canScroll()) {
                LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
                if (oldSelection == selectionOffset && (compactMode || selectionOffset != 0))
                    Collections.rotate(getFocusedRecipes(), keyEvent.isUp() ? 1 : -1);
                updateRecipeDisplay(getFocusedRecipe());
                return true;
            }
        }
        return super.keyPressed(keyEvent);
    }

    protected abstract void updateRecipeDisplay(RecipeInfo<R> rcp);

    @Override
    public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
        if (isValidIndex()) {
            if (compactMode) {
                if (canScroll()) {
                    renderScroll(graphics, getScrollRenderer());
                }
            } else {
                graphics.pose().pushMatrix();
                graphics.pose().translate(getXCorner() - 4.5f, getYCorner());
                applyOffset(graphics);
                if (getFocusedRecipes().size() == 2) {
                    FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.CRAFTING_2_SLOTS_SELECTION, 0, -12, 36, 78);
                } else if (getFocusedRecipes().size() > 2)
                    FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.CRAFTING_SELECTION, 0, -39, 36, 105);
                graphics.pose().popMatrix();
                if (getFocusedRecipes().size() >= 2) {
                    LegacyGuiItemRenderer.secureTranslucentRender(!canCraft(getFocusedRecipes().get(1)), 0.5f, (u) -> renderItem(graphics, getFocusedRecipes().get(1).getResultItem(), getX(), getY() + 27, false));
                    if (getFocusedRecipes().size() >= 3)
                        LegacyGuiItemRenderer.secureTranslucentRender(!canCraft(getFocusedRecipes().get(getFocusedRecipes().size() - 1)), 0.5f, (u) -> renderItem(graphics, getFocusedRecipes().get(getFocusedRecipes().size() - 1).getResultItem(), getX(), getY() - 27, false));
                }
            }
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(0, selectionOffset * 27);
        super.renderSelection(graphics, i, j, f);
        graphics.pose().popMatrix();
    }

    protected boolean canScroll() {
        return getRecipes().size() >= (compactMode ? 2 : 3);
    }

    protected boolean isValidIndex() {
        return !getRecipes().isEmpty();
    }

    protected int getSelectionIndex() {
        return selectionOffset == -1 ? getFocusedRecipes().size() - 1 : selectionOffset == 1 ? 1 : 0;
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (isFocused() && canScroll()) {
            Collections.rotate(getFocusedRecipes(), (int) Math.signum(g));
            updateRecipeDisplay();
            return true;
        }
        return false;
    }

    protected boolean isMouseOver(double d, double e, int selection) {
        return LegacyRenderUtil.isMouseOver(d, e, getXCorner(), getYCorner() + selection * 27, getWidth(), getHeight());
    }

    @Override
    public boolean isMouseOver(double d, double e) {
        return isHovered || isHoveredTop || isHoveredBottom;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean bl) {
        int oldSelection = selectionOffset;
        selectionOffset = isHoveredTop ? -1 : isHoveredBottom ? 1 : 0;
        if (oldSelection != selectionOffset) updateRecipeDisplay();
        else super.onClick(event, bl);
    }


    public void craft(InputWithModifiers input) {
        CommonNetwork.sendToServer(new ServerMenuCraftPayload(getFocusedRecipe(), input.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (isFocused() && isValidIndex()) {
            if (canCraft(getFocusedRecipe())) {
                craft(input);
                updateRecipeDisplay(getFocusedRecipe());
            } else {
                if (minecraft.player.containerMenu instanceof LegacyCraftingMenu m && !m.showedNotEnoughIngredientsHint && LegacyOptions.hints.get()) {
                    m.showedNotEnoughIngredientsHint = true;
                    LegacyTipManager.setActualTip(new LegacyTip(null, NOT_ENOUGH_INGREDIENTS));
                }
                LegacySoundUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(), 1.0f);
            }
        }
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return context.actionOfContext(KeyContext.class, c -> c.key() == InputConstants.KEY_RETURN && canCraft() && isValidIndex() && isFocused() ? LegacyComponents.CREATE : null);
    }
}
