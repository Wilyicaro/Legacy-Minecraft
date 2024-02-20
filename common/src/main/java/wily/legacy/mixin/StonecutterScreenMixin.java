package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.LegacySprites;
import wily.legacy.client.Offset;
import wily.legacy.client.StoneCuttingGroupManager;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.LegacyScrollRenderer;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.PagedList;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static wily.legacy.client.LegacySprites.ARROW_SPRITE;

@Mixin(StonecutterScreen.class)
public class StonecutterScreenMixin extends AbstractContainerScreen<StonecutterMenu> {
    protected List<LegacyIconHolder>  craftingButtons = new ArrayList<>();;
    protected List<List<StonecutterRecipe>> recipesByGroup = new ArrayList<>();
    protected List<List<StonecutterRecipe>> filteredRecipesByGroup = Collections.emptyList();
    protected Stocker.Sizeable craftingButtonsOffset =  new Stocker.Sizeable(0);
    protected LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected List<Ingredient> ingredientSlot = Collections.emptyList();
    protected int selectedCraftingButton = 0;
    private boolean onlyCraftableRecipes;

    private int lastFocused = -1;

    private ContainerListener listener;
    @Override
    public boolean mouseClicked(double d, double e, int i) {
        return super.mouseClicked(d, e, i);
    }

    public StonecutterScreenMixin(StonecutterMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }
    public void repositionElements() {
        lastFocused = getFocused() instanceof LegacyIconHolder h ? craftingButtons.indexOf(h) : -1;
        super.repositionElements();
    }
    @Inject(method = "<init>",at = @At("RETURN"))
    private void init(StonecutterMenu stonecutterMenu, Inventory inventory, Component component, CallbackInfo ci) {
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
                if(onlyCraftableRecipes) filteredRecipesByGroup = recipesByGroup.stream().map(l->l.stream().filter(r->ServerInventoryCraftPacket.canCraft(r.getIngredients(),inventory)).toList()).filter(l-> !l.isEmpty()).toList();
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
        if (lastFocused >= 0 && lastFocused < craftingButtons.size()) setInitialFocus(craftingButtons.get(lastFocused));
        else setInitialFocus(craftingButtons.get(0));
        craftingButtons.forEach(b->{
            b.setPos(leftPos + 13 + craftingButtons.indexOf(b) * 27,topPos + 38);
            addRenderableWidget(b);
        });
        craftingButtonsOffset.max = Math.max(0,recipesByGroup.size() - 12);
    }
    public boolean hasAutoCrafting(){
        return menu.slots.isEmpty() || !menu.getSlot(0).hasItem();
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, title,(imageWidth - font.width(title)) / 2, 17, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 109, 4210752, false);
    }
    @Override
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos,imageWidth,imageHeight,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 9,topPos + 103,163,105,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 176,topPos + 103,163,105,2f);
        guiGraphics.blitSprite(ARROW_SPRITE,leftPos + 79,topPos + 158,22,15);
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

            LegacyIconHolder h;
            craftingButtons.add(h = new LegacyIconHolder(leftPos + 13 + i * 27,topPos + 38,27, 27) {
                private int selectionOffset = 0;
                boolean isHoveredTop = false;
                boolean isHoveredBottom = false;
                private List<StonecutterRecipe> focusedRecipes;
                private boolean warningInputSlot = false;
                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    isHoveredTop = isFocused() && getFocusedRecipes().size() > 2 && isMouseOver(i,j,-1);
                    isHoveredBottom = isFocused() && getFocusedRecipes().size() >= 2 && isMouseOver(i,j,1);
                    itemIcon = isValidIndex() ? getFocusedRecipes().get(0).getResultItem(minecraft.level.registryAccess()) : ItemStack.EMPTY;
                    super.render(graphics, i, j, f);
                }
                @Override
                public void renderItem(GuiGraphics graphics, int i, int j, float f) {
                    if (!isValidIndex()) return;
                    ScreenUtil.secureTranslucentRender(graphics, !canCraft(getFocusedRecipes().get(0)),0.5f, ()->super.renderItem(graphics, i, j, f));
                }

                private boolean canCraft(StonecutterRecipe rcp){
                    if (rcp == null || onlyCraftableRecipes) return true;
                    boolean focusedRcp = isFocused() && getFocusedRecipe() == rcp;
                    List<Ingredient> ings = focusedRcp ? ingredientSlot : rcp.getIngredients();
                    boolean canCraft = true;
                    for (int i1 = 0; i1 < ings.size(); i1++) {
                        Ingredient ing = ings.get(i1);
                        if (ing.isEmpty()) continue;
                        int itemCount = minecraft.player.getInventory().items.stream().filter(ing).mapToInt(ItemStack::getCount).sum();
                        long ingCount = ings.stream().filter(i-> i == ing).count();
                        if (itemCount >= ingCount || PagedList.occurrenceOf(ings,ing,i1) < itemCount) {
                            if (focusedRcp && ingredientSlot.contains(ing)) warningInputSlot = false;
                        }else {
                            canCraft = false;
                            if (!focusedRcp || !ingredientSlot.contains(ing)) break;
                            else warningInputSlot = true;
                        }
                    }
                    return canCraft;
                }
                private List<StonecutterRecipe> getFocusedRecipes(){
                    if (!isFocused() || !isValidIndex() || !canScroll()) focusedRecipes = null;
                    else if (focusedRecipes == null) focusedRecipes = new ArrayList<>(getRecipes());
                    return focusedRecipes == null ? getRecipes() : focusedRecipes;
                }
                private List<StonecutterRecipe> getRecipes(){
                    List<List<StonecutterRecipe>> list = onlyCraftableRecipes ? filteredRecipesByGroup : recipesByGroup;
                    return list.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : list.get(craftingButtonsOffset.get() + index);
                }
                private void updateResultSlot(){
                    if (!hasAutoCrafting()) {
                        menu.getRecipes().stream().filter(h -> h.value() == getFocusedRecipe()).findFirst().ifPresent(h -> {
                            if (menu.clickMenuButton(minecraft.player, menu.getRecipes().indexOf(h))) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.getRecipes().indexOf(h));
                        });
                    }
                }
                @Override
                public void setFocused(boolean bl) {
                    if (bl){
                        updateResultSlot();
                        selectionOffset = 0;
                        updateIngredient();
                    }
                    super.setFocused(bl);
                }
                @Override
                public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, int i, int j) {
                    super.renderTooltip(minecraft, graphics, i, j);
                    if (!isFocused()) return;
                    if (isHoveredTop) renderTooltip(minecraft,graphics, getFocusedRecipes().get(getFocusedRecipes().size() - 1).getResultItem(minecraft.level.registryAccess()),i,j);
                    if (isHoveredBottom) renderTooltip(minecraft,graphics, getFocusedRecipes().get(1).getResultItem(minecraft.level.registryAccess()),i,j);
                    if (hasAutoCrafting()) {
                        if(!ingredientSlot.isEmpty() && ScreenUtil.isHovering(menu.getSlot(0),leftPos,topPos,i,j)) renderTooltip(minecraft,graphics,getActualItem(ingredientSlot.get(0)),i,j);
                        if(ScreenUtil.isHovering(menu.getSlot(1),leftPos,topPos,i,j)) renderTooltip(minecraft,graphics,getFocusedResult(),i,j);
                    }
                }
                private StonecutterRecipe getFocusedRecipe(){
                    return isValidIndex() ? getFocusedRecipes().get(selectionOffset == -1 ? getFocusedRecipes().size() - 1 : selectionOffset == 1 ? 1 : 0) : null;
                }
                private ItemStack getFocusedResult(){
                    return getFocusedRecipe() == null ? ItemStack.EMPTY : getFocusedRecipe().getResultItem(minecraft.level.registryAccess()) ;
                }
                private void updateIngredient(){
                    updateIngredient(getFocusedRecipe());
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if (i == InputConstants.KEY_X) {
                        onlyCraftableRecipes = !onlyCraftableRecipes;
                        listener.slotChanged(menu,0,ItemStack.EMPTY);
                        focusedRecipes = null;
                        updateIngredient();
                        return true;
                    }
                    int oldSelection = selectionOffset;
                    if ((i == 263 && index == 0) || (i == 262 && index == craftingButtons.size() - 1)){
                        int oldOffset = craftingButtonsOffset.get();
                        craftingButtonsOffset.add(i == 263 ? -1 : 1,true);
                        if ((oldOffset == craftingButtonsOffset.max && i == 262) || (oldOffset == 0 && i == 263)) StonecutterScreenMixin.this.setFocused(craftingButtons.get(i == 263 ? craftingButtons.size() - 1 : 0));
                        else {
                            scrollRenderer.updateScroll(i == 263 ? ScreenDirection.LEFT : ScreenDirection.RIGHT);
                            focusedRecipes = null;
                        }
                        ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(), 1.0f);
                        return true;
                    }
                    if ((i == 265 || i == 264) && isValidIndex()) {
                        if (i == InputConstants.KEY_UP && (getRecipes().size() > 2 || selectionOffset == 1))
                            selectionOffset = Math.max(selectionOffset - 1, -1);
                        if (i == InputConstants.KEY_DOWN && getRecipes().size() >= 2)
                            selectionOffset = Math.min(selectionOffset + 1, 1);
                        if (oldSelection != selectionOffset || canScroll()) {
                            ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(), 1.0f);
                            if (oldSelection == selectionOffset && selectionOffset != 0)
                                Collections.rotate(getFocusedRecipes(), -selectionOffset);
                            updateIngredient(getFocusedRecipe());
                            return true;
                        }
                    }
                    return super.keyPressed(i, j, k);
                }

                private void updateIngredient(StonecutterRecipe rcp) {
                    ingredientSlot = rcp == null ? Collections.emptyList() : rcp.getIngredients();
                }
                private ItemStack getActualItem(Ingredient ingredient){
                    return ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.getItems()[(int) ((Util.getMillis() / 800)% ingredient.getItems().length)];
                }
                @Override
                public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
                    if (hasAutoCrafting()) {
                        if(!ingredientSlot.isEmpty()) ScreenUtil.iconHolderRenderer.slotBounds(leftPos, topPos, menu.getSlot(0)).itemHolder(getActualItem(ingredientSlot.get(0)), !onlyCraftableRecipes && !ingredientSlot.get(0).isEmpty() && warningInputSlot).render(graphics, i, j, f);
                        ScreenUtil.iconHolderRenderer.slotBounds(leftPos, topPos, menu.getSlot(1)).itemHolder(getFocusedResult(), !canCraft(getFocusedRecipe())).render(graphics, i, j, f);
                    }
                    if (isValidIndex()) {
                        Component resultName = getFocusedResult().getHoverName();
                        ScreenUtil.renderScrollingString(graphics,font,resultName,leftPos + 11 + Math.max(163 - font.width(resultName),0) / 2,topPos + 114, leftPos + 170, topPos + 125,4210752, false);
                        graphics.pose().pushPose();
                        graphics.pose().translate(getXCorner() - 4.5f, getYCorner(), 0f);
                        applyTranslation(graphics);
                        RenderSystem.disableDepthTest();
                        if (getFocusedRecipes().size() == 2) {
                            graphics.blitSprite(LegacySprites.CRAFTING_2_SLOTS_SELECTION_SPRITE, 0, -12, 36, 78);
                        }else if (getFocusedRecipes().size() > 2)
                            graphics.blitSprite(LegacySprites.CRAFTING_SELECTION_SPRITE, 0, -39, 36, 105);
                        graphics.pose().popPose();
                        if (getFocusedRecipes().size() >= 2){
                            ScreenUtil.secureTranslucentRender(graphics, !canCraft(getFocusedRecipes().get(1)), 0.5f, ()-> renderItem(graphics, getFocusedRecipes().get(1).getResultItem(minecraft.level.registryAccess()),getX(),getY() + 27,false));
                            if (getFocusedRecipes().size() >= 3) ScreenUtil.secureTranslucentRender(graphics, !canCraft(getFocusedRecipes().get(getFocusedRecipes().size() - 1)), 0.5f, ()-> renderItem(graphics, getFocusedRecipes().get(getFocusedRecipes().size() - 1).getResultItem(minecraft.level.registryAccess()),getX(),getY() - 27,false));
                        }
                        RenderSystem.enableDepthTest();
                    }
                    graphics.pose().pushPose();
                    graphics.pose().translate(0,selectionOffset * 27,0);
                    super.renderSelection(graphics,i,j,f);
                    graphics.pose().popPose();
                }
                private boolean canScroll(){
                    return getRecipes().size() >= 3;
                }
                private boolean isValidIndex() {
                    return !getRecipes().isEmpty();
                }
                @Override
                public boolean mouseScrolled(double d, double e, double f, double g) {
                    if (isFocused() && canScroll()){
                        Collections.rotate(getFocusedRecipes(),(int)Math.signum(g));
                        updateIngredient();
                        return true;
                    }
                    return false;
                }
                private boolean isMouseOver(double d, double e, int selection){
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
                    if (oldSelection != selectionOffset) updateIngredient();
                    else super.onClick(d, e);
                }

                @Override
                public void playClickSound() {
                    if (!isFocused()) super.playClickSound();
                }

                @Override
                public void onPress(){
                    if (isValidIndex()) {
                        if (isFocused()) {
                            if (hasAutoCrafting() && canCraft(getFocusedRecipe())) {
                                LegacyMinecraft.NETWORK.sendToServer(new ServerInventoryCraftPacket(getFocusedRecipe(), -Item.getId(getFocusedResult().getItem()), 2, 38));
                            } else ScreenUtil.playSimpleUISound(LegacySoundEvents.CRAFT_FAIL.get(), 1.0f);
                        }
                        updateResultSlot();
                    }
                }
            });
            h.offset = new Offset(0.5,0.5,0);
            h.allowItemDecorations = false;
        }
    }
}
