package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;

import java.util.ArrayList;
import java.util.List;

import static wily.legacy.client.screen.ControlTooltip.Renderer;

public abstract class RecipesScreen<T extends AbstractContainerMenu, H extends LegacyIconHolder> extends AbstractContainerScreen<T> implements Controller.Event, ControlTooltip.Event {
    protected final UIAccessor accessor = UIAccessor.of(this);

    protected final List<H> recipeButtons = new ArrayList<>();
    protected final Inventory inventory;
    protected int selectedRecipeButton;

    protected final Stocker.Sizeable recipeButtonsOffset = new Stocker.Sizeable(0);
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private int timesInventoryChanged;
    private int updateTimer = 0;

    public RecipesScreen(T menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.inventory = inventory;
        addRecipeButtons();
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        ControlTooltip.setupDefaultButtons(renderer, this);
        ControlTooltip.Event.super.addControlTooltips(renderer);
    }

    @Override
    public boolean disableCursorOnInit() {
        return true;
    }

    @Override
    public boolean onceClickBindings(BindingState state) {
        return !state.is(ControllerBinding.DOWN_BUTTON) && Controller.Event.super.onceClickBindings(state);
    }

    protected abstract void updateRecipes();

    protected void updateRecipesAndResetTimer() {
        updateRecipes();
        updateTimer = 0;
    }

    protected void containerTick() {
        super.containerTick();
        updateTimer++;
        if (this.timesInventoryChanged != inventory.getTimesChanged()) {
            updateRecipesAndResetTimer();
            this.timesInventoryChanged = inventory.getTimesChanged();
        }

        if (updateTimer >= 20) {
            updateRecipesAndResetTimer();
        }
    }

    public void renderRecipesScroll(GuiGraphics guiGraphics, int x, int y) {
        if (recipeButtonsOffset.get() > 0)
            scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + accessor.getInteger("horizontalScroll.x", x), topPos + accessor.getInteger("horizontalScroll.y", y));
        if (recipeButtonsOffset.max > 0 && recipeButtonsOffset.get() < recipeButtonsOffset.max)
            scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + imageWidth - 6 - accessor.getInteger("horizontalScroll.x", x), topPos + accessor.getInteger("horizontalScroll.y", y));
    }

    protected abstract H createRecipeButton(int index);

    protected void addRecipeButtons() {
        int lastSize = recipeButtons.size();
        int max = getMaxRecipeButtons();
        if (lastSize == max) return;

        if (max > lastSize) {
            for (int i = lastSize; i < max; i++) {
                recipeButtons.add(createRecipeButton(i));
            }
        } else {
            while (recipeButtons.size() != max) {
                recipeButtons.remove(recipeButtons.size() - 1);
            }

            if (selectedRecipeButton >= recipeButtons.size())
                selectedRecipeButton = recipeButtons.size() - 1;
        }
    }

    public abstract int getMaxRecipeButtons();

    public List<? extends LegacyIconHolder> getRecipeButtons() {
        return recipeButtons;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        getRecipeButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));
        renderTooltip(guiGraphics, i, j);
    }

    @Override
    public void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderContents(guiGraphics, i, j, f);

        getRecipeButtons().forEach(b -> b.render(guiGraphics, i, j, 0));
        if (selectedRecipeButton < getRecipeButtons().size())
            getRecipeButtons().get(selectedRecipeButton).renderSelection(guiGraphics, i, j, 0);
    }

    //? if >1.20.1 {
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }
    //?}

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (this.getChildAt(d, e).filter((guiEventListener) -> guiEventListener.mouseScrolled(d, e, f, g)).isPresent())
            return true;
        if (super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g)) return true;
        int scroll = (int) Math.signum(g);
        if (((recipeButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && recipeButtonsOffset.max > 0)) && recipeButtonsOffset.add(scroll, false) != 0) {
            updateRecipesAndResetTimer();
            return true;
        }
        return false;
    }

    protected void init() {
        super.init();
        addRecipeButtons();
    }
}
