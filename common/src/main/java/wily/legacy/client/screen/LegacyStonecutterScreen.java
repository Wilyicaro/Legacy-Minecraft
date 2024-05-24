package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.Legacy4J;
import wily.legacy.client.StoneCuttingGroupManager;
import wily.legacy.client.Offset;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.PagedList;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static wily.legacy.util.LegacySprites.ARROW;
import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.ControlTooltip.CONTROL_ACTION_CACHE;
import static wily.legacy.client.screen.LegacyCraftingScreen.CRAFTING_OFFSET;


public class LegacyStonecutterScreen extends AbstractContainerScreen<LegacyCraftingMenu> {
    public static final Offset DISPLAY_OFFSET = new Offset(0.5,0,0);
    protected List<RecipeIconHolder<StonecutterRecipe>>  craftingButtons = new ArrayList<>();;
    protected List<List<StonecutterRecipe>> recipesByGroup = new ArrayList<>();
    protected List<List<StonecutterRecipe>> filteredRecipesByGroup = Collections.emptyList();
    protected Stocker.Sizeable craftingButtonsOffset = new Stocker.Sizeable(0);
    protected LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected List<Ingredient> ingredientSlot = List.of(Ingredient.EMPTY);
    protected int selectedCraftingButton = 0;
    private boolean onlyCraftableRecipes;


    private ContainerListener listener;
    @Override
    public boolean mouseClicked(double d, double e, int i) {
        return super.mouseClicked(d, e, i);
    }

    public LegacyStonecutterScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().tooltips.add(0,create(()->getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_RETURN,true) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(true),()->getFocused() instanceof LegacyIconHolder h && !h.isWarning()? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.create") : null));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_O,true) : ControllerBinding.UP_BUTTON.bindingState.getIcon(true), ()-> CONTROL_ACTION_CACHE.getUnchecked(onlyCraftableRecipes ? "legacy.action.all_recipes" : "legacy.action.show_craftable_recipes"));
        RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
        StoneCuttingGroupManager.list.values().forEach(l->{
            List<StonecutterRecipe> group = new ArrayList<>();
            l.forEach(v->v.addRecipes(RecipeType.STONECUTTING,manager,group,r-> recipesByGroup.stream().noneMatch(lr->lr.contains(r))));
            if (!group.isEmpty()) recipesByGroup.add(group);
        });
        manager.getAllRecipesFor(RecipeType.STONECUTTING).stream().filter(h->recipesByGroup.stream().noneMatch(l->l.contains(h.value()))).forEach(h->recipesByGroup.add(List.of(h.value())));
        addCraftingButtons();
        onlyCraftableRecipes = true;
        listener = new ContainerListener() {
            public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
                if(onlyCraftableRecipes){
                    filteredRecipesByGroup = recipesByGroup.stream().map(l->l.stream().filter(r->ServerInventoryCraftPacket.canCraft(r.getIngredients(),inventory, abstractContainerMenu.getCarried())).toList()).filter(l-> !l.isEmpty()).toList();
                    craftingButtons.get(selectedCraftingButton).updateRecipeDisplay();
                }
            }
            public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

            }
        };
        listener.slotChanged(menu,0,ItemStack.EMPTY);
        onlyCraftableRecipes = false;
    }
    @Override
    public void init() {
        imageWidth = 348;
        imageHeight = 215;
        super.init();
        menu.addSlotListener(listener);
        if (selectedCraftingButton < craftingButtons.size()) setFocused(craftingButtons.get(selectedCraftingButton));
        craftingButtons.forEach(b->{
            b.setPos(leftPos + 13 + craftingButtons.indexOf(b) * 27,topPos + 38);
            addRenderableWidget(b);
        });
        craftingButtonsOffset.max = Math.max(0,recipesByGroup.size() - 12);
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, title,(imageWidth - font.width(title)) / 2, 17, 0x383838, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 109, 0x383838, false);
    }
    @Override
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos,imageWidth,imageHeight,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 9,topPos + 103,163,105,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 176,topPos + 103,163,105,2f);
        guiGraphics.blitSprite(ARROW,leftPos + 79,topPos + 158,22,15);
        if (craftingButtonsOffset.get() > 0) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
        if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + 337, topPos + 45);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBackground(guiGraphics, i, j, f);
        super.render(guiGraphics, i, j, f);
        craftingButtons.get(selectedCraftingButton).renderSelection(guiGraphics,i,j,f);
        craftingButtons.forEach(h-> h.renderTooltip(minecraft,guiGraphics,i,j));
        renderTooltip(guiGraphics, i, j);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public void removed() {
        super.removed();
        menu.removeSlotListener(listener);
    }

    @Unique
    protected void addCraftingButtons(){
        for (int i = 0; i < 12; i++) {
            int index = i;

            RecipeIconHolder<StonecutterRecipe> h;
            craftingButtons.add(h = new RecipeIconHolder<>(leftPos + 13 + i * 27, topPos + 38) {
                private boolean warningInputSlot;

                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }

                protected boolean canCraft(StonecutterRecipe rcp) {
                    if (rcp == null || onlyCraftableRecipes) return true;
                    boolean focusedRcp = isFocused() && getFocusedRecipe() == rcp;
                    List<Ingredient> ings = focusedRcp ? ingredientSlot : rcp.getIngredients();
                    boolean canCraft = true;
                    for (int i1 = 0; i1 < ings.size(); i1++) {
                        Ingredient ing = ings.get(i1);
                        if (ing.isEmpty()) continue;
                        int itemCount = minecraft.player.getInventory().items.stream().filter(ing).mapToInt(ItemStack::getCount).sum() + (menu.getCarried().isEmpty() || !ing.test(menu.getCarried()) ? 0 : menu.getCarried().getCount());
                        long ingCount = ings.stream().filter(i -> i == ing).count();
                        if (itemCount >= ingCount || PagedList.occurrenceOf(ings, ing, i1) < itemCount) {
                            if (focusedRcp && ingredientSlot.contains(ing)) warningInputSlot = false;
                        } else {
                            canCraft = false;
                            if (!focusedRcp || !ingredientSlot.contains(ing)) break;
                            else warningInputSlot = true;
                        }
                    }
                    return canCraft;
                }

                public List<StonecutterRecipe> getFocusedRecipes() {
                    if (!isFocused() || !isValidIndex() || !canScroll()) focusedRecipes = null;
                    else if (focusedRecipes == null) focusedRecipes = new ArrayList<>(getRecipes());
                    return focusedRecipes == null ? getRecipes() : focusedRecipes;
                }

                protected List<StonecutterRecipe> getRecipes() {
                    List<List<StonecutterRecipe>> list = onlyCraftableRecipes ? filteredRecipesByGroup : recipesByGroup;
                    return list.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : list.get(craftingButtonsOffset.get() + index);
                }

                @Override
                protected void updateRecipeDisplay(StonecutterRecipe rcp) {
                    ingredientSlot = rcp == null ? List.of(Ingredient.EMPTY) : rcp.getIngredients();
                }

                @Override
                protected void toggleCraftableRecipes() {
                    listener.slotChanged(menu, 0, ItemStack.EMPTY);
                    onlyCraftableRecipes = !onlyCraftableRecipes;
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if (controlCyclicNavigation(i, index, craftingButtons, craftingButtonsOffset, scrollRenderer, LegacyStonecutterScreen.this))
                        return true;
                    return super.keyPressed(i, j, k);
                }

                @Override
                public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, int i, int j) {
                    super.renderTooltip(minecraft, graphics, i, j);
                    if (isFocused()) {
                        if (!ingredientSlot.isEmpty() && ScreenUtil.isMouseOver(i,j, leftPos + 38, topPos + 149, 36,36))
                            renderTooltip(minecraft, graphics, getActualItem(ingredientSlot.get(0)), i, j);
                        if (ScreenUtil.isMouseOver(i,j, leftPos + 110, topPos + 149, 36,36))
                            renderTooltip(minecraft, graphics, getFocusedResult(), i, j);
                    }
                }

                @Override
                public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
                    ScreenUtil.iconHolderRenderer.itemHolder(leftPos+38,topPos+149,36,36, getActualItem(ingredientSlot.get(0)), !onlyCraftableRecipes && !ingredientSlot.get(0).isEmpty() && warningInputSlot, DISPLAY_OFFSET).render(graphics, i, j, f);
                    ScreenUtil.iconHolderRenderer.itemHolder(leftPos+110,topPos+149,36,36,getFocusedResult(), !canCraft(getFocusedRecipe()), DISPLAY_OFFSET).render(graphics, i, j, f);

                    if (getFocusedRecipe() != null) {
                        Component resultName = getFocusedResult().getHoverName();
                        ScreenUtil.renderScrollingString(graphics, font, resultName, leftPos + 11 + Math.max(163 - font.width(resultName), 0) / 2, topPos + 114, leftPos + 170, topPos + 125, 0x383838, false);
                    }
                    super.renderSelection(graphics, i, j, f);
                }

                @Override
                public boolean mouseScrolled(double d, double e, double f, double g) {
                    if (isFocused() && canScroll()) {
                        Collections.rotate(getFocusedRecipes(), (int) Math.signum(g));
                        return true;
                    }
                    return false;
                }
                public void craft() {
                    Legacy4J.NETWORK.sendToServer(new ServerInventoryCraftPacket(getFocusedRecipe(), Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed));
                }
            });
            h.offset = CRAFTING_OFFSET;
            h.allowItemDecorations = false;
        }
    }
}
