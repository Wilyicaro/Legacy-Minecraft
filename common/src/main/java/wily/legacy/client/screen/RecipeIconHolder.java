package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import wily.legacy.Legacy4J;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class RecipeIconHolder<R extends Recipe<?>> extends LegacyIconHolder  {
    protected int selectionOffset = 0;
    boolean isHoveredTop = false;
    boolean isHoveredBottom = false;
    protected List<R> focusedRecipes;
    protected final Minecraft minecraft = Minecraft.getInstance();

    public RecipeIconHolder(int x, int y){
        super(x, y,27,27);
        allowItemDecorations = false;
    }
    @Override
    public void render(GuiGraphics graphics, int i, int j, float f) {
        isHoveredTop = isFocused() && getFocusedRecipes().size() > 2 && isMouseOver(i,j,-1);
        isHoveredBottom = isFocused() && getFocusedRecipes().size() >= 2 && isMouseOver(i,j,1);
        itemIcon = isValidIndex() ? getFocusedRecipes().get(0).getResultItem(minecraft.level.registryAccess()) : ItemStack.EMPTY;
        super.render(graphics, i, j, f);
    }
    @Override
    public void renderItem(GuiGraphics graphics, int i, int j, float f) {
        if (!isValidIndex()) return;
        ScreenUtil.secureTranslucentRender(graphics, !canCraft(getFocusedRecipes().get(0)),0.5f, (u)->super.renderItem(graphics,i,j,f));
    }

    protected abstract boolean canCraft(R rcp);
    public boolean canCraft(){
        return canCraft(getFocusedRecipe());
    }

    List<R> getFocusedRecipes(){
        if (!isFocused() || !isValidIndex() || !canScroll()) focusedRecipes = null;
        else if (focusedRecipes == null) focusedRecipes = new ArrayList<>(getRecipes());
        return focusedRecipes == null ? getRecipes() : focusedRecipes;
    }
    protected abstract List<R> getRecipes();
    @Override
    public void setFocused(boolean bl) {
        if (bl){
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
        if (isHoveredTop) renderTooltip(minecraft,graphics, getFocusedRecipes().get(getFocusedRecipes().size() - 1).getResultItem(minecraft.level.registryAccess()),i,j);
        if (isHoveredBottom) renderTooltip(minecraft,graphics, getFocusedRecipes().get(1).getResultItem(minecraft.level.registryAccess()),i,j);
    }
    protected R getFocusedRecipe(){
        if (selectionOffset > 0 && getFocusedRecipes().size() < 2 || selectionOffset < 0 && getFocusedRecipes().size() <= 2) selectionOffset = 0;
        return isValidIndex() ? getFocusedRecipes().get(getSelectionIndex()) : null;
    }
    protected ItemStack getFocusedResult(){
        return getFocusedRecipe() == null ? ItemStack.EMPTY : getFocusedRecipe().getResultItem(minecraft.level.registryAccess()) ;
    }
    public void updateRecipeDisplay(){
        updateRecipeDisplay(getFocusedRecipe());
    }
    protected abstract void toggleCraftableRecipes();

    public boolean controlCyclicNavigation(int i, int index, List<RecipeIconHolder<R>> craftingButtons, Stocker.Sizeable craftingOffset, LegacyScrollRenderer renderer, Screen screen){
        if ((i == 263 && index == 0) || (i == 262 && index == craftingButtons.size() - 1)){
            int oldOffset = craftingOffset.get();
            craftingOffset.add(i == 263 ? -1 : 1,true);
            if ((oldOffset == craftingOffset.max && i == 262) || (oldOffset == 0 && i == 263)) screen.setFocused(craftingButtons.get(i == 263 ? craftingButtons.size() - 1 : 0));
            else {
                renderer.updateScroll(i == 263 ? ScreenDirection.LEFT : ScreenDirection.RIGHT);
                focusedRecipes = null;
            }
            ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(), 1.0f);
            return true;
        }
        return false;
    }
    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_O) {
            focusedRecipes = null;
            selectionOffset = 0;
            toggleCraftableRecipes();
            updateRecipeDisplay();
            return true;
        }
        int oldSelection = selectionOffset;
        if ((i == 265 || i == 264) && isValidIndex()) {
            if (i == InputConstants.KEY_UP && (getRecipes().size() > 2 || selectionOffset == 1))
                selectionOffset = Math.max(selectionOffset - 1, -1);
            if (i == InputConstants.KEY_DOWN && getRecipes().size() >= 2)
                selectionOffset = Math.min(selectionOffset + 1, 1);
            if (oldSelection != selectionOffset || canScroll()) {
                ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(), 1.0f);
                if (oldSelection == selectionOffset && selectionOffset != 0)
                    Collections.rotate(getFocusedRecipes(), -selectionOffset);
                updateRecipeDisplay(getFocusedRecipe());
                return true;
            }
        }
        return super.keyPressed(i, j, k);
    }

    protected abstract void updateRecipeDisplay(R rcp);
    public static ItemStack getActualItem(Ingredient ingredient){
        return ingredient.isEmpty() || ingredient.getItems().length == 0 ? ItemStack.EMPTY : ingredient.getItems()[(int) ((Util.getMillis() / 800)% ingredient.getItems().length)];
    }
    @Override
    public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
        if (isValidIndex()) {
            graphics.pose().pushPose();
            graphics.pose().translate(getXCorner() - 4.5f, getYCorner(), 0f);
            applyOffset(graphics);
            RenderSystem.disableDepthTest();
            if (getFocusedRecipes().size() == 2) {
                graphics.blitSprite(LegacySprites.CRAFTING_2_SLOTS_SELECTION, 0, -12, 36, 78);
            }else if (getFocusedRecipes().size() > 2)
                graphics.blitSprite(LegacySprites.CRAFTING_SELECTION, 0, -39, 36, 105);
            graphics.pose().popPose();
            if (getFocusedRecipes().size() >= 2){
                ScreenUtil.secureTranslucentRender(graphics, !canCraft(getFocusedRecipes().get(1)), 0.5f, (u)-> renderItem(graphics, getFocusedRecipes().get(1).getResultItem(minecraft.level.registryAccess()),getX(),getY() + 27,false));
                if (getFocusedRecipes().size() >= 3) ScreenUtil.secureTranslucentRender(graphics, !canCraft(getFocusedRecipes().get(getFocusedRecipes().size() - 1)), 0.5f, (u)-> renderItem(graphics, getFocusedRecipes().get(getFocusedRecipes().size() - 1).getResultItem(minecraft.level.registryAccess()),getX(),getY() - 27,false));
            }
            RenderSystem.enableDepthTest();
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0,selectionOffset * 27,0);
        super.renderSelection(graphics,i,j,f);
        graphics.pose().popPose();
    }
    protected boolean canScroll(){
        return getRecipes().size() >= 3;
    }
    protected boolean isValidIndex() {
        return !getRecipes().isEmpty();
    }
    protected int getSelectionIndex(){
        return selectionOffset == -1 ? getFocusedRecipes().size() - 1 : selectionOffset == 1 ? 1 : 0;
    }
    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (isFocused() && canScroll()){
            Collections.rotate(getFocusedRecipes(),(int)Math.signum(g));
            updateRecipeDisplay();
            return true;
        }
        return false;
    }
    protected boolean isMouseOver(double d, double e, int selection){
        return ScreenUtil.isMouseOver(d,e,getXCorner(),getYCorner() + selection * 27,getWidth(),getHeight());
    }

    @Override
    public boolean isMouseOver(double d, double e) {
        return isHovered || isHoveredTop || isHoveredBottom;
    }

    @Override
    public void onClick(double d, double e) {
        int oldSelection = selectionOffset;
        selectionOffset = isHoveredTop ? -1 : isHoveredBottom ? 1 : 0;
        if (oldSelection != selectionOffset) updateRecipeDisplay();
        else super.onClick(d, e);
    }

    @Override
    public void playClickSound() {
        if (!isFocused()) super.playClickSound();
    }
    public void craft() {
        ScreenUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP,1.0f);
        Legacy4J.NETWORK.sendToServer(new ServerInventoryCraftPacket(getFocusedRecipe(), Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed));
    }

    @Override
    public void onPress(){
        if (isFocused() && isValidIndex()){
            if (canCraft(getFocusedRecipe())){
                craft();
                updateRecipeDisplay(getFocusedRecipe());
            }else ScreenUtil.playSimpleUISound(LegacySoundEvents.CRAFT_FAIL.get(),1.0f);
        }
    }
}
