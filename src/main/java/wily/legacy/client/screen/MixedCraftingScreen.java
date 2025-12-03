package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;


import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
//? if >=1.21.2 {
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.recipebook.PlaceRecipeHelper;
//?} else {
/*import net.minecraft.client.RecipeBookCategories;
 *///?}
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
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;


import net.minecraft.world.item.crafting.*;


import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;


import java.util.*;

import static wily.legacy.client.screen.ControlTooltip.*;


public class MixedCraftingScreen<T extends AbstractCraftingMenu> extends RecipesScreen<T, RecipeIconHolder<MixedCraftingScreen.VanillaCrafting>> implements TabList.Access {
    public static final ResourceLocation STRUCTURES_TAB = Legacy4J.createModLocation("structures");
    public static final ResourceLocation MECHANISMS_TAB = Legacy4J.createModLocation("mechanisms");
    public static final ResourceLocation TOOLS_TAB = Legacy4J.createModLocation("tools");
    public static final ResourceLocation MISC_TAB = Legacy4J.createModLocation("misc");
    public static final ResourceLocation SEARCH_TAB = Legacy4J.createModLocation("search");
    public static final ExtendedRecipeBookCategory[] VANILLA_CATEGORIES = new ExtendedRecipeBookCategory[]{RecipeBookCategories.CRAFTING_BUILDING_BLOCKS, RecipeBookCategories.CRAFTING_REDSTONE, RecipeBookCategories.CRAFTING_EQUIPMENT, RecipeBookCategories.CRAFTING_MISC, SearchRecipeBookCategory.CRAFTING};
    protected final List<ItemStack> compactItemStackList = new ArrayList<>();
    protected final StackedItemContents stackedContents = new StackedItemContents();
    protected final ScrollableRenderer scrollableRenderer = new ScrollableRenderer();
    protected final EditBox searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200, 20, LegacyComponents.SEARCH_ITEMS);
    private final boolean is2x2;
    protected Stocker.Sizeable infoType = new Stocker.Sizeable(0, 2);
    protected List<List<RecipeInfo<VanillaCrafting>>> recipesByGroup = new ArrayList<>();
    protected boolean allowRecipeDisplay = false;
    protected boolean searchMode = false;
    private boolean onlyCraftableRecipes = LegacyOptions.defaultShowCraftableRecipes.get();
    protected final TabList craftingTabList = new TabList(accessor);

    public MixedCraftingScreen(T abstractContainerMenu, Inventory inventory, Component component, boolean is2x2) {
        super(abstractContainerMenu, inventory, component);
        this.is2x2 = is2x2;
        searchBox.setResponder(s -> updateRecipesAndResetTimer());
        searchBox.setMaxLength(50);
        for (LegacyTabDisplay tab : Legacy4JClient.mixedCraftingTabs.map().values()) {
            boolean searchTab = tab.is(SEARCH_TAB);
            craftingTabList.add(LegacyTabButton.Type.LEFT, tab.icon(), tab.nameOrEmpty(), t -> {
                if (searchTab)
                    enableSearchMode(true);
                else
                    resetElements();
            });
        }
        resetElements(false, false);
        accessor.addStatic(UIDefinition.createBeforeInit(a -> accessor.putStaticElement("is2x2", is2x2)));
    }

    public static MixedCraftingScreen<CraftingMenu> craftingScreen(CraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        return new MixedCraftingScreen<>(abstractContainerMenu, inventory, component, false);
    }

    public static MixedCraftingScreen<InventoryMenu> playerCraftingScreen(Player player) {
        return new MixedCraftingScreen<>(player.inventoryMenu, player.getInventory(), LegacyCraftingMenu.CRAFTING_TITLE, true);
    }

    public static boolean isValidTab(LegacyTabDisplay display) {
        return display.is(STRUCTURES_TAB) || display.is(MECHANISMS_TAB) || display.is(TOOLS_TAB) || display.is(MISC_TAB) || display.is(SEARCH_TAB);
    }

    public void enableSearchMode(boolean clearSearch) {
        searchMode = true;
        resetElements(true, clearSearch);
        setFocused(searchBox);
    }

    public void disableSearchMode() {
        searchMode = false;
        resetElements(true, false);
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.
                add(EXTRA::get, () -> LegacyComponents.INFO).
                add(OPTION::get, () -> onlyCraftableRecipes ? LegacyComponents.ALL_RECIPES : LegacyComponents.SHOW_CRAFTABLE_RECIPES).
                add(() -> searchMode ? VERTICAL_NAVIGATION.get() : CompoundComponentIcon.of(ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_LSHIFT) : ControllerBinding.LEFT_STICK_BUTTON.getIcon(), PLUS_ICON, OPTION.get()), () -> searchMode ? LegacyComponents.EXIT_SEARCH_MODE : LegacyComponents.SEARCH_MODE).
                add(CONTROL_TAB::get, () -> LegacyComponents.GROUP);
    }

    public void resetElements() {
        searchMode = false;
        resetElements(true, true);
    }

    public void resetElements(boolean reposition, boolean resetCrafting) {
        if (!resetCrafting || searchBox.getValue().isEmpty()) updateRecipesAndResetTimer();
        else searchBox.setValue("");
        selectedRecipeButton = 0;
        if (resetCrafting) {
            infoType.set(0);
            recipeButtonsOffset.set(0);
        }
        if (reposition) repositionElements();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        if (guiEventListener instanceof TabList) return;
        super.setFocused(guiEventListener);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        if (!searchMode) {
            Component title = getTabList() == craftingTabList ? getTabList().tabButtons.get(getTabList().getIndex()).getMessage() : CommonComponents.EMPTY;
            LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(this.font, title, (imageWidth - font.width(title)) / 2, accessor.getInteger("title.y", 17), CommonColor.INVENTORY_GRAY_TEXT.get(), false));
        }
        int inventoryPanelX = accessor.getInteger("inventoryPanel.x", 176);
        int bottomPanelY = accessor.getInteger("bottomPanel.y", 103);
        int inventoryPanelWidth = accessor.getInteger("inventoryPanel.width", 163);
        int bottomPanelHeight = accessor.getInteger("bottomPanel.height", 105);
        if (infoType.get() <= 0)
            LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(this.font, this.playerInventoryTitle, inventoryPanelX + (inventoryPanelWidth - font.width(playerInventoryTitle)) / 2, bottomPanelY + accessor.getInteger("inventoryTitle.y", 11), CommonColor.INVENTORY_GRAY_TEXT.get(), false));
        else {
            if (selectedRecipeButton < getRecipeButtons().size()) {
                RecipeIconHolder<VanillaCrafting> h = recipeButtons.get(selectedRecipeButton);
                if (infoType.get() == 1 && LegacyTipManager.hasTip(h.getFocusedResult())) {
                    LegacyFontUtil.applySDFont(b -> {
                        List<FormattedCharSequence> l = font.split(LegacyTipManager.getTipComponent(h.getFocusedResult()), inventoryPanelWidth - 11);
                        int lineSpacing = (b ? 8 : 12);
                        scrollableRenderer.lineHeight = lineSpacing;
                        int lineAmount = (bottomPanelHeight - 21) / lineSpacing;
                        scrollableRenderer.scrolled.max = Math.max(0, l.size() - lineAmount);
                        scrollableRenderer.render(guiGraphics, inventoryPanelX + 5, bottomPanelY + 2, inventoryPanelWidth - 11, lineAmount * lineSpacing + 2, () -> {
                            for (int i1 = 0; i1 < l.size(); i1++)
                                guiGraphics.drawString(font, l.get(i1), inventoryPanelX + 5, bottomPanelY + 5 + i1 * (b ? 8 : 12), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                        });
                    });
                } else if (infoType.get() == 2) {
                    LegacyFontUtil.applySDFont(b -> guiGraphics.drawString(this.font, LegacyComponents.INGREDIENTS, inventoryPanelX + (inventoryPanelWidth - font.width(LegacyComponents.INGREDIENTS)) / 2, bottomPanelY + accessor.getInteger("ingredientsTitle.y", 5), CommonColor.INVENTORY_GRAY_TEXT.get(), false));
                    if (h.getFocusedRecipe() != null) {
                        int ingredientSize = accessor.getInteger("ingredientSlot.size", 14);
                        int ingredientX = inventoryPanelX + accessor.getInteger("ingredientSlot.x", 4);
                        int ingredientY = bottomPanelY + accessor.getInteger("ingredientSlot.y", 21);
                        float textScale = LegacyOptions.getUIMode().isFHD() ? 2 / 3f : LegacyOptions.getUIMode().isSD() ? 1 / 2f : 1.0f;

                        compactItemStackList.clear();
                        RecipeMenu.handleCompactItemStackList(compactItemStackList, () -> h.getFocusedRecipe().getOptionalIngredients().stream().map(RecipeIconHolder::getActualItem).iterator());
                        scrollableRenderer.scrolled.max = Math.max(0, compactItemStackList.size() - 4);
                        scrollableRenderer.lineHeight = ingredientSize + 1;
                        scrollableRenderer.render(guiGraphics, inventoryPanelX + 2, ingredientY - 2, inventoryPanelWidth - 11, (ingredientSize + 1) * 4 + 1, () -> {
                            for (int i1 = 0; i1 < compactItemStackList.size(); i1++) {
                                ItemStack ing = compactItemStackList.get(i1);
                                LegacyRenderUtil.iconHolderRenderer.itemHolder(ingredientX, ingredientY + (ingredientSize + 1) * i1, ingredientSize, ingredientSize, ing, false, Vec2.ZERO).render(guiGraphics, i, j, 0);
                                guiGraphics.pose().pushMatrix();
                                guiGraphics.pose().translate(ingredientX + ingredientSize + accessor.getInteger("ingredientText.x", 4), ingredientY + ingredientSize / 2 + (ingredientSize + 1) * i1);
                                LegacyFontUtil.applyFontOverrideIf(LegacyOptions.getUIMode().isHD(), LegacyFontUtil.MOJANGLES_11_FONT, b -> {
                                    guiGraphics.pose().scale(textScale);
                                    guiGraphics.pose().translate(0, -3);
                                    LegacyRenderUtil.renderScrollingString(guiGraphics, font, ing.getHoverName(), 0,-2, Math.round((inventoryPanelWidth - 22 - 2) / textScale), 7, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                                });
                                guiGraphics.pose().popMatrix();
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    protected void slotClicked(Slot slot, int i, int j, ClickType clickType) {
        allowRecipeDisplay = false;
        super.slotClicked(slot, i, j, clickType);
        updateRecipesAndResetTimer();
    }

    @Override
    protected void updateRecipes() {
        minecraft = Minecraft.getInstance();
        stackedContents.clear();
        inventory.fillStackedContents(stackedContents);
        menu.fillCraftSlotsStackedContents(stackedContents);
        var collections = Minecraft.getInstance().player.getRecipeBook().getCollection(VANILLA_CATEGORIES[getTabList().getIndex()]);
        allowRecipeDisplay = false;
        var context = SlotDisplayContext.fromLevel(minecraft.level);
        allowRecipeDisplay = menu.getInputGridSlots().stream().noneMatch(Slot::hasItem);
        collections.forEach(collection -> collection.selectRecipes(stackedContents, d -> switch (d) {
            case ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay ->
                    menu.getGridWidth() >= shapedCraftingRecipeDisplay.width() && menu.getGridWidth() >= shapedCraftingRecipeDisplay.height();
            case ShapelessCraftingRecipeDisplay shapelessCraftingRecipeDisplay ->
                    menu.getGridWidth() * menu.getGridHeight() >= shapelessCraftingRecipeDisplay.ingredients().size();
            default -> false;
        }));
        recipesByGroup.clear();


        List<RecipeCollection> searchCollections = null;
        if (!searchBox.getValue().isBlank()) {
            ClientPacketListener clientPacketListener = this.minecraft.getConnection();
            if (clientPacketListener != null) {
                searchCollections = clientPacketListener.searchTrees().recipes().search(searchBox.getValue().toLowerCase(Locale.ROOT));
            }
        }
        for (RecipeCollection collection : collections) {
            if (!collection.hasAnySelected() || searchCollections != null && !searchCollections.contains(collection))
                continue;
            var selectedRecipes = collection.getSelectedRecipes(onlyCraftableRecipes ? RecipeCollection.CraftableStatus.CRAFTABLE : RecipeCollection.CraftableStatus.ANY);


            if (selectedRecipes.isEmpty()) continue;


            recipesByGroup.add(selectedRecipes.stream().map(e -> {
                List<Optional<Ingredient>> ings = new ArrayList<>(Collections.nCopies(menu.getGridWidth() * menu.getGridHeight(), Optional.empty()));
                boolean[] warningSlots = new boolean[menu.getGridWidth() * menu.getGridHeight()];
                List<List<ItemStack>> displays = new ArrayList<>(Collections.nCopies(menu.getGridWidth() * menu.getGridHeight(), Collections.singletonList(ItemStack.EMPTY)));
                switch (e.display()) {
                    case ShapedCraftingRecipeDisplay shapedCraftingRecipeDisplay ->
                            PlaceRecipeHelper.placeRecipe(menu.getGridWidth(), menu.getGridHeight(), shapedCraftingRecipeDisplay.width(), shapedCraftingRecipeDisplay.height(), shapedCraftingRecipeDisplay.ingredients(), (slotDisplay, ix, jx, k) -> {
                                displays.set(ix, slotDisplay.resolveForStacks(context));
                            });
                    case ShapelessCraftingRecipeDisplay shapelessCraftingRecipeDisplay -> {
                        for (int index = 0; index < shapelessCraftingRecipeDisplay.ingredients().size(); index++) {
                            displays.set(index, shapelessCraftingRecipeDisplay.ingredients().get(index).resolveForStacks(context));
                        }
                    }
                    default -> {
                    }
                }
                e.craftingRequirements().ifPresent(ingredients -> {
                    for (int index = 0; index < ingredients.size(); index++) {
                        ings.set(index, Optional.of(ingredients.get(index)));
                    }
                });

                if (allowRecipeDisplay) {
                    compactItemStackList.clear();
                    RecipeMenu.handleCompactInventoryList(compactItemStackList, inventory, ItemStack.EMPTY);
                    main:
                    for (int i = 0; i < displays.size(); i++) {
                        List<ItemStack> stacks = displays.get(i);
                        if (stacks.isEmpty() || stacks.get(0).isEmpty()) {
                            warningSlots[i] = false;
                            continue;
                        }
                        for (ItemStack itemStack : compactItemStackList) {
                            if (stacks.stream().anyMatch(item -> item.is(itemStack.getItem()))) {
                                warningSlots[i] = false;
                                itemStack.shrink(1);
                                continue main;
                            }
                        }
                        warningSlots[i] = true;
                    }
                }
                VanillaCrafting crafting = new VanillaCrafting() {
                    @Override
                    public ItemStack getItemFromGrid(int index) {
                        List<ItemStack> items = displays.get(index);
                        if (items.isEmpty()) return ItemStack.EMPTY;
                        return items.get((int) ((Util.getMillis() / 800) % items.size()));
                    }

                    @Override
                    public boolean canCraft() {
                        return collection.isCraftable(e.id());
                    }

                    @Override
                    public void craft(InputWithModifiers input) {
                        minecraft.gameMode.handlePlaceRecipe(minecraft.player.containerMenu.containerId, e.id(), input.hasShiftDown());
                    }

                    @Override
                    public boolean isWarning(int index) {
                        return warningSlots[index];
                    }
                };
                List<ItemStack> resultDisplay = e.resultItems(context);
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
        recipeButtonsOffset.max = Math.max(0, recipesByGroup.size() - 12);
    }

    protected boolean isInventoryActive() {
        return infoType.get() <= 0;
    }

    @Override
    public int getTabYOffset() {
        return 18;
    }

    @Override
    protected void init() {
        imageWidth = 349;
        imageHeight = 215;
        super.init();
        topPos += getTabYOffset();
        int invSlotSize = accessor.getInteger("inventorySlot.size", 16);
        int invSlotX = accessor.getInteger("inventory.x", 186);
        int invSlotY = accessor.getInteger("inventory.y", 133);
        int quickSelectY = accessor.getInteger("quickSelect.y", 186);
        int slotSize = accessor.getInteger("craftingGridSlot.size", 23);
        int craftingGridPanelX = accessor.getInteger("craftingGridPanel.x", 9);
        int bottomPanelY = accessor.getInteger("bottomPanel.y", 103);
        int craftingGridSlotX = accessor.getInteger("craftingGridSlot.x", is2x2 ? 24 : 12);
        int craftingGridSlotY = accessor.getInteger("craftingGridSlot.y", 30);
        int resultSlotSize = accessor.getInteger("craftingResultSlot.size", 36);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (i == 0) {
                LegacySlotDisplay.override(s, craftingGridPanelX + accessor.getInteger("resultSlot.x", 104) + (is2x2 ? 0 : slotSize / 2), bottomPanelY + accessor.getInteger("resultSlot.y", 48), new LegacySlotDisplay() {

                    @Override
                    public int getWidth() {
                        return resultSlotSize;
                    }

                    @Override
                    public ItemStack getItemOverride() {
                        RecipeIconHolder<VanillaCrafting> recipeIconHolder;
                        if (allowRecipeDisplay && (recipeIconHolder = recipeButtons.get(selectedRecipeButton)).isValidIndex()) {
                            return recipeIconHolder.getFocusedResult();
                        }
                        return LegacySlotDisplay.super.getItemOverride();
                    }

                    @Override
                    public boolean isWarning() {
                        RecipeIconHolder<VanillaCrafting> recipeIconHolder;
                        if (allowRecipeDisplay && (recipeIconHolder = recipeButtons.get(selectedRecipeButton)).isValidIndex()) {
                            return !recipeIconHolder.canCraft();
                        }
                        return LegacySlotDisplay.super.isWarning();
                    }
                });
            } else if (i < (is2x2 ? 5 : 10)) {
                LegacySlotDisplay.override(s, craftingGridPanelX + craftingGridSlotX + s.getContainerSlot() % menu.getGridWidth() * slotSize, bottomPanelY + craftingGridSlotY + (is2x2 ? slotSize / 2 : 0) + s.getContainerSlot() / menu.getGridHeight() * slotSize, new LegacySlotDisplay() {
                    @Override
                    public int getWidth() {
                        return slotSize;
                    }

                    @Override
                    public ItemStack getItemOverride() {
                        RecipeIconHolder<VanillaCrafting> recipeIconHolder;
                        if (allowRecipeDisplay && (recipeIconHolder = recipeButtons.get(selectedRecipeButton)).isValidIndex()) {
                            return recipeIconHolder.getFocusedRecipe().get().getItemFromGrid(s.getContainerSlot());
                        }
                        return LegacySlotDisplay.super.getItemOverride();
                    }

                    @Override
                    public boolean isWarning() {
                        RecipeIconHolder<VanillaCrafting> recipeIconHolder;
                        if (allowRecipeDisplay && (recipeIconHolder = recipeButtons.get(selectedRecipeButton)).isValidIndex()) {
                            return recipeIconHolder.getFocusedRecipe().get().isWarning(s.getContainerSlot());
                        }
                        return LegacySlotDisplay.super.isWarning();
                    }
                });
            } else if (is2x2 && (i < 9 || i == menu.slots.size() - 1)) {
                LegacySlotDisplay.override(s, new LegacySlotDisplay() {
                    @Override
                    public boolean isVisible() {
                        return false;
                    }
                });
            } else if (i < menu.slots.size() - (is2x2 ? 10 : 9)) {
                LegacySlotDisplay.override(s, invSlotX + (s.getContainerSlot() - 9) % 9 * invSlotSize, invSlotY + (s.getContainerSlot() - 9) / 9 * invSlotSize, new LegacySlotDisplay() {
                    @Override
                    public int getWidth() {
                        return invSlotSize;
                    }

                    @Override
                    public boolean isVisible() {
                        return isInventoryActive();
                    }
                });
            } else if (i < menu.slots.size() - (is2x2 ? 1 : 0)) {
                LegacySlotDisplay.override(s, invSlotX + s.getContainerSlot() * invSlotSize, quickSelectY, new LegacySlotDisplay() {
                    @Override
                    public int getWidth() {
                        return invSlotSize;
                    }

                    @Override
                    public boolean isVisible() {
                        return isInventoryActive();
                    }
                });
            }
        }
        if (searchMode) {
            searchBox.setWidth(200);
            searchBox.setHeight(20);
            searchBox.setPosition(leftPos + (imageWidth - searchBox.getWidth()) / 2, topPos + 11);
            addRenderableWidget(accessor.putWidget("searchBox", searchBox));
        }
        if (selectedRecipeButton < getRecipeButtons().size() && !searchMode)
            setFocused(getRecipeButtons().get(selectedRecipeButton));

        int craftingButtonsX = accessor.getInteger("craftingButtons.x", 13);
        int craftingButtonsY = accessor.getInteger("craftingButtons.y", 38);
        int craftingButtonsSize = accessor.getInteger("craftingButtons.size", 27);
        getRecipeButtons().forEach(b -> {
            b.width = b.height = craftingButtonsSize;
            b.setPos(leftPos + craftingButtonsX + recipeButtons.indexOf(b) * b.width, topPos + craftingButtonsY);
            addWidget(b);
        });
        addWidget(getTabList());
        getTabList().init(leftPos, topPos - 37, imageWidth, 43, (t, i) -> {
            int index = getTabList().tabButtons.indexOf(t);
            t.type = LegacyTabButton.Type.bySize(index, getMaxTabCount());
            t.setWidth(accessor.getInteger("tabList.buttonWidth", 71));
            t.offset = (t1) -> new Vec2((LegacyRenderUtil.hasHorizontalArtifacts() && index % 2 != 0 ? 0.0125f : 0.0f) + accessor.getFloat("tabList.buttonOffset.x", -1.5f) * getTabList().tabButtons.indexOf(t), t1.selected ? 0 : accessor.getFloat("tabList.selectedOffset.y", 4.4f));
        });
    }

    public TabList getTabList() {
        return craftingTabList;
    }

    protected int getMaxTabCount() {
        return accessor.getInteger("maxTabCount", 5);
    }

    @Override
    protected RecipeIconHolder<VanillaCrafting> createRecipeButton(int index) {
        RecipeIconHolder<VanillaCrafting> h = new RecipeIconHolder<>(leftPos + 13 + index * 27, topPos + 38) {
            @Override
            public void render(GuiGraphics graphics, int i, int j, float f) {
                if (isFocused()) selectedRecipeButton = index;
                super.render(graphics, i, j, f);
            }

            protected boolean canCraft(RecipeInfo<VanillaCrafting> rcp) {
                return rcp == null || rcp.get().canCraft();
            }

            protected List<RecipeInfo<VanillaCrafting>> getRecipes() {
                return recipesByGroup.size() <= recipeButtonsOffset.get() + index ? Collections.emptyList() : recipesByGroup.get(recipeButtonsOffset.get() + index);
            }

            @Override
            public LegacyScrollRenderer getScrollRenderer() {
                return scrollRenderer;
            }

            @Override
            protected void toggleCraftableRecipes(InputWithModifiers input) {
                if (input.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed) return;
                onlyCraftableRecipes = !onlyCraftableRecipes;
                updateRecipes();
            }

            @Override
            public boolean keyPressed(KeyEvent keyEvent) {
                if (controlCyclicNavigation(keyEvent.key(), index, recipeButtons, recipeButtonsOffset, scrollRenderer, MixedCraftingScreen.this))
                    return true;
                if (keyEvent.key() == InputConstants.KEY_X) {
                    infoType.add(1, true);
                    LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
                    return true;
                }
                return super.keyPressed(keyEvent);
            }

            protected void updateRecipeDisplay(RecipeInfo<VanillaCrafting> rcp) {
                scrollableRenderer.resetScrolled();
            }

            @Override
            public void craft(InputWithModifiers input) {
                LegacySoundUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP, 1.0f);
                getFocusedRecipe().get().craft(input);
                //? if <1.21.2 {
                /*slotClicked(menu.getSlot(menu.getResultSlotIndex()), menu.getResultSlotIndex(), 0, ClickType.QUICK_MOVE);
                 *///?} else
                slotClicked(menu.getResultSlot(), menu.getResultSlot().index, 0, ClickType.QUICK_MOVE);
            }
        };
        h.offset = LegacyCraftingMenu.DEFAULT_INVENTORY_OFFSET;
        return h;
    }

    @Override
    public int getMaxRecipeButtons() {
        return accessor.getInteger("maxCraftingButtonsCount", 12);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (super.mouseScrolled(d, e, f, g)) return true;
        return scrollableRenderer.mouseScrolled(g);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (searchBox.isFocused() && searchMode && !searchBox.isMouseOver(event.x(), event.y())) disableSearchMode();
        return super.mouseClicked(event, bl);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        getTabList().render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(accessor.getResourceLocation("imageSprite", LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        getTabList().renderSelected(guiGraphics, i, j, f);
        int bottomPanelHeight = accessor.getInteger("bottomPanel.height", 105);
        int panelWidth = accessor.getInteger("craftingGridPanel.width", 163);
        int bottomPanelY = accessor.getInteger("bottomPanel.y", 103);
        int craftingGridPanelX = accessor.getInteger("craftingGridPanel.x", 9);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + craftingGridPanelX, topPos + bottomPanelY, panelWidth, bottomPanelHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + accessor.getInteger("inventoryPanel.x", 176), topPos + bottomPanelY, accessor.getInteger("inventoryPanel.width", 163), bottomPanelHeight);
        int slotSize = accessor.getInteger("craftingGridSlot.size", 23);
        int xDiff = leftPos + craftingGridPanelX;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_ARROW, xDiff + accessor.getInteger("craftingArrow.x", 77) + (is2x2 ? 0 : slotSize / 2), topPos + bottomPanelY + accessor.getInteger("craftingArrow.y", 57), 16, 14);
        renderRecipesScroll(guiGraphics, 5, 45);

        ItemStack resultStack = recipeButtons.get(selectedRecipeButton).getFocusedResult();
        if (!resultStack.isEmpty() && allowRecipeDisplay) {
            Component resultName = recipeButtons.get(selectedRecipeButton).getFocusedRecipe().getName();
            Component description = recipeButtons.get(selectedRecipeButton).getFocusedRecipe().getDescription();
            int titleY = bottomPanelY + accessor.getInteger("craftingTitle.y", 11) - (description == null ? 0 : 6);
            LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, resultName, xDiff + 2 + Math.max(panelWidth - font.width(resultName), 0) / 2, topPos + titleY, xDiff + panelWidth - 2, topPos + titleY + 11, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
            if (description != null)
                LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, description.copy().setStyle(Style.EMPTY), xDiff + 2 + Math.max(panelWidth - font.width(description), 0) / 2, topPos + titleY + 12, xDiff + panelWidth - 2, topPos + titleY + 23, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (searchMode && (keyEvent.key() == InputConstants.KEY_UP || keyEvent.key() == InputConstants.KEY_DOWN)) {
            disableSearchMode();
            return true;
        }
        if (getTabList().controlTab(keyEvent.key())) return true;
        if (keyEvent.key() != InputConstants.KEY_ESCAPE && searchBox.isFocused()) return searchBox.keyPressed(keyEvent);
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        if (!searchBox.isFocused() && !searchMode && keyEvent.key() == InputConstants.KEY_O && (keyEvent.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed)) {
            enableSearchMode(false);
            return true;
        }
        return super.keyReleased(keyEvent);
    }

    public interface VanillaCrafting {
        ItemStack getItemFromGrid(int index);

        boolean canCraft();

        void craft(InputWithModifiers input);

        boolean isWarning(int index);
    }
}
