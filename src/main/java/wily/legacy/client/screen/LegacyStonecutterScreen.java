package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Unique;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.network.CommonRecipeManager;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.RecipeInfo;
import wily.legacy.client.StoneCuttingGroupManager;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.*;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static wily.legacy.client.screen.ControlTooltip.*;


public class LegacyStonecutterScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event, ControlTooltip.Event {
    public static final Vec2 DISPLAY_OFFSET = new Vec2(0.5f, 0);
    public static final Vec2 ALT_DISPLAY_OFFSET = new Vec2(0.4f, 0);
    protected final List<ItemStack> compactInventoryList = new ArrayList<>();
    protected final UIAccessor accessor = UIAccessor.of(this);
    private final ContainerListener listener;
    protected List<RecipeIconHolder<StonecutterRecipe>> craftingButtons = new ArrayList<>();
    protected List<List<RecipeInfo<StonecutterRecipe>>> recipesByGroup = new ArrayList<>();
    protected List<List<RecipeInfo<StonecutterRecipe>>> filteredRecipesByGroup = Collections.emptyList();
    protected Stocker.Sizeable craftingButtonsOffset = new Stocker.Sizeable(0);
    protected LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected List<Optional<Ingredient>> ingredientSlot = Collections.singletonList(Optional.empty());
    protected int selectedCraftingButton = 0;
    private boolean onlyCraftableRecipes;

    public LegacyStonecutterScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
        List<RecipeInfo<StonecutterRecipe>> allRecipes = CommonRecipeManager.byType(RecipeType.STONECUTTING).stream().map(h -> RecipeInfo.create(h./*? if >1.20.1 {*/id()/*?} else {*//*getId()*//*?}*/, h/*? if >1.20.1 {*/.value()/*?}*/, LegacyCraftingMenu.getRecipeOptionalIngredients(h/*? if >1.20.1 {*/.value()/*?}*/), h/*? if >1.20.1 {*/.value()/*?}*/.assemble(null, Minecraft.getInstance().level.registryAccess()))).toList();
        StoneCuttingGroupManager.listing.values().forEach(l -> {
            List<RecipeInfo<StonecutterRecipe>> group = new ArrayList<>();
            l.forEach(v -> v.addRecipes(allRecipes.stream().filter(h -> recipesByGroup.stream().noneMatch(r -> r.contains(h)))::iterator, group::add));
            if (!group.isEmpty()) recipesByGroup.add(group);
        });
        allRecipes.stream().filter(h -> recipesByGroup.stream().noneMatch(r -> r.contains(h))).forEach(h -> recipesByGroup.add(Collections.singletonList(h)));
        addCraftingButtons();
        onlyCraftableRecipes = true;
        listener = new ContainerListener() {
            public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
                if (onlyCraftableRecipes) {
                    filteredRecipesByGroup = recipesByGroup.stream().map(l -> l.stream().filter(r -> RecipeMenu.canCraft(r.getOptionalIngredients(), inventory, abstractContainerMenu.getCarried())).toList()).filter(l -> !l.isEmpty()).toList();
                    craftingButtons.get(selectedCraftingButton).updateRecipeDisplay();
                }
            }

            public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

            }
        };
        listener.slotChanged(menu, 0, ItemStack.EMPTY);
        onlyCraftableRecipes = false;
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        ControlTooltip.setupDefaultButtons(renderer, this);
        Event.super.addControlTooltips(renderer);
        renderer.add(OPTION::get, () -> onlyCraftableRecipes ? LegacyComponents.ALL_RECIPES : LegacyComponents.SHOW_CRAFTABLE_RECIPES);
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
    public void init() {
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
        menu.addSlotListener(listener);
        if (selectedCraftingButton < craftingButtons.size()) setFocused(craftingButtons.get(selectedCraftingButton));
        int craftingButtonsX = accessor.getInteger("craftingButtons.x", 13);
        int craftingButtonsY = accessor.getInteger("craftingButtons.y", 38);
        int craftingButtonsSize = accessor.getInteger("craftingButtons.size", 27);
        craftingButtons.forEach(b -> {
            b.width = b.height = craftingButtonsSize;
            b.setPos(leftPos + craftingButtonsX + craftingButtons.indexOf(b) * craftingButtonsSize, topPos + craftingButtonsY);
            addRenderableWidget(b);
        });
        craftingButtonsOffset.max = Math.max(0, recipesByGroup.size() - 12);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    public void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        LegacyFontUtil.applySDFont(b -> {
            guiGraphics.drawString(this.font, title, (imageWidth - font.width(title)) / 2, accessor.getInteger("title.y", 17), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            int inventoryPanelX = accessor.getInteger("inventoryPanel.x", 176);
            int bottomPanelY = accessor.getInteger("bottomPanel.y", 103);
            int inventoryPanelWidth = accessor.getInteger("inventoryPanel.width", 163);
            guiGraphics.drawString(this.font, this.playerInventoryTitle, inventoryPanelX + (inventoryPanelWidth - font.width(playerInventoryTitle)) / 2, bottomPanelY + accessor.getInteger("inventoryTitle.y", 11), CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        });
    }

    @Override
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        boolean sd = LegacyOptions.getUIMode().isSD();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(accessor.getResourceLocation("imageSprite", LegacySprites.SMALL_PANEL), leftPos, topPos, imageWidth, imageHeight);
        int bottomPanelHeight = accessor.getInteger("bottomPanel.height", 105);
        int panelWidth = accessor.getInteger("craftingGridPanel.width", 163);
        int bottomPanelY = accessor.getInteger("bottomPanel.y", 103);
        int craftingGridPanelX = accessor.getInteger("craftingGridPanel.x", 9);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + craftingGridPanelX, topPos + bottomPanelY, panelWidth, bottomPanelHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + accessor.getInteger("inventoryPanel.x", 176), topPos + bottomPanelY, accessor.getInteger("inventoryPanel.width", 163), bottomPanelHeight);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(sd ? LegacySprites.SMALL_ARROW : LegacySprites.ARROW, leftPos + craftingGridPanelX + accessor.getInteger("craftingArrow.x", 70), topPos + bottomPanelY + accessor.getInteger("craftingArrow.y", 55), sd ? 16 : 22, sd ? 14 : 15);
        if (craftingButtonsOffset.get() > 0)
            scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, accessor.getInteger("horizontalScroll.x", leftPos + 5), topPos + 45);
        if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
            scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + imageWidth - 11, topPos + 45);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        renderTooltip(guiGraphics, i, j);
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderContents(guiGraphics, i, j, f);

        craftingButtons.get(selectedCraftingButton).renderSelection(guiGraphics, i, j, f);
        craftingButtons.forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        //? if >=1.21.2 {
        if (this.getChildAt(d, e).filter((guiEventListener) -> guiEventListener.mouseScrolled(d, e, f, g)).isPresent())
            return true;
        //?}
        if (super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g)) return true;
        int scroll = (int) Math.signum(g);
        if (((craftingButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && craftingButtonsOffset.max > 0)) && craftingButtonsOffset.add(scroll, false) != 0) {
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
    protected void addCraftingButtons() {
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
                    RecipeMenu.handleCompactInventoryList(compactInventoryList, Minecraft.getInstance().player.getInventory(), menu.getCarried());
                    return LegacyCraftingScreen.canCraft(compactInventoryList, isFocused() && getFocusedRecipe() == rcp ? ingredientSlot : rcp.getOptionalIngredients(), null);
                }

                @Override
                protected List<RecipeInfo<StonecutterRecipe>> getRecipes() {
                    List<List<RecipeInfo<StonecutterRecipe>>> list = onlyCraftableRecipes ? filteredRecipesByGroup : recipesByGroup;
                    return list.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : list.get(craftingButtonsOffset.get() + index);
                }

                @Override
                public LegacyScrollRenderer getScrollRenderer() {
                    return scrollRenderer;
                }

                @Override
                protected void updateRecipeDisplay(RecipeInfo<StonecutterRecipe> rcp) {
                    ingredientSlot = rcp == null ? Collections.singletonList(Optional.empty()) : rcp.getOptionalIngredients();
                }

                @Override
                protected void toggleCraftableRecipes(InputWithModifiers input) {
                    listener.slotChanged(menu, 0, ItemStack.EMPTY);
                    onlyCraftableRecipes = !onlyCraftableRecipes;
                }

                @Override
                public boolean keyPressed(KeyEvent keyEvent) {
                    if (controlCyclicNavigation(keyEvent.key(), index, craftingButtons, craftingButtonsOffset, scrollRenderer, LegacyStonecutterScreen.this))
                        return true;
                    return super.keyPressed(keyEvent);
                }

                @Override
                public void renderSelection(GuiGraphics graphics, int i, int j, float f) {
                    boolean warning = !canCraft(getFocusedRecipe());
                    int xDiff = leftPos + accessor.getInteger("craftingGridPanel.x", 9);
                    int yDiff = topPos + accessor.getInteger("bottomPanel.y", 103);
                    int panelWidth = accessor.getInteger("craftingGridPanel.width", 163);
                    int stonecutterSlotSize = accessor.getInteger("stonecutterSlot.size", 36);

                    LegacyIconHolder input = LegacyRenderUtil.iconHolderRenderer.itemHolder(xDiff + accessor.getInteger("inputSlot.x", 29), yDiff + accessor.getInteger("inputSlot.y",  43), stonecutterSlotSize, stonecutterSlotSize, getActualItem(ingredientSlot.get(0)), !onlyCraftableRecipes && !ingredientSlot.get(0).isEmpty() && warning, LegacyRenderUtil.hasHorizontalArtifacts() ? ALT_DISPLAY_OFFSET : DISPLAY_OFFSET);
                    input.render(graphics, i, j, f);
                    input.renderTooltip(minecraft, graphics, i, j);
                    LegacyIconHolder output = LegacyRenderUtil.iconHolderRenderer.itemHolder(xDiff + accessor.getInteger("resultSlot.x", 101), yDiff + accessor.getInteger("resultSlot.y", 43), stonecutterSlotSize, stonecutterSlotSize, getFocusedResult(), warning, LegacyRenderUtil.hasHorizontalArtifacts() ? ALT_DISPLAY_OFFSET : DISPLAY_OFFSET);
                    output.render(graphics, i, j, f);
                    output.renderTooltip(minecraft, graphics, i, j);

                    if (getFocusedRecipe() != null) {
                        Component resultName = getFocusedRecipe().getName();
                        int titleY = yDiff + accessor.getInteger("craftingTitle.y", 11);
                        LegacyFontUtil.applySDFont(b -> LegacyRenderUtil.renderScrollingString(graphics, font, resultName, xDiff + 2 + Math.max(panelWidth - font.width(resultName), 0) / 2, titleY, xDiff + panelWidth - 2, titleY + 11, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
                    }
                    super.renderSelection(graphics, i, j, f);
                }

            });
            h.offset = LegacyCraftingMenu.DEFAULT_INVENTORY_OFFSET;
            h.allowItemDecorations = false;
        }
    }
}
