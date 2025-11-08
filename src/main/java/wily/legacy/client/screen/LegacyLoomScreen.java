package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BannerPattern;
//? if >=1.20.5 {
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.StackIngredient;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.PagedList;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.RecipeInfo;
import wily.legacy.client.LoomTabListing;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.network.ServerMenuCraftPayload;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.*;
import java.util.stream.Collectors;

import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.LegacyCraftingScreen.clearIngredients;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class LegacyLoomScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event, ControlTooltip.Event, TabList.Access {
    protected final List<ItemStack> compactInventoryList = new ArrayList<>();
    protected final List<Optional<Ingredient>> ingredientsGrid = new ArrayList<>(Collections.nCopies(9, Optional.empty()));
    protected final List<Optional<Ingredient>> selectedIngredients = new ArrayList<>();
    protected final UIAccessor accessor = UIAccessor.of(this);
    protected final ScrollableRenderer scrollableRenderer = new ScrollableRenderer();
    protected final List<LoomTabListing> loomTabListings = List.copyOf(Legacy4JClient.loomListingManager.map().values());
    protected final List<RecipeIconHolder<BannerRecipe>> craftingButtons = new ArrayList<>();
    protected final Stocker.Sizeable page = new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset = new Stocker.Sizeable(0);
    protected final TabList craftingTabList = new TabList(accessor, new PagedList<>(page, this::getMaxTabCount));
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private final Inventory inventory;
    private final boolean[] warningSlots = new boolean[9];
    protected final List<CustomRecipeIconHolder> selectBannerButton = Collections.singletonList(new CustomRecipeIconHolder() {
        @Override
        public @Nullable Component getAction(Context context) {
            return context.actionOfContext(KeyContext.class, c -> c.key() == InputConstants.KEY_O && isFocused() && hasItem() ? LegacyComponents.SELECT : null);
        }

        public Component getDisplayName() {
            return LegacyComponents.SELECT_BANNER_TAB;
        }

        public ItemStack nextItem() {
            return nextItem(inventory, i -> i.getItem() instanceof BannerItem && LegacyItemUtil.getPatternsCount(i) < 6);
        }

        public ItemStack previousItem() {
            return previousItem(inventory, i -> i.getItem() instanceof BannerItem && LegacyItemUtil.getPatternsCount(i) < 6);
        }

        public boolean applyNextItemIfAbsent() {
            return true;
        }

        public boolean canCraft() {
            return true;
        }

        @Override
        public void craft(InputWithModifiers input) {
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
        public boolean keyPressed(KeyEvent keyEvent) {
            if (keyEvent.key() == InputConstants.KEY_O && !itemIcon.isEmpty() && hasItem()) {
                updateRecipe();
                selectedStack = itemIcon.copyWithCount(1);
                craftingTabList.tabButtons.get(1).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                return true;
            }
            return super.keyPressed(keyEvent);
        }

        void updateRecipe() {
            clearIngredients(ingredientsGrid);
            resultStack = itemIcon.copyWithCount(1);
            ingredientsGrid.set(4, resultStack.isEmpty() ? Optional.empty() : Optional.of(StackIngredient.of(true, resultStack, 1)));
            scrollableRenderer.resetScrolled();
        }

        @Override
        LegacyScrollRenderer getScrollRenderer() {
            return scrollRenderer;
        }
    });
    protected ItemStack resultStack = ItemStack.EMPTY;
    protected ItemStack selectedStack = ItemStack.EMPTY;
    protected ItemStack previewStack = ItemStack.EMPTY;
    protected List<RecipeInfo<BannerRecipe>> selectedPatterns = new ArrayList<>();
    protected List<List<RecipeInfo<BannerRecipe>>> recipesByGroup = new ArrayList<>();
    protected int selectedCraftingButton;
    protected boolean inited;
    public LegacyLoomScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        this.inventory = inventory;
        for (LoomTabListing listing : loomTabListings) {
            if (!listing.isValid()) continue;
            craftingTabList.add(LegacyTabButton.Type.LEFT, listing.icon(), listing.nameOrEmpty(), t -> resetElements());
        }
        inited = true;
        addCraftingButtons();
        resetElements(false);
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        ControlTooltip.setupDefaultButtons(renderer, this);
        Event.super.addControlTooltips(renderer);
        renderer.
                add(OPTION::get, () -> ControlTooltip.getKeyMessage(InputConstants.KEY_O, this)).
                add(ControlTooltip.EXTRA::get, () -> getFocused() instanceof RecipeIconHolder<?> r && r.getFocusedRecipe() != null && selectedPatterns.contains(r.getFocusedRecipe()) ? LegacyComponents.REMOVE : null).
                add(CONTROL_TAB::get, () -> isSelectionTab() ? null : LegacyComponents.GROUP).
                add(CONTROL_TYPE::get, () -> page.max > 0 ? LegacyComponents.PAGE : null);
    }

    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            recipesByGroup.clear();
            selectedIngredients.clear();
            if (!isSelectionTab() && !selectedStack.isEmpty()) {
                previewStack = selectedStack.copy();
                selectedIngredients.add(Optional.of(StackIngredient.of(true, selectedStack, 1)));
                if (!selectedPatterns.isEmpty()) {
                    List<BannerPatternLayers.Layer> layersList = previewStack.get(DataComponents.BANNER_PATTERNS) == null ? new ArrayList<>() : new ArrayList<>(previewStack.get(DataComponents.BANNER_PATTERNS).layers());
                    selectedPatterns.forEach(rcp -> {
                        layersList.add(new BannerPatternLayers.Layer(Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(rcp.get().pattern), rcp.get().color));
                        for (int i1 = 1; i1 < rcp.getOptionalIngredients().size(); i1++) {
                            Optional<Ingredient> ing = rcp.getOptionalIngredients().get(i1);
                            if (ing.isEmpty()) continue;
                            selectedIngredients.add(ing);
                        }
                    });
                    previewStack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(layersList));
                }
                Optional<Ingredient> previewIng = Optional.of(StackIngredient.of(true, previewStack, 1));
                loomTabListings.get(craftingTabList.getIndex()).patterns().stream().filter(p -> Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).listElementIds().anyMatch(p::equals)).forEach(p -> recipesByGroup.add(Arrays.stream(DyeColor.values()).map(color -> {
                    ItemStack result = previewStack.copy();
                    List<BannerPatternLayers.Layer> layersList = result.get(DataComponents.BANNER_PATTERNS) == null ? new ArrayList<>() : new ArrayList<>(result.get(DataComponents.BANNER_PATTERNS).layers());
                    layersList.add(new BannerPatternLayers.Layer(Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(p), color));
                    result.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(layersList));
                    Optional<Ingredient> dye = Optional.of(Ingredient.of(DyeItem.byColor(color)));
                    ArrayList<Optional<Ingredient>> previewIngs = new ArrayList<>(selectedIngredients);
                    previewIngs.add(dye);
                    List<Optional<Ingredient>> ings = List.of(previewIng, dye);
                    List<Optional<Ingredient>> displayIngs = new ArrayList<>(ings);
                    Optional<Ingredient> extraIng = LegacyCraftingMenu.getBannerPatternExtraIngredient(Minecraft.getInstance().getConnection().registryAccess(), p);
                    if (extraIng.isPresent()) {
                        displayIngs.add(1, extraIng);
                        previewIngs.add(extraIng);
                    }
                    displayIngs.set(0, selectedIngredients.get(0));
                    return RecipeInfo.create(p.location().withPrefix(color.getName() + "_"), new BannerRecipe(previewIngs, LegacyCraftingMenu.updateShapedIngredients(new ArrayList<>(Collections.nCopies(9, Optional.empty())), displayIngs, 3, 2, 2), p, color), ings, result);
                }).collect(Collectors.collectingAndThen(Collectors.toList(), l -> {
                    Collections.reverse(l);
                    return l;
                }))));
                craftingButtons.get(selectedCraftingButton).updateRecipeDisplay();
            } else {
                if (getCraftingButtons().size() > selectedCraftingButton && getCraftingButtons().get(selectedCraftingButton) instanceof LegacyCraftingScreen.CustomCraftingIconHolder h)
                    h.updateRecipe();
            }
        }

        public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

        }
    };

    public void resetElements() {
        resetElements(true);
    }

    public void resetElements(boolean reposition) {
        if (!reposition) listener.slotChanged(menu, -1, ItemStack.EMPTY);
        selectedCraftingButton = 0;
        craftingButtonsOffset.set(0);
        craftingButtons.get(selectedCraftingButton).invalidateFocused();
        if (reposition) repositionElements();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        if (guiEventListener instanceof TabList) return;
        super.setFocused(guiEventListener);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        LegacyFontUtil.applySDFont(b -> {
            Component title = !isSelectionTab() ? craftingTabList.tabButtons.get(craftingTabList.getIndex()).getMessage() : getFocused() instanceof CustomRecipeIconHolder h ? h.getDisplayName() : Component.empty();
            guiGraphics.drawString(this.font, title, ((!isSelectionTab() ? imageWidth : imageWidth / 2) - font.width(title)) / 2, accessor.getInteger("title.y", 17), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            int inventoryPanelX = accessor.getInteger("inventoryPanel.x", 176);
            int bottomPanelY = accessor.getInteger("bottomPanel.y", 103);
            int inventoryPanelWidth = accessor.getInteger("inventoryPanel.width", 163);
            if (menu.inventoryActive)
                guiGraphics.drawString(this.font, this.playerInventoryTitle, inventoryPanelX + (inventoryPanelWidth - font.width(playerInventoryTitle)) / 2, bottomPanelY + accessor.getInteger("inventoryTitle.y", 11), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            else
                guiGraphics.drawString(this.font, LegacyComponents.PREVIEW, inventoryPanelX + (inventoryPanelWidth - font.width(LegacyComponents.PREVIEW)) / 2, bottomPanelY + accessor.getInteger("inventoryTitle.y", 11), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        });
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderContents(guiGraphics, i, j, f);

        getCraftingButtons().forEach(b -> b.render(guiGraphics, i, j, 0));
        if (selectedCraftingButton < getCraftingButtons().size())
            getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.pressed && state.canClick() && state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s && !isSelectionTab())
            craftingTabList.controlPage(page, s.x < 0 && -s.x > Math.abs(s.y), s.x > 0 && s.x > Math.abs(s.y));
    }

    @Override
    public TabList getTabList() {
        return craftingTabList;
    }

    @Override
    public int getTabYOffset() {
        return accessor.getInteger("tabYOffset", 18);
    }

    @Override
    public boolean disableCursorOnInit() {
        return true;
    }

    @Override
    public boolean onceClickBindings(BindingState state) {
        return !state.is(ControllerBinding.DOWN_BUTTON) && Controller.Event.super.onceClickBindings(state);
    }

    @Override
    protected void init() {
        resultStack = ItemStack.EMPTY;
        imageWidth = 348;
        imageHeight = 215;
        super.init();
        LegacySlotDisplay display = new LegacySlotDisplay() {
            @Override
            public int getWidth() {
                return 16;
            }

            @Override
            public Vec2 getOffset() {
                return menu.inventoryOffset;
            }

            @Override
            public boolean isVisible() {
                return menu.inventoryActive;
            }
        };
        for (int i = 0; i < 36; i++) {
            Slot s = menu.slots.get(i);
            if (i < 27) {
                LegacySlotDisplay.override(s, 186 + (s.getContainerSlot() - 9) % 9 * 16, 133 + (s.getContainerSlot() - 9) / 9 * 16, display);
            } else {
                LegacySlotDisplay.override(s, 186 + s.getContainerSlot() * 16, 186, display);
            }
        }
        topPos += getTabYOffset();
        menu.addSlotListener(listener);
        getTabList().setSelected(selectedStack.isEmpty() ? 0 : Math.max(getTabList().getIndex(), 1));
        menu.inventoryActive = accessor.putBearer("isInventoryActive", Bearer.of(selectedStack.isEmpty()));
        int count = accessor.putStaticElement("craftingButtons.count", getCraftingButtons().size());
        if (selectedCraftingButton < count) setFocused(getCraftingButtons().get(selectedCraftingButton));
        int craftingButtonsX = accessor.getInteger("craftingButtons.x", 13);
        int craftingButtonsY = accessor.getInteger("craftingButtons.y", 38);
        int craftingButtonsSize = accessor.getInteger("craftingButtons.size", 27);
        if (!isSelectionTab()) {
            craftingButtonsOffset.max = Math.max(0, loomTabListings.get(page.get() * getMaxTabCount() + getTabList().getIndex()).patterns().size() - 12);
            craftingButtons.forEach(b -> {
                b.width = b.height = craftingButtonsSize;
                b.setPos(leftPos + craftingButtonsX + craftingButtons.indexOf(b) * b.width, topPos + craftingButtonsY);
                addWidget(b);
            });
        } else {
            int slotX = accessor.getInteger("customCraftingButtons.x", (count == 1 ? 77 : count == 2 ? 52 : count == 3 ? 21 : 8));
            int xOffset = accessor.getInteger("customCraftingButtons.offset.x", (count == 2 ? 35 : count == 3 ? 28 : 18));
            getCraftingButtons().forEach(b -> {
                b.width = b.height = craftingButtonsSize;
                int index = getCraftingButtons().indexOf(b);
                b.setPos(leftPos + slotX + index * (b.width + xOffset), topPos + craftingButtonsY);
                if (count == 3)
                    b.offset = new Vec2((LegacyRenderUtil.hasHorizontalArtifacts() ? 0.0125f : 0.0f) + 0.5f + index * 0.5f, 0);
                b.init();
                addWidget(b);
            });
        }

        addWidget(getTabList());
        getTabList().init(leftPos, topPos - 37, imageWidth, 43, (t, i) -> {
            int index = getTabList().tabButtons.indexOf(t);
            t.active = index == 0 && selectedStack.isEmpty() || index != 0 && !selectedStack.isEmpty();
            t.type = LegacyTabButton.Type.bySize(index, getMaxTabCount());
            t.setWidth(accessor.getInteger("tabList.buttonWidth", 51));
            t.offset = (t1) -> new Vec2((LegacyRenderUtil.hasHorizontalArtifacts() && index % 2 != 0 ? 0.0125f : 0.0f) + accessor.getFloat("tabList.buttonOffset.x", -1.5f) * index, t1.active ? t1.selected ? 0 : accessor.getFloat("tabList.selectedOffset.y", 4.4f) : accessor.getFloat("tabList.inactiveOffset.y", 26.4f));
        });
        listener.slotChanged(menu, -1, ItemStack.EMPTY);
    }

    protected int getMaxTabCount() {
        return accessor.getInteger("maxTabCount", 7);
    }

    protected boolean canCraft() {
        return selectedIngredients.size() > 1 && LegacyLoomScreen.this.canCraft(selectedIngredients, false);
    }

    protected boolean canCraft(List<Optional<Ingredient>> ingredients, boolean isFocused) {
        compactInventoryList.clear();
        RecipeMenu.handleCompactInventoryList(compactInventoryList, Minecraft.getInstance().player.getInventory(), menu.getCarried());
        return LegacyCraftingScreen.canCraft(compactInventoryList, ingredients, isFocused ? warningSlots : null);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        //? if >=1.21.2 {
        if (this.getChildAt(d, e).filter((guiEventListener) -> guiEventListener.mouseScrolled(d, e, f, g)).isPresent())
            return true;
        //?}
        if (super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g)) return true;
        if (scrollableRenderer.mouseScrolled(g)) return true;
        int scroll = (int) Math.signum(g);
        if (((craftingButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && craftingButtonsOffset.max > 0)) && craftingButtonsOffset.add(scroll, false) != 0) {
            repositionElements();
            return true;
        }
        return false;
    }

    protected void addCraftingButtons() {
        for (int i = 0; i < 12; i++) {
            int index = i;

            RecipeIconHolder<BannerRecipe> h;
            craftingButtons.add(h = new RecipeIconHolder<>(leftPos + 13 + i * 27, topPos + 38) {
                @Override
                public @Nullable Component getAction(Context context) {
                    return context.actionOfContext(KeyContext.class, c -> c.key() == InputConstants.KEY_RETURN && isFocused() && LegacyLoomScreen.this.canCraft() ? LegacyComponents.CREATE : c.key() == InputConstants.KEY_O && isFocused() && canCraft() ? LegacyComponents.ADD : null);
                }

                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }

                protected boolean canCraft(RecipeInfo<BannerRecipe> rcp) {
                    if (rcp == null) return true;
                    return LegacyItemUtil.getPatternsCount(previewStack) < 6 && LegacyLoomScreen.this.canCraft(rcp.get().displayIngredients(), isFocused() && getFocusedRecipe() == rcp) && LegacyLoomScreen.this.canCraft(rcp.get().previewIngredients, false);
                }

                protected List<RecipeInfo<BannerRecipe>> getRecipes() {
                    return recipesByGroup.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : recipesByGroup.get(craftingButtonsOffset.get() + index);
                }

                @Override
                public LegacyScrollRenderer getScrollRenderer() {
                    return scrollRenderer;
                }

                @Override
                protected void toggleCraftableRecipes(InputWithModifiers input) {
                }

                @Override
                public boolean keyPressed(KeyEvent keyEvent) {
                    if ((keyEvent.key() == InputConstants.KEY_O || keyEvent.key() == InputConstants.KEY_X) && isValidIndex()) {
                        if (keyEvent.key() == InputConstants.KEY_O) {
                            if (this.canCraft()) {
                                selectedPatterns.add(getFocusedRecipe());
                                LegacySoundUtil.playSimpleUISound(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0f);
                            }
                        } else selectedPatterns.remove(getFocusedRecipe());
                        int cycle = getFocusedRecipes().indexOf(getFocusedRecipe()) - getRecipes().indexOf(getFocusedRecipe());
                        focusedRecipes = null;
                        listener.slotChanged(menu, -1, ItemStack.EMPTY);
                        Collections.rotate(getFocusedRecipes(), cycle);
                        updateRecipeDisplay();
                        return true;
                    }
                    if (controlCyclicNavigation(keyEvent.key(), index, craftingButtons, craftingButtonsOffset, scrollRenderer, LegacyLoomScreen.this))
                        return true;
                    return super.keyPressed(keyEvent);
                }

                protected void updateRecipeDisplay(RecipeInfo<BannerRecipe> rcp) {
                    resultStack = getFocusedResult();
                    clearIngredients(ingredientsGrid);
                    if (rcp == null) {
                        return;
                    }
                    for (int i = 0; i < rcp.get().displayIngredients().size(); i++)
                        ingredientsGrid.set(i, rcp.get().displayIngredients().get(i));
                }

                @Override
                public void onPress(InputWithModifiers input) {
                    if (isFocused() && isValidIndex()) {
                        if (LegacyLoomScreen.this.canCraft()) {
                            LegacySoundUtil.playSimpleUISound(SoundEvents.UI_LOOM_TAKE_RESULT, 1.0f);
                            selectedPatterns.forEach(b -> CommonNetwork.sendToServer(new ServerMenuCraftPayload(Optional.of(b.get().pattern.location()), b.getOptionalIngredients(), -1, input.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed)));
                            selectedPatterns.clear();
                            selectedStack = ItemStack.EMPTY;
                            previewStack = ItemStack.EMPTY;
                            craftingTabList.tabButtons.get(0).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                        } else LegacySoundUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(), 1.0f);
                    }
                }

            });
            h.allowCraftableRecipesToggle = false;
            h.offset = new Vec2(0.5f, 0.5f);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        getTabList().render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(accessor.getResourceLocation("imageSprite", LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        getTabList().renderSelected(guiGraphics, i, j, f);
        int bottomPanelHeight = accessor.getInteger("bottomPanel.height", 105);
        int panelWidth = accessor.getInteger("craftingGridPanel.width", 163);
        int inventoryPanelX = accessor.getInteger("inventoryPanel.x", 176);
        int inventoryPanelWidth = accessor.getInteger("inventoryPanel.width", 163);
        int bottomPanelY = accessor.getInteger("bottomPanel.y", 103);
        int craftingGridPanelX = accessor.getInteger("craftingGridPanel.x", 9);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + craftingGridPanelX, topPos + bottomPanelY, panelWidth, bottomPanelHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + inventoryPanelX, topPos + bottomPanelY, inventoryPanelWidth, bottomPanelHeight);
        if (!menu.inventoryActive && !previewStack.isEmpty()) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(leftPos + inventoryPanelX + inventoryPanelWidth / 2f, topPos + bottomPanelY + bottomPanelHeight / 2f + 10);
            float previewScale = accessor.getFloat("preview.scale", 4.25f);
            guiGraphics.pose().scale(previewScale, previewScale);
            guiGraphics.renderItem(previewStack, -8, -8);
            guiGraphics.pose().popMatrix();
        }
        int descriptionPanelWidth = accessor.getInteger("descriptionPanel.width", 163);
        int descriptionPanelHeight = accessor.getInteger("descriptionPanel.height", 93);
        int descriptionPanelX = accessor.getInteger("descriptionPanel.x", 176);
        int descriptionPanelY = accessor.getInteger("descriptionPanel.y", 8);
        if (getTabList().getIndex() == 0)
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + descriptionPanelX, topPos + descriptionPanelY, descriptionPanelWidth, descriptionPanelHeight);
        int slotSize = accessor.getInteger("craftingGridSlot.size", 23);
        int xDiff = leftPos + craftingGridPanelX;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SMALL_ARROW, xDiff + accessor.getInteger("craftingArrow.x", 77) + (slotSize / 2), topPos + bottomPanelY + accessor.getInteger("craftingArrow.y", 57), 16, 14);
        if (getTabList().getIndex() != 0) {
            if (craftingButtonsOffset.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
            if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + imageWidth - 11, topPos + 45);
        }

        int resultSlotSize = accessor.getInteger("craftingResultSlot.size", 36);
        int yDiff = topPos + bottomPanelY;
        int craftingGridSlotX = accessor.getInteger("craftingGridSlot.x", 12);
        int craftingGridSlotY = accessor.getInteger("craftingGridSlot.y", 30);
        boolean anyWarning = false;
        for (int index = 0; index < ingredientsGrid.size(); index++) {
            LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.itemHolder(xDiff + craftingGridSlotX + index % 3 * slotSize, yDiff + craftingGridSlotY + index / 3 * slotSize, slotSize, slotSize, getActualItem(ingredientsGrid.get(index)), (!getActualItem(ingredientsGrid.get(index)).isEmpty() && warningSlots[index]), new Vec2(LegacyRenderUtil.hasHorizontalArtifacts() ? 0.4f : 0.5f, 0.4f));
            if (holder.isWarning()) anyWarning = true;
            holder.render(guiGraphics, i, j, f);
            holder.renderTooltip(minecraft, guiGraphics, i, j);
        }

        LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.itemHolder(xDiff + accessor.getInteger("resultSlot.x", 104) + (slotSize / 2), yDiff + accessor.getInteger("resultSlot.y", 48), resultSlotSize, resultSlotSize, resultStack, anyWarning, new Vec2(LegacyRenderUtil.hasHorizontalArtifacts() ? 0.4f : 0.5f, 0));
        holder.render(guiGraphics, i, j, f);
        holder.renderTooltip(minecraft, guiGraphics, i, j);

        if (!resultStack.isEmpty()) {
            Component resultName = getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h ? h.getFocusedRecipe().getName() : resultStack.getHoverName();
            Component description = getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h ? h.getFocusedRecipe().getDescription() : null;
            int titleY = bottomPanelY + accessor.getInteger("craftingTitle.y", 11) - (description == null ? 0 : 6);
            LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, resultName, xDiff + 2 + Math.max(panelWidth - font.width(resultName), 0) / 2, topPos + titleY, xDiff + panelWidth - 2, topPos + titleY + 11, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
            if (description != null)
                LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(guiGraphics, font, description.copy().setStyle(Style.EMPTY), xDiff + 2 + Math.max(panelWidth - font.width(description), 0) / 2, topPos + titleY + 12, xDiff + panelWidth - 2, topPos + titleY + 23, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
            LegacyFontUtil.applySDFont(b -> {
                if (getTabList().getIndex() == 0) {
                    int descriptionTextXOffset = accessor.getInteger("descriptionText.x", 4);
                    int descriptionTextYOffset = accessor.getInteger("descriptionText.y", 7);
                    int descriptionTextWidth = descriptionPanelWidth - descriptionTextXOffset * 2;
                    int lineSpacing = (b ? 8 : 12);
                    scrollableRenderer.lineHeight = lineSpacing;
                    int lineAmount = (descriptionPanelHeight - descriptionTextYOffset * 2 - 7) / lineSpacing;
                    List<FormattedCharSequence> list = LegacyRenderUtil.getTooltip(resultStack, descriptionTextWidth);
                    scrollableRenderer.scrolled.max = Math.max(0, list.size() - lineAmount);
                    scrollableRenderer.render(guiGraphics, leftPos + descriptionPanelX + descriptionTextXOffset, topPos + descriptionPanelY + descriptionTextYOffset, descriptionTextWidth, lineAmount * lineSpacing, () -> {
                        for (int i1 = 0; i1 < list.size(); i1++) {
                            FormattedCharSequence sequence = list.get(i1);
                            guiGraphics.drawString(font, sink -> sequence.accept((i2, style, j1) -> sink.accept(i2, Style.EMPTY, j1)), leftPos + descriptionPanelX + descriptionTextXOffset, topPos + descriptionPanelY + descriptionTextYOffset + i1 * lineSpacing, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.hasShiftDown() && !isSelectionTab() && craftingTabList.controlPage(page, keyEvent.isLeft(), keyEvent.isRight()))
            return true;
        if (craftingTabList.controlTab(keyEvent.key())) return true;
        return super.keyPressed(keyEvent);
    }

    public List<? extends LegacyIconHolder> getCraftingButtons() {
        return isSelectionTab() ? selectBannerButton : craftingButtons;
    }

    public boolean isSelectionTab() {
        return loomTabListings.get(craftingTabList.getIndex()).is(LoomTabListing.SELECT_BANNER);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }

    public record BannerRecipe(List<Optional<Ingredient>> previewIngredients,
                               List<Optional<Ingredient>> displayIngredients, ResourceKey<BannerPattern> pattern,
                               DyeColor color) {

    }





}
