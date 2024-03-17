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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.Nullable;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.LegacySprites;
import wily.legacy.client.Offset;
import wily.legacy.client.controller.ComponentState;
import wily.legacy.client.controller.ControllerComponent;
import wily.legacy.client.controller.ControllerEvent;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.network.ServerInventoryCraftPacket;
import wily.legacy.util.PagedList;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.*;
import java.util.stream.Collectors;

import static wily.legacy.client.LegacySprites.SMALL_ARROW_SPRITE;
import static wily.legacy.client.screen.ControlTooltip.*;

public class LegacyCraftingScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements ControllerEvent {
    private final Inventory inventory;
    private final boolean is2x2;
    private final int gridDimension;
    private int lastFocused;
    private boolean onlyCraftableRecipes = false;
    protected final List<Ingredient> ingredientsGrid;
    protected final List<LegacyIconHolder> craftingButtons = new ArrayList<>();
    protected final List<List<List<CraftingRecipe>>> recipesByTab = new ArrayList<>();
    protected List<List<CraftingRecipe>> filteredRecipesByGroup = Collections.emptyList();
    protected final Stocker.Sizeable page =  new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset =  new Stocker.Sizeable(0);
    protected final TabList tabList = new TabList(new PagedList<>(page,7));
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            if (onlyCraftableRecipes)
                filteredRecipesByGroup = recipesByTab.get(tabList.selectedTab).stream().map(l -> l.stream().filter(r -> ServerInventoryCraftPacket.canCraft(r.getIngredients(), inventory)).toList()).filter(l -> !l.isEmpty()).toList();
        }

        public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

        }
    };;
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
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().tooltips.set(0,create(()->getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_RETURN,true) : ControllerComponent.DOWN_BUTTON.componentState.getIcon(true),()->getFocused() instanceof LegacyIconHolder h && !h.isWarning()? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.create") : null));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_O,true) : ControllerComponent.UP_BUTTON.componentState.getIcon(true), ()-> CONTROL_ACTION_CACHE.getUnchecked(onlyCraftableRecipes ? "legacy.action.all_recipes" : "legacy.action.show_craftable_recipes"));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().addCompound(()-> new Component[]{ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET,true) : ControllerComponent.LEFT_BUMPER.componentState.getIcon(true),ControlTooltip.SPACE,ControlTooltip.getActiveType().isKeyboard() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET,true) : ControllerComponent.RIGHT_BUMPER.componentState.getIcon(true)},()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.group"));
        ((LegacyMenuAccess<?>)this).getControlTooltipRenderer().add(()-> page.max > 0 ? ControlTooltip.getActiveType().isKeyboard() ? COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT,true),ControlTooltip.PLUS,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT,true),ControlTooltip.SPACE,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT,true)}) : ControllerComponent.RIGHT_STICK.componentState.getIcon(true) : null,()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.page"));
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
                listener.slotChanged(menu,-1,ItemStack.EMPTY);
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
                listener.slotChanged(menu,-1,ItemStack.EMPTY);
                setFocused(null);
                craftingButtonsOffset.set(0);
                if (inited) repositionElements();
            });
        });
        tabList.resetSelectedTab();
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
    public void componentTick(ComponentState state) {
        if (state.is(ControllerComponent.RIGHT_STICK) && state instanceof ComponentState.Stick s && s.pressed && s.canClick()) controlPage(s.x > 0 && s.x > Math.abs(s.y),s.x < 0 && -s.x > Math.abs(s.y));
    }
    @Override
    protected void init() {
        imageWidth = 348;
        imageHeight = 215;
        super.init();
        topPos+=18;
        menu.addSlotListener(listener);
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
                    if (!ingredientsGrid.isEmpty() && isFocused() && getFocusedRecipes().isEmpty()) updateIngredientGrid(null);
                    isHoveredTop = isFocused() && getFocusedRecipes().size() > 2 && isMouseOver(i,j,-1);
                    isHoveredBottom = isFocused() && getFocusedRecipes().size() >= 2 && isMouseOver(i,j,1);
                    itemIcon = isValidIndex() ? getFocusedRecipes().get(0).getResultItem(minecraft.level.registryAccess()) : ItemStack.EMPTY;
                    super.render(graphics, i, j, f);
                }
                @Override
                public void renderItem(GuiGraphics graphics, int i, int j, float f) {
                    if (!isValidIndex()) return;
                    ScreenUtil.secureTranslucentRender(graphics, !canCraft(getFocusedRecipes().get(0)),0.5f, ()->renderItem(graphics,itemIcon,getX(),getY(),false));
                }

                @Override
                public boolean isWarning() {
                    return getFocusedRecipe() == null || !canCraft(getFocusedRecipe());
                }
                public ResourceLocation getIconHolderSprite(){
                    return ICON_HOLDER;
                }

                private boolean canCraft(CraftingRecipe rcp){
                    if (rcp == null || onlyCraftableRecipes) return true;
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
                    List<List<CraftingRecipe>> list = onlyCraftableRecipes ? filteredRecipesByGroup : recipesByTab.get(page.get() * 7 + tabList.selectedTab);
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
                    for (int index = 0; index < ingredientsGrid.size(); index++)
                        if (ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23,23,23)) renderTooltip(minecraft,graphics,getActualItem(ingredientsGrid.get(index)),i,j);
                    if (ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 113 : 124), topPos + 151,36,36)) renderTooltip(minecraft,graphics,getFocusedResult(),i,j);
                    if (getFocusedRecipes().size() <= 1) return;
                    if (isHoveredTop) renderTooltip(minecraft,graphics, getFocusedRecipes().get(getFocusedRecipes().size() - 1).getResultItem(minecraft.level.registryAccess()),i,j);
                    if (isHoveredBottom) renderTooltip(minecraft,graphics, getFocusedRecipes().get(1).getResultItem(minecraft.level.registryAccess()),i,j);
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
                    if (i == InputConstants.KEY_O) {
                        onlyCraftableRecipes = !onlyCraftableRecipes;
                        listener.slotChanged(menu,0,ItemStack.EMPTY);
                        focusedRecipes = null;
                        updateIngredientGrid(getFocusedRecipe());
                        return true;
                    }
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
                    return ingredient.isEmpty() || ingredient.getItems().length == 0 ? ItemStack.EMPTY : ingredient.getItems()[(int) ((Util.getMillis() / 800)% ingredient.getItems().length)];
                }
                @Override
                public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
                    for (int index = 0; index < ingredientsGrid.size(); index++)
                        ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23,23,23, getActualItem(ingredientsGrid.get(index)), !onlyCraftableRecipes && !getActualItem(ingredientsGrid.get(index)).isEmpty() && warningSlots[index],new Offset(0.5, is2x2 ? 0 : 0.5, 0)).render(graphics,i,j,f);;
                    ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 113 : 124), topPos + 151,36,36, getFocusedResult(), !onlyCraftableRecipes && !canCraft(getFocusedRecipe()),new Offset(0.5, 0, 0)).render(graphics, i, j, f);
                    if (isValidIndex()) {
                        Component resultName = getFocusedResult().getHoverName();
                        ScreenUtil.renderScrollingString(graphics,font,resultName,leftPos + 11 + Math.max(163 - font.width(resultName),0) / 2,topPos + 114, leftPos + 170, topPos + 125,4210752, false);
                        graphics.pose().pushPose();
                        graphics.pose().translate(getXCorner() - 4.5f, getYCorner(), 0f);
                        applyOffset(graphics);
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
                            LegacyMinecraft.NETWORK.sendToServer(new ServerInventoryCraftPacket(getFocusedRecipe(),hasShiftDown() || ControllerComponent.LEFT_STICK_BUTTON.componentState.pressed));
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
        if (hasShiftDown()) controlPage(i == 262 , i == 263);
        return super.keyPressed(i, j, k);
    }
    protected void controlPage(boolean left, boolean right){
        if ((left|| right) && page.max > 0){
            int lastPage = page.get();
            page.add( left ? 1 : -1);
            if (lastPage != page.get()) {
                tabList.resetSelectedTab();
                rebuildWidgets();
            }
        }
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
