package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BannerPattern;
//? if >=1.20.5 {
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.StackIngredient;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.PagedList;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.RecipeInfo;
import wily.legacy.client.LoomTabListing;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.network.ServerMenuCraftPayload;

import java.util.*;
import java.util.stream.Collectors;

import static wily.legacy.util.LegacySprites.SMALL_ARROW;
import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.LegacyCraftingScreen.clearIngredients;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class LegacyLoomScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event,ControlTooltip.Event,TabList.Access {
    private final Inventory inventory;
    protected final List<ItemStack> compactInventoryList = new ArrayList<>();
    protected final List<Optional<Ingredient>> ingredientsGrid = new ArrayList<>(Collections.nCopies(9,Optional.empty()));
    protected final List<Optional<Ingredient>> selectedIngredients = new ArrayList<>();
    protected ItemStack resultStack = ItemStack.EMPTY;
    protected ItemStack selectedStack = ItemStack.EMPTY;
    protected ItemStack previewStack = ItemStack.EMPTY;
    protected final UIDefinition.Accessor accessor = UIDefinition.Accessor.of(this);
    protected final ScrollableRenderer scrollableRenderer = new ScrollableRenderer();
    protected final List<LoomTabListing> loomTabListings = List.copyOf(LoomTabListing.map.values());
    protected final List<RecipeIconHolder<BannerRecipe>> craftingButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> selectBannerButton = Collections.singletonList(new CustomRecipeIconHolder(){
        @Override
        public @Nullable Component getAction(Context context) {
            return context.actionOfContext(KeyContext.class, c-> c.key() == InputConstants.KEY_O && isFocused() && hasItem() ? LegacyComponents.SELECT : null);
        }

        public Component getDisplayName() {
            return LegacyComponents.SELECT_BANNER_TAB;
        }

        public ItemStack nextItem() {
            return nextItem(inventory, i-> i.getItem() instanceof BannerItem && Legacy4J.getItemPatternsCount(i) < 6);
        }
        public ItemStack previousItem() {
            return previousItem(inventory, i-> i.getItem() instanceof BannerItem && Legacy4J.getItemPatternsCount(i) < 6);
        }
        public boolean applyNextItemIfAbsent() {
            return true;
        }

        public boolean canCraft() {
            return true;
        }

        @Override
        public void craft() {
        }

        public List<Optional<Ingredient>> getIngredientsGrid() {
            return ingredientsGrid;
        }

        int findInventoryMatchSlot() {
            for (int i = 0; i < menu.slots.size(); i++)
                if (menu.slots.get(i).getItem() == itemIcon) return i;
            itemIcon = nextItem;
            return itemIcon.isEmpty() ? 0 : findInventoryMatchSlot();
        }

        @Override
        public void render(GuiGraphics graphics, int i, int j, float f) {
            if (isFocused()) selectedCraftingButton = getCraftingButtons().indexOf(this);
            super.render(graphics, i, j, f);
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (i == InputConstants.KEY_O && !itemIcon.isEmpty() && hasItem()) {
                updateRecipe();
                selectedStack = itemIcon.copyWithCount(1);
                craftingTabList.tabButtons.get(1).onPress();
                return true;
            }
            return super.keyPressed(i, j, k);
        }

        void updateRecipe() {
            clearIngredients(ingredientsGrid);
            resultStack = itemIcon.copyWithCount(1);
            ingredientsGrid.set(4, resultStack.isEmpty() ? Optional.empty() : Optional.of(StackIngredient.of(true, resultStack, 1)));
            scrollableRenderer.scrolled.set(0);
        }

        @Override
        LegacyScrollRenderer getScrollRenderer() {
            return scrollRenderer;
        }
    });
    protected List<RecipeInfo<BannerRecipe>> selectedPatterns = new ArrayList<>();
    protected List<List<RecipeInfo<BannerRecipe>>> recipesByGroup = new ArrayList<>();
    protected final Stocker.Sizeable page =  new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset =  new Stocker.Sizeable(0);
    protected final TabList craftingTabList = new TabList(new PagedList<>(page,7));
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private final boolean[] warningSlots = new boolean[9];
    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            recipesByGroup.clear();
            selectedIngredients.clear();
            if (craftingTabList.selectedTab != 0 && !selectedStack.isEmpty()) {
                previewStack = selectedStack.copy();
                selectedIngredients.add(Optional.of(StackIngredient.of(true, selectedStack, 1)));
                if (!selectedPatterns.isEmpty()) {
                    //? if <1.20.5 {
                    /*CompoundTag beTag = previewStack.getOrCreateTagElement("BlockEntityTag");
                    ListTag patternsTag = beTag.getList("Patterns", 10);
                    if (!beTag.contains("Patterns", 9)) beTag.put("Patterns", patternsTag);
                    *///?} else
                    List<BannerPatternLayers.Layer> layersList = previewStack.get(DataComponents.BANNER_PATTERNS) == null ? new ArrayList<>() : new ArrayList<>(previewStack.get(DataComponents.BANNER_PATTERNS).layers());
                    selectedPatterns.forEach(rcp -> {
                        //? if <1.20.5 {
                        /*CompoundTag patternTag = new CompoundTag();
                        patternsTag.add(patternTag);
                        patternTag.putString("Pattern", BuiltInRegistries.BANNER_PATTERN.get(rcp.get().pattern()).getHashname());
                        patternTag.putInt("Color", rcp.get().color.getId());
                        *///?} else
                        layersList.add(new BannerPatternLayers.Layer(Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(rcp.get().pattern),rcp.get().color));
                        for (int i1 = 1; i1 < rcp.getOptionalIngredients().size(); i1++) {
                            Optional<Ingredient> ing = rcp.getOptionalIngredients().get(i1);
                            if (ing.isEmpty()) continue;
                            selectedIngredients.add(ing);
                        }
                    });
                    //? if >=1.20.5
                    previewStack.set(DataComponents.BANNER_PATTERNS,new BannerPatternLayers(layersList));
                }
                Optional<Ingredient> previewIng = Optional.of(StackIngredient.of(true,previewStack,1));
                loomTabListings.get(craftingTabList.selectedTab - 1).patterns().stream().filter(p->Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).listElementIds().anyMatch(p::equals)).forEach(p -> recipesByGroup.add(Arrays.stream(DyeColor.values()).map(color -> {
                    ItemStack result = previewStack.copy();
                    //? if <1.20.5 {
                    /*CompoundTag beTag = result.getOrCreateTagElement("BlockEntityTag");
                    ListTag patternsTag = beTag.getList("Patterns", 10);
                    if (!beTag.contains("Patterns", 9)) beTag.put("Patterns", patternsTag);
                    CompoundTag addPattern = new CompoundTag();
                    patternsTag.add(addPattern);
                    addPattern.putString("Pattern", BuiltInRegistries.BANNER_PATTERN.get(p).getHashname());
                    addPattern.putInt("Color", color.getId());
                    *///?} else {
                    List<BannerPatternLayers.Layer> layersList = result.get(DataComponents.BANNER_PATTERNS) == null ? new ArrayList<>() : new ArrayList<>(result.get(DataComponents.BANNER_PATTERNS).layers());
                    layersList.add(new BannerPatternLayers.Layer(Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(p),color));
                    result.set(DataComponents.BANNER_PATTERNS,new BannerPatternLayers(layersList));
                    //?}
                    Optional<Ingredient> dye = Optional.of(Ingredient.of(DyeItem.byColor(color)));
                    ArrayList<Optional<Ingredient>> previewIngs = new ArrayList<>(selectedIngredients);
                    previewIngs.add(dye);
                    List<Optional<Ingredient>> ings = List.of(previewIng, dye);
                    List<Optional<Ingredient>> displayIngs = new ArrayList<>(ings);
                    Optional<Ingredient> extraIng = LegacyCraftingMenu.getBannerPatternExtraIngredient(Minecraft.getInstance().getConnection().registryAccess(), p);
                    if (extraIng.isPresent()){
                        displayIngs.add(1,extraIng);
                        previewIngs.add(extraIng);
                    }
                    displayIngs.set(0,selectedIngredients.get(0));
                    return RecipeInfo.create(p.location().withPrefix(color.getName() + "_"), new BannerRecipe(previewIngs, LegacyCraftingMenu.updateShapedIngredients(new ArrayList<>(Collections.nCopies(9,Optional.empty())), displayIngs, 3, 2, 2), p, color), ings, result);
                }).collect(Collectors.collectingAndThen(Collectors.toList(), l -> {
                    Collections.reverse(l);
                    return l;
                }))));
                craftingButtons.get(selectedCraftingButton).updateRecipeDisplay();
            }else {
                if (getCraftingButtons().size() > selectedCraftingButton && getCraftingButtons().get(selectedCraftingButton) instanceof LegacyCraftingScreen.CustomCraftingIconHolder h) h.updateRecipe();
            }
        }

        public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

        }
    };
    protected int selectedCraftingButton;
    protected boolean inited;
    public LegacyLoomScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        this.inventory = inventory;
        craftingTabList.addTabButton(43,0,LegacyTabButton.iconOf(Items.WHITE_BANNER),Component.empty(),t-> resetElements());
        for (LoomTabListing listing : loomTabListings) {
            craftingTabList.addTabButton(43,0, listing.icon(),listing.name(), t->resetElements());
        }
        craftingTabList.resetSelectedTab();
        inited = true;
        addCraftingButtons();
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        ControlTooltip.setupDefaultButtons(renderer,this);
        Event.super.addControlTooltips(renderer);
        renderer.
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()-> ControlTooltip.getKeyMessage(InputConstants.KEY_O,this)).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> getFocused() instanceof RecipeIconHolder<?> r && r.getFocusedRecipe() != null && selectedPatterns.contains(r.getFocusedRecipe()) ? LegacyComponents.REMOVE : null).
                addCompound(()-> getTabList().selectedTab == 0 ? new ControlTooltip.Icon[]{} : new ControlTooltip.Icon[]{ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(),ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon()},()->LegacyComponents.GROUP).
                add(()-> page.max > 0 ? ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_STICK.bindingState.getIcon() : null,()->LegacyComponents.PAGE);
    }

    public void resetElements(){
        listener.slotChanged(menu,-1,ItemStack.EMPTY);
        selectedCraftingButton = 0;
        craftingButtonsOffset.set(0);
        if (inited) repositionElements();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        if (guiEventListener instanceof TabList) return;
        super.setFocused(guiEventListener);
    }
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        Component title = craftingTabList.selectedTab != 0 ? craftingTabList.tabButtons.get(craftingTabList.selectedTab).getMessage() : getFocused() instanceof CustomRecipeIconHolder h ? h.getDisplayName() : Component.empty();
        guiGraphics.drawString(this.font, title,((craftingTabList.selectedTab != 0 ? imageWidth : imageWidth / 2) - font.width(title)) / 2,17, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        if (menu.inventoryActive) guiGraphics.drawString(this.font, this.playerInventoryTitle, (355 + 160 - font.width(playerInventoryTitle))/ 2, 114, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        else guiGraphics.drawString(this.font, LegacyComponents.PREVIEW, (355 + 160 - font.width(LegacyComponents.PREVIEW))/ 2, 114, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        guiGraphics.pose().translate(-leftPos,-topPos,0);
        getCraftingButtons().forEach(b-> b.render(guiGraphics,i,j,0));
        if (selectedCraftingButton < getCraftingButtons().size()) getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
        guiGraphics.pose().translate(leftPos,topPos,0);
    }
    @Override
    public void bindingStateTick(BindingState state) {
        if (state.pressed && state.canClick() && state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s) controlPage(s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y));
    }

    @Override
    public TabList getTabList() {
        return craftingTabList;
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
    protected void init() {
        resultStack = ItemStack.EMPTY;
        imageWidth = 348;
        imageHeight = 215;
        super.init();
        topPos+=18;
        menu.addSlotListener(listener);
        craftingTabList.selectedTab = selectedStack.isEmpty() ? 0 : Math.max(craftingTabList.selectedTab,1);
        menu.inventoryActive = selectedStack.isEmpty();
        if (selectedCraftingButton < getCraftingButtons().size()) setFocused(getCraftingButtons().get(selectedCraftingButton));
        if (craftingTabList.selectedTab != 0) {
            craftingButtonsOffset.max = Math.max(0,loomTabListings.get(page.get() * 7 + craftingTabList.selectedTab - 1).patterns().size() - 12);
            craftingButtons.forEach(b->{
                b.setPos(leftPos + 13 + craftingButtons.indexOf(b) * 27,topPos + 38);
                addWidget(b);
            });
        }else {
            getCraftingButtons().forEach(b->{
                b.setPos(leftPos + 77 + getCraftingButtons().indexOf(b) * 55,topPos + 39);
                b.offset = new Vec3(getCraftingButtons().indexOf(b) * 0.5,0,0);
                b.init();
                addWidget(b);
            });
        }

        addWidget(craftingTabList);
        craftingTabList.init(leftPos, topPos - 37, imageWidth, (t, i) -> {
            int index = craftingTabList.tabButtons.indexOf(t);
            t.active = index == 0 && selectedStack.isEmpty() || index != 0 && !selectedStack.isEmpty();
            t.type = index == 0 ? 0 : index >= 6 ? 2 : 1;
            t.setWidth(51);
            t.offset = (t1) -> new Vec3(-1.5 * craftingTabList.tabButtons.indexOf(t), t1.active ? t1.selected ? 0 : 4.5: 26.5, 0);
        });
    }
    protected boolean canCraft(){
        return selectedIngredients.size() > 1 && LegacyLoomScreen.this.canCraft(selectedIngredients,false);
    }

    protected boolean canCraft(List<Optional<Ingredient>> ingredients, boolean isFocused) {
        compactInventoryList.clear();
        RecipeMenu.handleCompactInventoryList(compactInventoryList,Minecraft.getInstance().player.getInventory(),menu.getCarried());
        return LegacyCraftingScreen.canCraft(compactInventoryList, ingredients, isFocused ? warningSlots : null);
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

    protected void addCraftingButtons(){
        for (int i = 0; i < 12; i++) {
            int index = i;

            RecipeIconHolder<BannerRecipe> h;
            craftingButtons.add(h = new RecipeIconHolder<>(leftPos + 13 + i * 27, topPos + 38) {
                @Override
                public @Nullable Component getAction(Context context) {
                    return context.actionOfContext(KeyContext.class, c-> c.key() == InputConstants.KEY_RETURN && isFocused() && LegacyLoomScreen.this.canCraft() ? LegacyComponents.CREATE : c.key() == InputConstants.KEY_O && isFocused() && canCraft() ? LegacyComponents.ADD : null);
                }
                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }

                protected boolean canCraft(RecipeInfo<BannerRecipe> rcp) {
                    if (rcp == null) return true;
                    return Legacy4J.getItemPatternsCount(previewStack) < 6 && LegacyLoomScreen.this.canCraft(rcp.get().displayIngredients(),isFocused() && getFocusedRecipe() == rcp) && LegacyLoomScreen.this.canCraft(rcp.get().previewIngredients,false);
                }

                protected List<RecipeInfo<BannerRecipe>> getRecipes() {
                    return recipesByGroup.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : recipesByGroup.get(craftingButtonsOffset.get() + index);
                }

                @Override
                protected void toggleCraftableRecipes() {
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if ((i == InputConstants.KEY_O || i == InputConstants.KEY_X) && isValidIndex()){
                        if (i == InputConstants.KEY_O){
                            if (this.canCraft()){
                                selectedPatterns.add(getFocusedRecipe());
                                ScreenUtil.playSimpleUISound(SoundEvents.UI_LOOM_SELECT_PATTERN,1.0f);
                            }
                        } else selectedPatterns.remove(getFocusedRecipe());
                        int cycle = getFocusedRecipes().indexOf(getFocusedRecipe()) - getRecipes().indexOf(getFocusedRecipe());
                        focusedRecipes = null;
                        listener.slotChanged(menu,-1,ItemStack.EMPTY);
                        Collections.rotate(getFocusedRecipes(),cycle);
                        updateRecipeDisplay();
                        return true;
                    }
                    if (controlCyclicNavigation(i, index, craftingButtons, craftingButtonsOffset, scrollRenderer, LegacyLoomScreen.this))
                        return true;
                    return super.keyPressed(i, j, k);
                }

                protected void updateRecipeDisplay(RecipeInfo<BannerRecipe> rcp) {
                    resultStack = getFocusedResult();
                    clearIngredients(ingredientsGrid);
                    if (rcp == null){
                        return;
                    }
                    for (int i = 0; i < rcp.get().displayIngredients().size(); i++)
                        ingredientsGrid.set(i, rcp.get().displayIngredients().get(i));
                }
                public void onPress(){
                    if (isFocused() && isValidIndex()){
                        if (LegacyLoomScreen.this.canCraft()){
                            ScreenUtil.playSimpleUISound(SoundEvents.UI_LOOM_TAKE_RESULT,1.0f);
                            selectedPatterns.forEach(b-> CommonNetwork.sendToServer(new ServerMenuCraftPayload(Optional.of(b.get().pattern.location()),b.getOptionalIngredients(),-1, Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.bindingState.pressed)));
                            selectedPatterns.clear();
                            selectedStack = ItemStack.EMPTY;
                            previewStack = ItemStack.EMPTY;
                            craftingTabList.tabButtons.get(0).onPress();
                        } else ScreenUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(),1.0f);
                    }
                }

            });
            h.allowCraftableRecipesToggle = false;
            h.offset = new Vec3(0.5,0.5,0);
        }
    }

    public record BannerRecipe(List<Optional<Ingredient>> previewIngredients, List<Optional<Ingredient>> displayIngredients, ResourceKey<BannerPattern> pattern, DyeColor color) {

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

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        craftingTabList.render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(accessor.getElementValue("imageSprite",LegacySprites.SMALL_PANEL, ResourceLocation.class), leftPos, topPos, imageWidth, imageHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + 9, topPos + 103, 163, 105);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 176, topPos + 103, 163, 105);
        if (!menu.inventoryActive && !previewStack.isEmpty()){
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(leftPos + 220.5, topPos + 130.5,0);
            guiGraphics.pose().scale(4.25f,4.25f,4.25f);
            guiGraphics.renderItem(previewStack,0,0);
            guiGraphics.pose().popPose();
        }
        if (craftingTabList.selectedTab == 0) FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 176, topPos + 8, 163, 93);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(SMALL_ARROW, leftPos + 97, topPos + 161, 16, 13);
        if (craftingTabList.selectedTab != 0) {
            if (craftingButtonsOffset.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
            if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + 337, topPos + 45);
        }
    }


    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (hasShiftDown() && controlPage(i == 263, i == 262)) return true;
        if (craftingTabList.controlTab(i)) return true;
        return super.keyPressed(i, j, k);
    }
    protected boolean controlPage(boolean left, boolean right){
        if ((left || right) && page.max > 0 && craftingTabList.selectedTab != 0){
            int lastPage = page.get();
            page.add(left ? -1 : 1);
            if (lastPage != page.get()) {
                craftingTabList.resetSelectedTab();
                rebuildWidgets();
                return true;
            }
        }return false;
    }
    public List<? extends LegacyIconHolder> getCraftingButtons(){
        return craftingTabList.selectedTab == 0 ? selectBannerButton : craftingButtons;
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        //? if >1.20.1 {
        renderBackground(guiGraphics, i, j, f);
        //?}
        super.render(guiGraphics, i, j, f);
        for (int index = 0; index < ingredientsGrid.size(); index++)
            ScreenUtil.iconHolderRenderer.itemHolder(leftPos + 21 + index % 3 * 23, topPos +  133 + index / 3 * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), !getActualItem(ingredientsGrid.get(index)).isEmpty() &&  warningSlots[index], new Vec3(0.5, 0.5, 0)).render(guiGraphics, i, j, f);;
        ScreenUtil.iconHolderRenderer.itemHolder(leftPos +  124, topPos + 151, 36, 36, resultStack, getFocused() instanceof RecipeIconHolder<?> r && r.isValidIndex() && !r.canCraft(), new Vec3(0.5, 0, 0)).render(guiGraphics, i, j, f);
        if (!resultStack.isEmpty()) {
            Component resultName = resultStack.getHoverName();
            ScreenUtil.renderScrollingString(guiGraphics, font, resultName, leftPos + 11 + Math.max(163 - font.width(resultName), 0) / 2, topPos + 114, leftPos + 170, topPos + 125, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            if (craftingTabList.selectedTab == 0){
                List<Component> list = ScreenUtil.getTooltip(resultStack);
                scrollableRenderer.scrolled.max = Math.max(0,list.size()-6);
                scrollableRenderer.render(guiGraphics,leftPos + 180, topPos + 15, 152, 72,()->{
                    for (int i1 = 0; i1 < list.size(); i1++) {
                        guiGraphics.drawString(font, list.get(i1).copy().setStyle(Style.EMPTY), leftPos + 180, topPos + 15 + i1 * 12, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    }
                });
            }
            if (ScreenUtil.isMouseOver(i,j,leftPos + 124, topPos + 151,36,36)) guiGraphics.renderTooltip(font, resultStack,i,j);
        }
        for (int index = 0; index < ingredientsGrid.size(); index++) if (!getActualItem(ingredientsGrid.get(index)).isEmpty() && ScreenUtil.isMouseOver(i,j,leftPos +  21 + index % 3 * 23, topPos + 133 + index / 3 * 23,23,23)) guiGraphics.renderTooltip(font,getActualItem(ingredientsGrid.get(index)),i,j);
        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }
}
