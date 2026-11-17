package wily.legacy.client.control.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.control.*;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.client.LegacySoundUtil;

//TODO Make this not inherit LegacyIconHolder (basically, remove the rendering from this)
public class LegacySlotWidget extends LegacyIconHolder implements NavigationElement {
    public final Slot slot;
    public int itemSeed;
    public String quickCraftText = null;
    public boolean quickCraftHighlight = false;
    public boolean isVisible = true;

    public LegacySlotWidget(Slot slot) {
        this.slot = slot;
    }

    @Override
    public void renderItem(GuiGraphicsExtractor graphics, int i, int j, float f) {
        if (isHovered)
            renderHighlight(graphics);
        if (quickCraftHighlight)
            renderHighlight(graphics);
        super.renderItem(graphics, i, j, f);
    }

    @Override
    public void applyFocus(ComponentPath.Path path, boolean apply) {
        if (apply) {
            path.component().setFocused(null);
            if (ControllerManager.getInstance().isControllerTheLastInput() && LegacyControlsOptions.interfaceSensitivity.get() > 0)
                ControllerBinding.LEFT_STICK.state().block();
            ControllerManager.getInstance().enableCursor();

            ControllerManager.getInstance().setPointerPos(getMiddleX(), getMiddleY());
        }
    }

    @Override
    public void playFocusSound(ComponentPath.Path path) {
        if (LegacyOptions.inventoryHoverFocusSound.get())
            LegacySoundUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        return isHovered;
    }

    @Override
    public boolean isMouseOver(double d, double e) {
        return false;
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
        return isVisible && !isHovered && (!ControllerListener.of(Minecraft.getInstance().screen).disableCursorOnInit() || !ControllerManager.getInstance().isControllerTheLastInput() || LegacyControlsOptions.cursorMode.get().isAlways()) ? super.nextFocusPath(focusNavigationEvent) : null;
    }

    @Override
    public void renderItem(GuiGraphicsExtractor graphics, ItemStack item, int x, int y, boolean isWarning) {
        if (!item.isEmpty()) renderItem(graphics, () -> {
            renderPaddedItem(graphics, item, () -> graphics.item(item, 0, 0, itemSeed));
            if (allowItemDecorations)
                graphics.itemDecorations(Minecraft.getInstance().font, item, 0, 0, quickCraftText);
        }, x, y, isWarning);
    }
}
