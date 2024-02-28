package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.Nullable;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.LegacySprites;
import wily.legacy.client.Offset;
import wily.legacy.client.controller.ControllerComponent;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.PagedList;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.*;
import java.util.stream.Collectors;

import static wily.legacy.client.LegacySprites.SMALL_ARROW_SPRITE;

public class LegacyCraftingScreen extends AbstractContainerScreen<LegacyCraftingMenu> {
    private final Inventory inventory;
    private final boolean is2x2;
    private final int gridDimension;
    private int lastFocused;
    protected final List<Ingredient> ingredientsGrid;
    protected final List<LegacyIconHolder> craftingButtons = new ArrayList<>();
    protected final List<List<List<CraftingRecipe>>> recipesByTab = new ArrayList<>();
    protected final Stocker.Sizeable page =  new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset =  new Stocker.Sizeable(0);
    protected final TabList tabList = new TabList(new PagedList<>(page,7));
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected int selectedCraftingButton;
    public static final Item[] VANILLA_CATEGORY_ICONS = new Item[]{Items.BRICKS,Items.REDSTONE,Items.GOLDEN_SWORD,Items.LAVA_BUCKET};
    protected RecipeManager manager;
    public static LegacyCraftingScreen craftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component){
        return new LegacyCraftingScreen(abstractContainerMenu,inventory,component,false);
    }
    public static LegacyCraftingScreen playerCraftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component){
        return new LegacyCraftingScreen(abstractContainerMenu,inventory,component,true);
    }
    protected boolean inited = false;
    public LegacyCraftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component, boolean is2x2) {
        super(abstractContainerMenu, inventory, component);
        this.inventory = inventory;
        this.is2x2 = is2x2;
        gridDimension = is2x2 ? 2 : 3;
        ingredientsGrid = new ArrayList<>(Collections.nCopies(gridDimension * gridDimension,Ingredient.EMPTY));
        if (Minecraft.getInstance().level == null) return;
        manager = Minecraft.getInstance().level.getRecipeManager();
        for (LegacyCraftingTabListing listing : LegacyCraftingTabListing.list) {
            if (!listing.isValid()) continue;
            List<List<CraftingRecipe>> groups = new ArrayList<>();
            listing.craftings.values().forEach(l->{
                if (l.isEmpty()) return;
                List<CraftingRecipe> group = new ArrayList<>();
                l.forEach(v->v.addRecipes(RecipeType.CRAFTING,manager,group,r-> !is2x2 || is2x2Recipe(r)));
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) continue;

            recipesByTab.add(groups);

            tabList.addTabButton(43,0,listing.icon,listing.itemIconTag,listing.displayName, t->{
                setFocused(null);
                craftingButtonsOffset.set(0);
                if (inited) repositionElements();
            });

        }
        if (ScreenUtil.getLegacyOptions().vanillaTabs().get()) manager.getAllRecipesFor(RecipeType.CRAFTING).stream().collect(Collectors.groupingBy(h->h.value().category(),()->new TreeMap<>(Comparator.comparingInt(Enum::ordinal)),Collectors.groupingBy(h->h.value().getGroup().isEmpty() ? h.id().toString() : h.value().getGroup()))).forEach((category, m)->{
            if (m.isEmpty()) return;
            List<List<CraftingRecipe>> groups = new ArrayList<>();
            m.values().forEach(l->{
                List<CraftingRecipe> group = l.stream().filter(h->!(h.value() instanceof CustomRecipe) && (!is2x2 || is2x2Recipe(h.value()))).map(RecipeHolder::value).collect(Collectors.toList());
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) return;
            recipesByTab.add(groups);
            tabList.addTabButton(43,0,VANILLA_CATEGORY_ICONS[category.ordinal()].arch$registryName(), getTitle(), t->{
                setFocused(null);
                craftingButtonsOffset.set(0);
                if (inited) repositionElements();
            });
        });
        tabList.selectedTab = -1;
        tabList.tabButtons.get(0).onPress();
        inited = true;
        addCraftingButtons();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        if (guiEventListener instanceof TabList) return;
        super.setFocused(guiEventListener);
    }

    public boolean is2x2Recipe(CraftingRecipe recipe){
        return (!(recipe instanceof ShapedRecipe rcp) || Math.max(rcp.getHeight(), rcp.getWidth()) < 3) && (!(recipe instanceof ShapelessRecipe s) || s.getIngredients().size() <= 4);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        Component title = tabList.tabButtons.get(tabList.selectedTab).getMessage();
        guiGraphics.drawString(this.font, title,(imageWidth - font.width(title)) / 2, 17, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 114, 4210752, false);
    }
    public void repositionElements() {
        lastFocused = getFocused() instanceof LegacyIconHolder h ? craftingButtons.indexOf(h) : -1;
        super.repositionElements();
    }
    @Override
    protected void init() {
        imageWidth = 348;
        imageHeight = 215;
        super.init();
        topPos+=18;
        craftingButtonsOffset.max = Math.max(0,recipesByTab.get(page.get() * 7 + tabList.selectedTab).size() - 12);
        if (lastFocused >= 0 && lastFocused < craftingButtons.size()) setInitialFocus(craftingButtons.get(lastFocused));
        else setInitialFocus(craftingButtons.get(0));
        craftingButtons.forEach(b->{
            b.setPos(leftPos + 13 + craftingButtons.indexOf(b) * 27,topPos + 38);
            addRenderableWidget(b);
        });
        addWidget(tabList);
        tabList.init(leftPos,topPos-37, imageWidth,(t,i)->{
            int index = tabList.tabButtons.indexOf(t);
            t.type = index == 0 ? 0 : index >= 6 ? 2 : 1;
            t.setWidth(51);
            t.offset = (t1)-> new Offset(-1.5 * tabList.tabButtons.indexOf(t),t1.selected ? 0 : 4.5 ,0);
        });
    }


    protected void addCraftingButtons(){
        for (int i = 0; i < 12; i++) {
            int index = i;

            LegacyIconHolder h;
            craftingButtons.add(h = new LegacyIconHolder(leftPos + 13 + i * 27,topPos + 38,27, 27) {
                private int selectionOffset = 0;
                boolean isHoveredTop = false;
                boolean isHoveredBottom = false;
                private List<CraftingRecipe> focusedRecipes;
                private final boolean[] warningSlots = new boolean[gridDimension * gridDimension];
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

                private boolean canCraft(CraftingRecipe rcp){
                    if (rcp == null) return true;
                    boolean focusedRcp = isFocused() && getFocusedRecipe() == rcp;
                    List<Ingredient> ings = focusedRcp ? ingredientsGrid : rcp.getIngredients();
                    boolean canCraft = true;
                    for (int i1 = 0; i1 < ings.size(); i1++) {
                        Ingredient ing = ings.get(i1);
                        if (ing.isEmpty()) continue;
                        int itemCount = inventory.items.stream().filter(ing).mapToInt(ItemStack::getCount).sum();
                        long ingCount = ings.stream().filter(i-> i == ing).count();
                        if (itemCount >= ingCount || PagedList.occurrenceOf(ings,ing,i1) < itemCount) {
                            if (focusedRcp && ingredientsGrid.contains(ing)) warningSlots[i1] = false;
                        }else {
                            canCraft = false;
                            if (!focusedRcp || !ingredientsGrid.contains(ing)) break;
                            else warningSlots[i1] = true;
                        }
                    }
                    return canCraft;
                }
                private List<CraftingRecipe> getFocusedRecipes(){
                    if (!isFocused() || !isValidIndex() || !canScroll()) focusedRecipes = null;
                    else if (focusedRecipes == null) focusedRecipes = new ArrayList<>(getRecipes());
                    return focusedRecipes == null ? getRecipes() : focusedRecipes;
                }
                private List<CraftingRecipe> getRecipes(){
                    List<List<CraftingRecipe>> list = recipesByTab.get(page.get() * 7 + tabList.selectedTab);
                    return list.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : list.get(craftingButtonsOffset.get() + index);
                }
                @Override
                public void setFocused(boolean bl) {
                    if (bl){
                        selectionOffset = 0;
                        updateIngredientGrid();
                    }
                    super.setFocused(bl);
                }
                @Override
                public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, int i, int j) {
                    super.renderTooltip(minecraft, graphics, i, j);
                    if (!isFocused()) return;
                    if (isHoveredTop) renderTooltip(minecraft,graphics, getFocusedRecipes().get(getFocusedRecipes().size() - 1).getResultItem(minecraft.level.registryAccess()),i,j);
                    if (isHoveredBottom) renderTooltip(minecraft,graphics, getFocusedRecipes().get(1).getResultItem(minecraft.level.registryAccess()),i,j);
                    for (int index = 0; index < ingredientsGrid.size(); index++)
                        if (ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23,23,23)) renderTooltip(minecraft,graphics,getActualItem(ingredientsGrid.get(index)),i,j);
                    if (ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 113 : 124), topPos + 151,36,36)) renderTooltip(minecraft,graphics,getFocusedResult(),i,j);
                }
                private CraftingRecipe getFocusedRecipe(){
                    return isValidIndex() ? getFocusedRecipes().get(selectionOffset == -1 ? getFocusedRecipes().size() - 1 : selectionOffset == 1 ? 1 : 0) : null;
                }
                private ItemStack getFocusedResult(){
                    return getFocusedRecipe() == null ? ItemStack.EMPTY : getFocusedRecipe().getResultItem(minecraft.level.registryAccess()) ;
                }
                private void updateIngredientGrid(){
                    updateIngredientGrid(getFocusedRecipe());
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    int oldSelection = selectionOffset;
                    if ((i == 263 && index == 0) || (i == 262 && index == craftingButtons.size() - 1)){
                        int oldOffset = craftingButtonsOffset.get();
                        craftingButtonsOffset.add(i == 263 ? -1 : 1,true);
                        if ((oldOffset == craftingButtonsOffset.max && i == 262) || (oldOffset == 0 && i == 263)) LegacyCraftingScreen.this.setFocused(craftingButtons.get(i == 263 ? craftingButtons.size() - 1 : 0));
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
                            updateIngredientGrid(getFocusedRecipe());
                            return true;
                        }
                    }
                    return super.keyPressed(i, j, k);
                }

                private void updateIngredientGrid(CraftingRecipe rcp) {
                    for (int i = 0; i < ingredientsGrid.size(); i++) {
                        if (!ingredientsGrid.get(i).isEmpty()) ingredientsGrid.set(i,Ingredient.EMPTY);
                    }
                    if (!(rcp instanceof ShapedRecipe r)) {
                        if (rcp != null) for (int i = 0; i < rcp.getIngredients().size(); i++) ingredientsGrid.set(i,rcp.getIngredients().get(i));
                        return;
                    }

                    int rcpDimension = Math.max(r.getHeight(),r.getWidth());
                    Ingredient[] ingredients = new Ingredient[rcpDimension * rcpDimension];
                    for (int i = 0; i < r.getIngredients().size(); i++) ingredients[r.getWidth() < r.getHeight() ? (i / r.getWidth()) * r.getHeight() + (i % r.getWidth()) : i] = rcp.getIngredients().get(i);

                    for (int i = 0; i < ingredients.length; i++)
                        ingredientsGrid.set(i > 1 && gridDimension > rcpDimension ?  i + 1 : i, ingredients[i] == null ? Ingredient.EMPTY : ingredients[i]);
                }
                private ItemStack getActualItem(Ingredient ingredient){
                    return ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.getItems()[(int) ((Util.getMillis() / 800)% ingredient.getItems().length)];
                }
                @Override
                public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
                    for (int index = 0; index < ingredientsGrid.size(); index++)
                        ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23,23,23, getActualItem(ingredientsGrid.get(index)), !getActualItem(ingredientsGrid.get(index)).isEmpty() && warningSlots[index],new Offset(0.5, is2x2 ? 0 : 0.5, 0)).render(graphics,i,j,f);;
                    ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 113 : 124), topPos + 151,36,36, getFocusedResult(), !canCraft(getFocusedRecipe()),new Offset(0.5, 0, 0)).render(graphics, i, j, f);
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
                        updateIngredientGrid();
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
                    if (oldSelection != selectionOffset) updateIngredientGrid();
                    else super.onClick(d, e);
                }

                @Override
                public void playClickSound() {
                    if (!isFocused()) super.playClickSound();
                }

                @Override
                public void onPress(){
                    if (isFocused() && isValidIndex()){
                        if (canCraft(getFocusedRecipe())){
                            ScreenUtil.playSimpleUISound(LegacySoundEvents.CRAFT.get(),1.0f);
                            LegacyMinecraft.NETWORK.sendToServer(new ServerInventoryCraftPacket(getFocusedRecipe(),0,36));
                        }else ScreenUtil.playSimpleUISound(LegacySoundEvents.CRAFT_FAIL.get(),1.0f);
                    }
                }
            });
            h.offset = new Offset(0.5,0.5,0);
            h.allowItemDecorations = false;
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        tabList.render(guiGraphics,i,j,f);
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos,imageWidth,imageHeight,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 9,topPos + 103,163,105,2f);
        ScreenUtil.renderSquareRecessedPanel(guiGraphics,leftPos + 176,topPos + 103,163,105,2f);
        guiGraphics.blitSprite(SMALL_ARROW_SPRITE,leftPos + (is2x2 ? 87 : 97),topPos + 161,16,13);
        if (craftingButtonsOffset.get() > 0) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
        if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + 337, topPos + 45);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        tabList.controlTab(i);
        if (page.max > 0 && (hasShiftDown() || ControllerComponent.LEFT_STICK_BUTTON.componentState.pressed) && (i == 262 || i == 263)){
            int oldPage = page.get();
            page.add(i == 262 ? 1 : -1);
            if (oldPage != page.get()) {
                tabList.selectedTab = -1;
                tabList.tabButtons.get(0).onPress();
                return true;
            }
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBackground(guiGraphics, i, j, f);
        super.render(guiGraphics, i, j, f);
        craftingButtons.get(selectedCraftingButton).renderSelection(guiGraphics,i,j,f);
        craftingButtons.forEach(h-> h.renderTooltip(minecraft,guiGraphics,i,j));
        renderTooltip(guiGraphics, i, j);
    }
}
