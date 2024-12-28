package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Unique;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.RecipeInfo;
import wily.legacy.client.StoneCuttingGroupManager;
import wily.legacy.client.controller.Controller;
import wily.legacy.network.CommonRecipeManager;
import wily.legacy.util.*;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.RecipeMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static wily.legacy.util.LegacySprites.ARROW;
import static wily.legacy.client.screen.ControlTooltip.*;


public class LegacyStonecutterScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event,ControlTooltip.Event {
    public static final Vec3 DISPLAY_OFFSET = new Vec3(0.5,0,0);
    protected List<RecipeIconHolder<StonecutterRecipe>>  craftingButtons = new ArrayList<>();;
    protected List<List<RecipeInfo<StonecutterRecipe>>> recipesByGroup = new ArrayList<>();
    protected List<List<RecipeInfo<StonecutterRecipe>>> filteredRecipesByGroup = Collections.emptyList();
    protected Stocker.Sizeable craftingButtonsOffset = new Stocker.Sizeable(0);
    protected LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected List<Optional<Ingredient>> ingredientSlot = Collections.singletonList(Optional.empty());
    protected int selectedCraftingButton = 0;
    private boolean onlyCraftableRecipes;
    protected final List<ItemStack> compactInventoryList = new ArrayList<>();
    private final ContainerListener listener;
    protected final UIDefinition.Accessor accessor = UIDefinition.Accessor.of(this);

    public LegacyStonecutterScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        List<RecipeInfo<StonecutterRecipe>> allRecipes = CommonRecipeManager.byType(RecipeType.STONECUTTING).stream().map(h-> RecipeInfo.create(h./*? if >1.20.1 {*/id()/*?} else {*//*getId()*//*?}*/, h/*? if >1.20.1 {*/.value()/*?}*/, LegacyCraftingMenu.getRecipeOptionalIngredients(h/*? if >1.20.1 {*/.value()/*?}*/),h/*? if >1.20.1 {*/.value()/*?}*/.assemble(null,Minecraft.getInstance().level.registryAccess()))).toList();
        StoneCuttingGroupManager.listing.values().forEach(l->{
            List<RecipeInfo<StonecutterRecipe>> group = new ArrayList<>();
            l.forEach(v->v.addRecipes(allRecipes.stream().filter(h->recipesByGroup.stream().noneMatch(r->r.contains(h)))::iterator,group::add));
            if (!group.isEmpty()) recipesByGroup.add(group);
        });
        allRecipes.stream().filter(h->recipesByGroup.stream().noneMatch(r->r.contains(h))).forEach(h->recipesByGroup.add(Collections.singletonList(h)));
        addCraftingButtons();
        onlyCraftableRecipes = true;
        listener = new ContainerListener() {
            public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
                if(onlyCraftableRecipes){
                    filteredRecipesByGroup = recipesByGroup.stream().map(l->l.stream().filter(r-> RecipeMenu.canCraft(r.getOptionalIngredients(),inventory, abstractContainerMenu.getCarried())).toList()).filter(l-> !l.isEmpty()).toList();
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
    public void addControlTooltips(Renderer renderer) {
        ControlTooltip.setupDefaultButtons(renderer,this);
        Event.super.addControlTooltips(renderer);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()-> onlyCraftableRecipes ? LegacyComponents.ALL_RECIPES : LegacyComponents.SHOW_CRAFTABLE_RECIPES);
    }

    @Override
    public boolean disableCursorOnInit() {
        return true;
    }

    @Override
    public boolean onceClickBindings() {
        return false;
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
    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    //?} else {
    /*@Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }
    *///?}
    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, title,(imageWidth - font.width(title)) / 2, 17, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 109, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
    }
    @Override
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(accessor.getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class),leftPos,topPos,imageWidth,imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 9,topPos + 103,163,105);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 176,topPos + 103,163,105);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(ARROW,leftPos + 79,topPos + 158,22,15);
        if (craftingButtonsOffset.get() > 0) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
        if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + 337, topPos + 45);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        //? if >1.20.1 {
        renderBackground(guiGraphics, i, j, f);
         //?}
        super.render(guiGraphics, i, j, f);
        craftingButtons.get(selectedCraftingButton).renderSelection(guiGraphics,i,j,f);
        craftingButtons.forEach(h-> h.renderTooltip(minecraft,guiGraphics,i,j));
        renderTooltip(guiGraphics, i, j);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        //? if >=1.21.2 {
        if (this.getChildAt(d, e).filter((guiEventListener) -> guiEventListener.mouseScrolled(d, e, f, g)).isPresent()) return true;
        //?}
        if (super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g)) return true;
        int scroll = (int)Math.signum(g);
        if (((craftingButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && craftingButtonsOffset.max > 0)) && craftingButtonsOffset.add(scroll,false) != 0){
            repositionElements();
            return true;
        }
        return false;
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

                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }


                @Override
                protected boolean canCraft(RecipeInfo<StonecutterRecipe> rcp) {
                    if (rcp == null || onlyCraftableRecipes) return true;
                    compactInventoryList.clear();
                    RecipeMenu.handleCompactInventoryList(compactInventoryList,Minecraft.getInstance().player.getInventory(),menu.getCarried());
                    return LegacyCraftingScreen.canCraft(compactInventoryList, isFocused() && getFocusedRecipe() == rcp ? ingredientSlot : rcp.getOptionalIngredients(), null);
                }

                public List<RecipeInfo<StonecutterRecipe>> getFocusedRecipes() {
                    if (!isFocused() || !isValidIndex() || !canScroll()) focusedRecipes = null;
                    else if (focusedRecipes == null) focusedRecipes = new ArrayList<>(getRecipes());
                    return focusedRecipes == null ? getRecipes() : focusedRecipes;
                }

                protected List<RecipeInfo<StonecutterRecipe>> getRecipes() {
                    List<List<RecipeInfo<StonecutterRecipe>>> list = onlyCraftableRecipes ? filteredRecipesByGroup : recipesByGroup;
                    return list.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : list.get(craftingButtonsOffset.get() + index);
                }

                @Override
                protected void updateRecipeDisplay(RecipeInfo<StonecutterRecipe> rcp) {
                    ingredientSlot = rcp == null ? Collections.singletonList(Optional.empty()) : rcp.getOptionalIngredients();
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
                    boolean warning = !canCraft(getFocusedRecipe());
                    ScreenUtil.iconHolderRenderer.itemHolder(leftPos+38,topPos+149,36,36, getActualItem(ingredientSlot.get(0)), !onlyCraftableRecipes && !ingredientSlot.get(0).isEmpty() && warning, DISPLAY_OFFSET).render(graphics, i, j, f);
                    ScreenUtil.iconHolderRenderer.itemHolder(leftPos+110,topPos+149,36,36,getFocusedResult(), warning, DISPLAY_OFFSET).render(graphics, i, j, f);

                    if (getFocusedRecipe() != null) {
                        Component resultName = getFocusedRecipe().getName();
                        ScreenUtil.renderScrollingString(graphics, font, resultName, leftPos + 11 + Math.max(163 - font.width(resultName), 0) / 2, topPos + 114, leftPos + 170, topPos + 125, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    }
                    super.renderSelection(graphics, i, j, f);
                }

            });
            h.offset = LegacyCraftingMenu.DEFAULT_INVENTORY_OFFSET;
            h.allowItemDecorations = false;
        }
    }
}
