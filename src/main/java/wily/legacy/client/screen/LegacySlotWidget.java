package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.NavigationElement;
import wily.legacy.client.controller.Controller;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.client.LegacySoundUtil;

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
    public void renderItem(GuiGraphics graphics, int i, int j, float f) {
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
            if (Legacy4JClient.controllerManager.isControllerTheLastInput() && LegacyOptions.interfaceSensitivity.get() > 0)
                ControllerBinding.LEFT_STICK.state().block();
            Legacy4JClient.controllerManager.enableCursor();
            Legacy4JClient.controllerManager.setPointerPos(getMiddleX(), getMiddleY());
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
        return isVisible && !isHovered && (!Controller.Event.of(Minecraft.getInstance().screen).disableCursorOnInit() || !Legacy4JClient.controllerManager.isControllerTheLastInput() || LegacyOptions.cursorMode.get().isAlways()) ? super.nextFocusPath(focusNavigationEvent) : null;
    }

    @Override
    public void renderItem(GuiGraphics graphics, ItemStack item, int x, int y, boolean isWarning) {
        if (!item.isEmpty()) renderItem(graphics, () -> {
            graphics.renderItem(item, 0, 0, itemSeed);
            if (allowItemDecorations)
                graphics.renderItemDecorations(Minecraft.getInstance().font, item, 0, 0, quickCraftText);
        }, x, y, isWarning);
    }
}
