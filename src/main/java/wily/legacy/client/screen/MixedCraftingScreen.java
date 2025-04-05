package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;


import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
//? if >=1.21.2 {
/*import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.recipebook.PlaceRecipeHelper;
*///?} else {
import net.minecraft.client.RecipeBookCategories;
//?}
import net.minecraft.client.multiplayer.ClientPacketListener;
//? if <1.21 {
/*import net.minecraft.client.searchtree.SearchRegistry;
*///?}
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;


import net.minecraft.world.item.crafting.*;


import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.util.PagedList;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;


import java.util.*;
import java.util.function.Function;

import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.util.LegacySprites.SMALL_ARROW;


public class MixedCraftingScreen<T extends /*? if <1.20.5 {*//*RecipeBookMenu<CraftingContainer>*//*?} else if <1.21.2 {*/RecipeBookMenu<CraftingInput, CraftingRecipe>/*?} else {*//*AbstractCraftingMenu*//*?}*/> extends AbstractContainerScreen<T> implements Controller.Event, Event,TabList.Access {
    private final Inventory inventory;
    protected final List<ItemStack> compactItemStackList = new ArrayList<>();
    protected final /*? if <1.21.2 {*/ StackedContents/*?} else {*//*StackedItemContents*//*?}*/ stackedContents = new /*? if <1.21.2 {*/ StackedContents/*?} else {*//*StackedItemContents*//*?}*/();
    private int timesInventoryChanged;
    private final boolean is2x2;
    private boolean onlyCraftableRecipes = false;
    protected Stocker.Sizeable infoType = new Stocker.Sizeable(0, 2);
    protected final UIAccessor accessor = UIAccessor.of(this);
    protected final List<RecipeIconHolder<VanillaCrafting>> craftingButtons = new PagedList<>(new Stocker.Sizeable(0, 0), () -> accessor.getInteger("maxCraftingButtonsCount", 12));
    protected List<List<RecipeInfo<VanillaCrafting>>> recipesByGroup = new ArrayList<>();
    protected final Stocker.Sizeable craftingButtonsOffset = new Stocker.Sizeable(0);
    protected final TabList craftingTabList = new TabList().add(43, 0, LegacyTabButton.iconOf(LegacySprites.STRUCTURES), LegacyComponents.STRUCTURES, t -> resetElements()).add(43, 0, LegacyTabButton.iconOf(LegacySprites.MECHANISMS), LegacyComponents.MECHANISMS, t -> resetElements()).add(43, 0, LegacyTabButton.iconOf(LegacySprites.TOOLS), LegacyComponents.TOOLS_AND_ARMOUR, t -> resetElements()).add(43, 0, LegacyTabButton.iconOf(LegacySprites.MISC), LegacyComponents.MISC, t -> resetElements()).add(43, 0, LegacyTabButton.iconOf(LegacySprites.SEARCH), LegacyComponents.SEARCH_ITEMS, t -> enableSearchMode(true));
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected final ScrollableRenderer scrollableRenderer = new ScrollableRenderer();
    protected boolean allowRecipeDisplay = false;
    protected final EditBox searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200,20, LegacyComponents.SEARCH_ITEMS);
    protected boolean searchMode = false;

    public static final /*? if <1.21.2 {*/RecipeBookCategories/*?} else {*//*ExtendedRecipeBookCategory*//*?}*/[] VANILLA_CATEGORIES = new /*? if <1.21.2 {*/RecipeBookCategories/*?} else {*//*ExtendedRecipeBookCategory*//*?}*/[]{RecipeBookCategories.CRAFTING_BUILDING_BLOCKS, RecipeBookCategories.CRAFTING_REDSTONE, RecipeBookCategories.CRAFTING_EQUIPMENT, RecipeBookCategories.CRAFTING_MISC, /*? if <1.21.2 {*/RecipeBookCategories.CRAFTING_SEARCH/*?} else {*//*SearchRecipeBookCategory.CRAFTING*//*?}*/};

    protected int selectedCraftingButton;

    public static MixedCraftingScreen<CraftingMenu> craftingScreen(CraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        return new MixedCraftingScreen<>(abstractContainerMenu, inventory, component, false);
    }

    public static MixedCraftingScreen<InventoryMenu> playerCraftingScreen(Player player) {
        return new MixedCraftingScreen<>(player.inventoryMenu, player.getInventory(), LegacyCraftingMenu.CRAFTING_TITLE, true);
    }

    public void enableSearchMode(boolean clearSearch){
        searchMode = true;
        resetElements(true, clearSearch);
        setFocused(searchBox);
    }

    public void disableSearchMode(){
        searchMode = false;
        resetElements(true, false);
    }

    public MixedCraftingScreen(T abstractContainerMenu, Inventory inventory, Component component, boolean is2x2) {
        super(abstractContainerMenu, inventory, component);
        this.inventory = inventory;
        this.is2x2 = is2x2;
        searchBox.setResponder(s-> updateStackedContents());
        searchBox.setMaxLength(50);
        resetElements(false, false);
        addCraftingButtons();
        accessor.getStaticDefinitions().add(UIDefinition.createBeforeInit(a -> accessor.putStaticElement("is2x2", is2x2)));
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        setupDefaultButtons(renderer, this);
        Event.super.addControlTooltips(renderer);
        renderer.
                add(EXTRA::get, () -> LegacyComponents.INFO).
                add(OPTION::get, () -> onlyCraftableRecipes ? LegacyComponents.ALL_RECIPES : LegacyComponents.SHOW_CRAFTABLE_RECIPES).
                add(()-> searchMode ? VERTICAL_NAVIGATION.get() : ControlTooltip.ComponentIcon.compoundOf(ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_LSHIFT) : ControllerBinding.LEFT_STICK_BUTTON.getIcon(),PLUS_ICON,OPTION.get()), () -> searchMode ? LegacyComponents.EXIT_SEARCH_MODE : LegacyComponents.SEARCH_MODE).
                addCompound(() -> new Icon[]{ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.getIcon(), SPACE_ICON, ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.getIcon()}, () -> LegacyComponents.GROUP);
    }

    public void resetElements() {
        searchMode = false;
        resetElements(true, true);
    }

    public void resetElements(boolean reposition, boolean resetCrafting) {
        if (!resetCrafting || searchBox.getValue().isEmpty()) updateStackedContents();
        else searchBox.setValue("");
        selectedCraftingButton = 0;
        if (resetCrafting){
            infoType.set(0);
            craftingButtonsOffset.set(0);
        }
        if (reposition) repositionElements();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener){
        if (guiEventListener instanceof TabList) return;
        super.setFocused(guiEventListener);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics,int i, int j){
        if (!searchMode) {
            Component title = getTabList() == craftingTabList ? getTabList().tabButtons.get(getTabList().selectedTab).getMessage() : CommonComponents.EMPTY;
            guiGraphics.drawString(this.font, title, (imageWidth - font.width(title)) / 2, 17, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        }
        int inventoryPanelX = accessor.getInteger("inventoryPanelX", 176);
        int inventoryPanelWidth = accessor.getInteger("inventoryPanelWidth", 163);
        if (infoType.get() <= 0) guiGraphics.drawString(this.font, this.playerInventoryTitle, inventoryPanelX + (inventoryPanelWidth - font.width(playerInventoryTitle)) / 2, accessor.getInteger("bottomPanelTitleY", 114), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        else {
            if (selectedCraftingButton < getCraftingButtons().size()) {
                RecipeIconHolder<VanillaCrafting> h = craftingButtons.get(selectedCraftingButton);
                if (infoType.get() == 1 && LegacyTipManager.hasTip(h.getFocusedResult())) {
                    List<FormattedCharSequence> l = font.split(LegacyTipManager.getTipComponent(h.getFocusedResult()), inventoryPanelWidth - 11);
                    scrollableRenderer.scrolled.max = Math.max(0, l.size() - 7);
                    scrollableRenderer.render(guiGraphics, inventoryPanelX + 5, 105, inventoryPanelWidth - 11, 84, () -> {
                        for (int i1 = 0; i1 < l.size(); i1++)
                            guiGraphics.drawString(font, l.get(i1),  inventoryPanelX + 5, 108 + i1 * 12, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    });

                } else if (infoType.get() == 2) {
                    guiGraphics.drawString(this.font, LegacyComponents.INGREDIENTS, inventoryPanelX + (inventoryPanelWidth - font.width(LegacyComponents.INGREDIENTS)) / 2, 108, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    if (h.getFocusedRecipe() != null) {
                        compactItemStackList.clear();
                        RecipeMenu.handleCompactItemStackList(compactItemStackList, ()-> h.getFocusedRecipe().getOptionalIngredients().stream().map(RecipeIconHolder::getActualItem).iterator());
                        scrollableRenderer.scrolled.max = Math.max(0, compactItemStackList.size() - 4);
                        scrollableRenderer.render(guiGraphics, inventoryPanelX + 2, 122, 152, 60, () -> {
                            for (int i1 = 0; i1 < compactItemStackList.size(); i1++) {
                                ItemStack ing = compactItemStackList.get(i1);
                                ScreenUtil.iconHolderRenderer.itemHolder(inventoryPanelX + 4, 124 + 15 * i1, 14, 14, ing, false, Vec3.ZERO).render(guiGraphics, i, j, 0);
                                guiGraphics.pose().pushPose();
                                guiGraphics.pose().translate(inventoryPanelX + 22, 128 + 15 * i1, 0);
                                Legacy4JClient.applyFontOverrideIf(ScreenUtil.is720p(), LegacyIconHolder.MOJANGLES_11_FONT, b -> {
                                    if (!b) guiGraphics.pose().scale(2 / 3f, 2 / 3f, 2 / 3f);
                                    guiGraphics.drawString(font, ing.getHoverName(), 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                                });
                                guiGraphics.pose().popPose();
                            }
                        });
                    }
                }
            }
        }
        guiGraphics.pose().translate(-leftPos, -topPos, 0);
        getCraftingButtons().forEach(b -> b.render(guiGraphics, i, j, 0));
        if (selectedCraftingButton < getCraftingButtons().size())
            getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
        guiGraphics.pose().translate(leftPos, topPos, 0);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.timesInventoryChanged != this.minecraft.player.getInventory().getTimesChanged()) {
            this.updateStackedContents();
            this.timesInventoryChanged = this.minecraft.player.getInventory().getTimesChanged();
        }
    }

    @Override
    protected void slotClicked(Slot slot, int i, int j, ClickType clickType) {
        allowRecipeDisplay = false;
        super.slotClicked(slot, i, j, clickType);
        updateStackedContents();
    }

    private void updateStackedContents() {
        minecraft = Minecraft.getInstance();
        stackedContents.clear();
        inventory.fillStackedContents(stackedContents);
        menu.fillCraftSlotsStackedContents(stackedContents);
        var collections = Minecraft.getInstance().player.getRecipeBook().getCollection(VANILLA_CATEGORIES[getTabList().selectedTab]);
        allowRecipeDisplay = false;
        //? if <1.21.2 {
        int dim = menu.getGridWidth() * menu.getGridHeight();
        for (int i = 1; i <= dim; i++) {
            if (menu.getSlot(i).hasItem()) {
                break;
            } else if (i == dim) allowRecipeDisplay = true;
        }
        collections.forEach(collection-> collection.canCraft(stackedContents, menu.getGridWidth(), menu.getGridHeight(), Minecraft.getInstance().player.getRecipeBook()));
        //?} else {
        /*var context = SlotDisplayContext.fromLevel(minecraft.level);
        allowRecipeDisplay = menu.getInputGridSlots().stream().noneMatch(Slot::hasItem);
        collections.forEach(collection-> collection.selectRecipes(stackedContents, d -> switch (d) {
            case ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay -> menu.getGridWidth() >= shapedCraftingRecipeDisplay.width() && menu.getGridWidth() >= shapedCraftingRecipeDisplay.height();
            case ShapelessCraftingRecipeDisplay shapelessCraftingRecipeDisplay -> menu.getGridWidth() * menu.getGridHeight() >= shapelessCraftingRecipeDisplay.ingredients().size();
            default -> false;
        }));
        *///?}
        recipesByGroup.clear();


        List<RecipeCollection> searchCollections = null;
        if (!searchBox.getValue().isBlank()){
            ClientPacketListener clientPacketListener = this.minecraft.getConnection();
            if (clientPacketListener != null) {
                searchCollections = /*? if <1.21 {*//*minecraft.getSearchTree(SearchRegistry.RECIPE_COLLECTIONS)*//*?} else {*/clientPacketListener.searchTrees().recipes()/*?}*/.search(searchBox.getValue().toLowerCase(Locale.ROOT));
            }
        }
        for (RecipeCollection collection : collections) {
            if (!collection./*? if <1.21.2 {*/hasFitting/*?} else {*//*hasAnySelected*//*?}*/() || searchCollections != null && !searchCollections.contains(collection)) continue;
            //? if <1.21.2 {
            var selectedRecipes = collection.getRecipes(onlyCraftableRecipes);
            //?} else
            /*var selectedRecipes = collection.getSelectedRecipes(onlyCraftableRecipes ? RecipeCollection.CraftableStatus.CRAFTABLE : RecipeCollection.CraftableStatus.ANY);*/


            if (selectedRecipes.isEmpty()) continue;


            recipesByGroup.add(selectedRecipes.stream().map(e->{
                List<Optional<Ingredient>> ings = new ArrayList<>(Collections.nCopies(menu.getGridWidth() * menu.getGridHeight(), Optional.empty()));
                boolean[] warningSlots = new boolean[menu.getGridWidth() * menu.getGridHeight()];
                //? if <1.21.2 {
                Recipe<?> recipe = /*? if <1.20.2 {*//*e*//*?} else {*/e.value()/*?}*/;
                if (recipe instanceof ShapedRecipe shapedRecipe) {
                    LegacyCraftingMenu.updateShapedIngredients(ings, LegacyCraftingMenu.getRecipeOptionalIngredients(shapedRecipe), menu.getGridWidth(), shapedRecipe.getWidth(), shapedRecipe.getHeight());
                } else for (int i = 0; i < recipe.getIngredients().size(); i++) {
                    ings.set(i, Optional.of(recipe.getIngredients().get(i)));
                }
                //?} else {
                /*List<List<ItemStack>> displays = new ArrayList<>(Collections.nCopies(menu.getGridWidth() * menu.getGridHeight(), Collections.singletonList(ItemStack.EMPTY)));
                switch (e.display()){
                    case ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay ->
                            PlaceRecipeHelper.placeRecipe(menu.getGridWidth(), menu.getGridHeight(), shapedCraftingRecipeDisplay.width(), shapedCraftingRecipeDisplay.height(), shapedCraftingRecipeDisplay.ingredients(), (slotDisplay, ix, jx, k) -> {
                                displays.set(ix, slotDisplay.resolveForStacks(context));
                            });
                    case ShapelessCraftingRecipeDisplay shapelessCraftingRecipeDisplay -> {
                        for (int index = 0; index < shapelessCraftingRecipeDisplay.ingredients().size(); index++) {
                            displays.set(index, shapelessCraftingRecipeDisplay.ingredients().get(index).resolveForStacks(context));
                        }
                    }
                    default -> {}
                }
                e.craftingRequirements().ifPresent(ingredients -> {
                    for (int index = 0; index < ingredients.size(); index++) {
                        ings.set(index, Optional.of(ingredients.get(index)));
                    }
                });
                *///?}

                if (allowRecipeDisplay) {
                    compactItemStackList.clear();
                    RecipeMenu.handleCompactInventoryList(compactItemStackList, inventory, ItemStack.EMPTY);
                    //? if <1.21.2 {
                    LegacyCraftingScreen.canCraft(compactItemStackList, ings, warningSlots);
                    //?} else {
                    /*main : for (int i = 0; i < displays.size(); i++) {
                        List<ItemStack> stacks = displays.get(i);
                        if (stacks.isEmpty() || stacks.get(0).isEmpty()) {
                            warningSlots[i] = false;
                            continue;
                        }
                        for (ItemStack itemStack : compactItemStackList) {
                            if (stacks.stream().anyMatch(item->item.is(itemStack.getItem()))) {
                                warningSlots[i] = false;
                                itemStack.shrink(1);
                                continue main;
                            }
                        }
                        warningSlots[i] = true;
                    }
                    *///?}
                }
                VanillaCrafting crafting = new VanillaCrafting() {
                    @Override
                    public ItemStack getItemFromGrid(int index) {
                        //? if <1.21.2 {
                        return RecipeIconHolder.getActualItem(ings.get(index));
                        //?} else {
                        /*List<ItemStack> items = displays.get(index);
                        if (items.isEmpty()) return ItemStack.EMPTY;
                        return items.get((int) ((Util.getMillis() / 800) % items.size()));
                        *///?}
                    }

                    @Override
                    public boolean canCraft() {
                        return collection.isCraftable(/*? if <1.21.2 {*/e/*?} else {*//*e.id()*//*?}*/);
                    }

                    @Override
                    public void craft() {
                        minecraft.gameMode.handlePlaceRecipe(minecraft.player.containerMenu.containerId, /*? if <1.21.2 {*/e/*?} else {*//*e.id()*//*?}*/, Screen.hasShiftDown());
                    }

                    @Override
                    public boolean isWarning(int index) {
                        return warningSlots[index];
                    }
                };
                List<ItemStack> resultDisplay = /*? if <1.21.2 {*/Collections.singletonList(recipe.getResultItem(minecraft.getConnection().registryAccess()))/*?} else {*//*e.resultItems(context)*//*?}*/;
                RecipeInfo<VanillaCrafting> info = new RecipeInfo<>() {

                    @Override
                    public VanillaCrafting get() {
                        return crafting;
                    }

                    @Override
                    public ResourceLocation getId() {
                        return null;
                    }

                    @Override
                    public List<Optional<Ingredient>> getOptionalIngredients() {
                        return ings;
                    }

                    @Override
                    public ItemStack getResultItem() {
                        return resultDisplay.size() == 1 ? resultDisplay.get(0) : resultDisplay.get((int) ((Util.getMillis() / 800) % resultDisplay.size()));
                    }

                    @Override
                    public Component getName() {
                        return getResultItem().getHoverName();
                    }

                    @Override
                    public Component getDescription() {
                        return null;
                    }
                };
                return info;
            }).toList());
        }
        craftingButtonsOffset.max = Math.max(0, recipesByGroup.size() - 12);
    }

    protected boolean isInventoryActive(){
        return infoType.get() <= 0;
    }

    @Override
    protected void init() {
        imageWidth = 349;
        imageHeight = 215;
        super.init();
        topPos += 18;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0){
                LegacySlotDisplay.override(s,123, 151, new LegacySlotDisplay(){

                    @Override
                    public int getWidth() {
                        return 36;
                    }

                    @Override
                    public ItemStack getItemOverride() {
                        RecipeIconHolder<VanillaCrafting> recipeIconHolder;
                        if (allowRecipeDisplay && (recipeIconHolder = craftingButtons.get(selectedCraftingButton)).isValidIndex()){
                            return recipeIconHolder.getFocusedResult();
                        }
                        return LegacySlotDisplay.super.getItemOverride();
                    }

                    @Override
                    public boolean isWarning() {
                        RecipeIconHolder<VanillaCrafting> recipeIconHolder;
                        if (allowRecipeDisplay && (recipeIconHolder = craftingButtons.get(selectedCraftingButton)).isValidIndex()){
                            return !recipeIconHolder.canCraft();
                        }
                        return LegacySlotDisplay.super.isWarning();
                    }
                });
            } else if (i < (is2x2 ? 5 : 10)) {
                LegacySlotDisplay.override(s,21 + s.getContainerSlot() % menu.getGridWidth() * 23, (is2x2 ? 145 : 133) + s.getContainerSlot() / menu.getGridHeight() * 23, new LegacySlotDisplay(){
                    @Override
                    public int getWidth() {
                        return 23;
                    }

                    @Override
                    public ItemStack getItemOverride() {
                        RecipeIconHolder<VanillaCrafting> recipeIconHolder;
                        if (allowRecipeDisplay && (recipeIconHolder = craftingButtons.get(selectedCraftingButton)).isValidIndex()){
                            return recipeIconHolder.getFocusedRecipe().get().getItemFromGrid(s.getContainerSlot());
                        }
                        return LegacySlotDisplay.super.getItemOverride();
                    }

                    @Override
                    public boolean isWarning() {
                        RecipeIconHolder<VanillaCrafting> recipeIconHolder;
                        if (allowRecipeDisplay && (recipeIconHolder = craftingButtons.get(selectedCraftingButton)).isValidIndex()){
                            return recipeIconHolder.getFocusedRecipe().get().isWarning(s.getContainerSlot());
                        }
                        return LegacySlotDisplay.super.isWarning();
                    }
                });
            } else if (is2x2 && (i < 9 || i == menu.slots.size() - 1)) {
                LegacySlotDisplay.override(s, new LegacySlotDisplay(){
                    @Override
                    public boolean isVisible() {
                        return false;
                    }
                });
            } else if (i < menu.slots.size() - (is2x2 ? 10 : 9)) {
                LegacySlotDisplay.override(s, 186 + (s.getContainerSlot() - 9) % 9 * 16, 133 + (s.getContainerSlot() - 9) / 9 * 16, new LegacySlotDisplay() {
                    @Override
                    public int getWidth() {
                        return 16;
                    }

                    @Override
                    public boolean isVisible() {
                        return isInventoryActive();
                    }
                });
            } else if (i < menu.slots.size() - (is2x2 ? 1 : 0)) {
                LegacySlotDisplay.override(s, 186 + s.getContainerSlot() * 16,186, new LegacySlotDisplay() {
                    @Override
                    public int getWidth() {
                        return 16;
                    }

                    @Override
                    public boolean isVisible() {
                        return isInventoryActive();
                    }
                });
            }
        }
        if (searchMode){
            searchBox.setPosition(leftPos + (imageWidth - searchBox.getWidth()) / 2, topPos + 11);
            addRenderableWidget(searchBox);
        }
        if (selectedCraftingButton < getCraftingButtons().size() && !searchMode)
            setFocused(getCraftingButtons().get(selectedCraftingButton));
        getCraftingButtons().forEach(b -> {
            b.setPos(leftPos + accessor.getInteger("craftingButtonsX", 13) + getCraftingButtons().indexOf(b) * 27, topPos + 38);
            addWidget(b);
        });
        addWidget(getTabList());
        getTabList().init(leftPos, topPos - 37, imageWidth, (t, i) -> {
            int index = getTabList().tabButtons.indexOf(t);
            t.type = index == 0 ? 0 : index >= getMaxTabCount() ? 2 : 1;
            t.setWidth(accessor.getInteger("tabListButtonWidth", 71));
            t.offset = (t1) -> new Vec3(accessor.getDouble("tabListXOffset", -1.5) * getTabList().tabButtons.indexOf(t), t1.selected ? 0 : accessor.getDouble("tabListSelectedYOffset", 4.5), 0);
        });
    }

    public TabList getTabList() {
        return craftingTabList;
    }

    protected int getMaxTabCount(){
        return accessor.getInteger("maxTabCount",4);
    }

    protected void addCraftingButtons() {
        for (int i = 0; i < 12; i++) {
            int index = i;

            RecipeIconHolder<VanillaCrafting> h;
            getCraftingButtons().add(h = new RecipeIconHolder<>(leftPos + 13 + i * 27, topPos + 38) {
                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }

                protected boolean canCraft(RecipeInfo<VanillaCrafting> rcp) {
                    return rcp == null || rcp.get().canCraft();
                }

                protected List<RecipeInfo<VanillaCrafting>> getRecipes() {
                    return recipesByGroup.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : recipesByGroup.get(craftingButtonsOffset.get() + index);
                }

                @Override
                protected void toggleCraftableRecipes() {
                    if (hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed) return;
                    onlyCraftableRecipes = !onlyCraftableRecipes;
                    updateStackedContents();
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if (controlCyclicNavigation(i, index, craftingButtons, craftingButtonsOffset, scrollRenderer, MixedCraftingScreen.this))
                        return true;
                    if (i == InputConstants.KEY_X) {
                        infoType.add(1, true);
                        ScreenUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
                        return true;
                    }
                    return super.keyPressed(i, j, k);
                }

                protected void updateRecipeDisplay(RecipeInfo<VanillaCrafting> rcp) {
                    scrollableRenderer.scrolled.set(0);
                }

                @Override
                public void craft() {
                    ScreenUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP, 1.0f);
                    getFocusedRecipe().get().craft();
                    //? if <1.21.2 {
                    slotClicked(menu.getSlot(menu.getResultSlotIndex()), menu.getResultSlotIndex(), 0, ClickType.QUICK_MOVE);
                    //?} else
                    /*slotClicked(menu.getResultSlot(), menu.getResultSlot().index, 0, ClickType.QUICK_MOVE);*/
                }
            });
            h.offset = LegacyCraftingMenu.DEFAULT_INVENTORY_OFFSET;
        }
    }

    public interface VanillaCrafting {
        ItemStack getItemFromGrid(int index);
        boolean canCraft();
        void craft();
        boolean isWarning(int index);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g){
        //? if >=1.21.2 {
        /*if (this.getChildAt(d, e).filter((guiEventListener) -> guiEventListener.mouseScrolled(d, e, f, g)).isPresent())
            return true;
        *///?}
        if (super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g)) return true;
        if (scrollableRenderer.mouseScrolled(g)) return true;
        int scroll = (int) Math.signum(g);
        if (((craftingButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && craftingButtonsOffset.max > 0)) && craftingButtonsOffset.add(scroll, false) != 0) {
            repositionElements();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (searchBox.isFocused() && searchMode && !searchBox.isMouseOver(d,e)) disableSearchMode();
        return super.mouseClicked(d, e, i);
    }

    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics,int i, int j, float f){
        renderBg(guiGraphics, f, i, j);
    }
    //?} else {
    /*@Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }
    *///?}

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j){
        getTabList().render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(accessor.getElementValue("imageSprite", LegacySprites.SMALL_PANEL, ResourceLocation.class), leftPos, topPos, imageWidth, imageHeight);
        int panelWidth = accessor.getInteger("craftingGridPanelWidth", 163);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 9, topPos + 103, panelWidth, 105);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + accessor.getInteger("inventoryPanelX", 176), topPos + accessor.getInteger("inventoryPanelY", 103), accessor.getInteger("inventoryPanelWidth", 163), 105);
        int contentsWidth = (is2x2 ? 2 : 3) * 23 + 69;
        int xDiff = leftPos + 9 + (panelWidth - contentsWidth) / 2;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(SMALL_ARROW, xDiff + (is2x2 ? 2 : 3) * 23 + 8, topPos + 161, 16, 13);
        if (craftingButtonsOffset.get() > 0)
            scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
        if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
            scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + imageWidth - 11, topPos + 45);
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
    public boolean keyPressed(int i, int j, int k){
        if (searchMode && (i == InputConstants.KEY_UP || i == InputConstants.KEY_DOWN)){
            disableSearchMode();
            return true;
        }
        if (getTabList().controlTab(i)) return true;
        if (i != InputConstants.KEY_ESCAPE && searchBox.isFocused()) return searchBox.keyPressed(i,j,k);
        return super.keyPressed(i, j, k);
    }

    @Override
    public boolean keyReleased(int i, int j, int k) {
        if (!searchBox.isFocused() && !searchMode && i == InputConstants.KEY_O && (hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed)){
            enableSearchMode(false);
            return true;
        }
        return super.keyReleased(i, j, k);
    }

    public List<RecipeIconHolder<VanillaCrafting>> getCraftingButtons() {
        return craftingButtons;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f){
        //? if >1.20.1 {
        renderBackground(guiGraphics, i, j, f);
        //?}
        super.render(guiGraphics, i, j, f);
        int panelWidth = accessor.getInteger("craftingGridPanelWidth", 163);
        ItemStack resultStack = craftingButtons.get(selectedCraftingButton).getFocusedResult();
        if (!resultStack.isEmpty() && allowRecipeDisplay) {
            Component resultName = getCraftingButtons().get(selectedCraftingButton).getFocusedRecipe().getName();
            Component description = getCraftingButtons().get(selectedCraftingButton).getFocusedRecipe().getDescription();
            int titleY = accessor.getInteger("bottomPanelTitleY", 114) - (description == null ? 0 : 6);
            ScreenUtil.renderScrollingString(guiGraphics, font, resultName, leftPos + 11 + Math.max(panelWidth - font.width(resultName), 0) / 2, topPos + titleY, leftPos + 7 + panelWidth, topPos + titleY + 11, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            if (description != null)
                ScreenUtil.renderScrollingString(guiGraphics, font, description.copy().setStyle(Style.EMPTY), leftPos + 11 + Math.max(panelWidth - font.width(description), 0) / 2, topPos + titleY + 12, leftPos + 7 + panelWidth, topPos + titleY + 23, CommonColor.INVENTORY_GRAY_TEXT.get(), false);

        }

        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }
}
