package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.base.Stocker;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ItemViewerScreen extends PanelBackgroundScreen implements LegacyMenuAccess<AbstractContainerMenu> {
    public static final Container layerSelectionGrid = new SimpleContainer(50);
    public final List<ItemStack> layerItems = new ArrayList<>();
    public final List<LegacySlotWidget> slotWidgets = new ArrayList<>();
    protected final Stocker.Sizeable scrolledList = new Stocker.Sizeable(0);

    protected final AbstractContainerMenu menu;
    protected final LegacyScroller scroller = LegacyScroller.create(135, () -> scrolledList);
    protected Slot hoveredSlot = null;

    public ItemViewerScreen(Screen parent, Function<Screen, Panel> panelConstructor, Component component) {
        super(parent, panelConstructor, component);
        menu = new AbstractContainerMenu(null, -1) {

            @Override
            public ItemStack quickMoveStack(Player player, int i) {
                return null;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };
        for (int i = 0; i < layerSelectionGrid.getContainerSize(); i++) {
            menu.slots.add(LegacySlotDisplay.override(new Slot(layerSelectionGrid, i, 23 + i % 10 * 27, 24 + i / 10 * 27), new LegacySlotDisplay() {
                public int getWidth() {
                    return 27;
                }

                public int getHeight() {
                    return 27;
                }
            }));
        }
        for (Slot slot : menu.slots) {
            slotWidgets.add(new LegacySlotWidget(slot));
        }
        addLayerItems();
        scrolledList.max = Math.max(0, (layerItems.size() - 1) / layerSelectionGrid.getContainerSize());
    }

    protected void addLayerItems() {
        BuiltInRegistries.ITEM.forEach(i -> {
            if (i != Items.AIR) layerItems.add(i.getDefaultInstance());
        });
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        ControlTooltip.setupDefaultScreen(renderer, this).add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_W) : ControllerBinding.RIGHT_TRIGGER.getIcon(), () -> getHoveredSlot() != null && getHoveredSlot().hasItem() && LegacyTipManager.hasTip(getHoveredSlot().getItem()) ? LegacyComponents.WHATS_THIS : null);
    }

    @Override
    protected void init() {
        panel.init();
        scroller.setPosition(panel.x + 299, panel.y + 23);
        scroller.offset(new Vec3(LegacyRenderUtil.hasHorizontalArtifacts() ? 0.0f : 0.5f, 0, 0));
        fillLayerGrid();
        for (LegacySlotWidget slotWidget : slotWidgets) {
            addWidget(slotWidget);
        }
    }

    public void fillLayerGrid() {
        for (int i = 0; i < layerSelectionGrid.getContainerSize(); i++) {
            int index = scrolledList.get() * 50 + i;
            layerSelectionGrid.setItem(i, layerItems.size() > index ? layerItems.get(index) : ItemStack.EMPTY);
        }
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (scroller.mouseScrolled(g)) fillLayerGrid();
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (scroller.mouseClicked(event)) fillLayerGrid();
        if (hoveredSlot != null) slotClicked(hoveredSlot);
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        scroller.mouseReleased(event);
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == InputConstants.KEY_W && hoveredSlot != null && hoveredSlot.hasItem() && LegacyTipManager.setTip(LegacyTipManager.getTip(hoveredSlot.getItem().copy()))) {
            LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    protected void slotClicked(Slot slot) {
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double f, double g) {
        if (scroller.mouseDragged(event.y())) fillLayerGrid();
        return super.mouseDragged(event, f, g);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        setHoveredSlot(null);
        for (LegacySlotWidget slotWidget : slotWidgets) {
            slotWidget.isHovered = LegacyRenderUtil.isHovering(slotWidget.slot, panel.x, panel.y, i, j);
            if (slotWidget.isHovered)
                setHoveredSlot(slotWidget.slot);
            slotWidget.slotBoundsWithItem(panel.x, panel.y, slotWidget.slot);
            slotWidget.render(guiGraphics, i, j, f);
        }
        if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty())
            guiGraphics.setTooltipForNextFrame(font, hoveredSlot.getItem(), i, j);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        LegacyRenderUtil.renderDefaultBackground(accessor, guiGraphics, false);
        panel.render(guiGraphics, i, j, f);
        renderScroll(guiGraphics, i, j, f);
    }

    protected void renderScroll(GuiGraphics guiGraphics, int i, int j, float f) {
        scroller.render(guiGraphics, i, j, f);
    }

    @Override
    public AbstractContainerMenu getMenu() {
        return menu;
    }

    @Override
    public ScreenRectangle getMenuRectangle() {
        return new ScreenRectangle(panel.x, panel.y, panel.width, panel.height);
    }

    @Override
    public ScreenRectangle getMenuRectangleLimit() {
        return new ScreenRectangle(panel.x + 23, panel.y + 24, 10 * 27 - 2, 5 * 27 - 2);
    }

    @Override
    public boolean isOutsideClick(int i) {
        return false;
    }

    @Override
    public Slot getHoveredSlot() {
        return hoveredSlot;
    }

    public void setHoveredSlot(Slot hoveredSlot) {
        this.hoveredSlot = hoveredSlot;
    }

    @Override
    public int getTipXDiff() {
        return 0;
    }

    @Override
    public boolean disableCursorOnWidgets() {
        return true;
    }
}
