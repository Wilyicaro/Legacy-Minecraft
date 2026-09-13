package wily.legacy.client.control.navigation;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import wily.legacy.client.control.ControllerListener;
import wily.legacy.client.control.ControllerManager;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.TabList;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Comparator;

public interface LegacyMenuAccess<T extends AbstractContainerMenu> extends MenuAccess<T>, GuiEventListener, ControllerListener {
    default boolean movePointerToSlot(Slot s) {
        return movePointerToSlot(s, true);
    }

    default boolean movePointerToSlot(Slot s, boolean allowHovered) {
        if (s == null || (!allowHovered && s == findHoveredSlot()) || !LegacySlotDisplay.isVisibleAndActive(s))
            return false;
        LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.slotBounds(getMenuRectangle().left(), getMenuRectangle().top(), s);
        ControllerManager.getInstance().setPointerPos(holder.getMiddleX(), holder.getMiddleY());
        return true;
    }

    ScreenRectangle getMenuRectangle();

    ScreenRectangle getMenuRectangleLimit();

    static ScreenRectangle createMenuRectangleLimit(LegacyMenuAccess<?> menu, int x, int y, int width, int height) {
        return createMenuRectangleLimit(menu, x, y, width, height, 20, 10);
    }

    static ScreenRectangle createMenuRectangleLimit(LegacyMenuAccess<?> menu, int x, int y, int width, int height, int paddingH, int paddingV) {
        if (menu instanceof TabList.Access tabList) {
            x -= tabList.getTabXOffset();
            width += tabList.getTabXOffset();
            y -= tabList.getTabYOffset();
            height += tabList.getTabYOffset();
        }
        return new ScreenRectangle(x - paddingH, y - paddingV, width + paddingH * 2, height + paddingV * 2);
    }

    boolean isOutsideClick(int i);

    Slot getHoveredSlot();

    default Slot findSlotAt(double d, double e) {
        ScreenRectangle rectangle = getMenuRectangle();
        for (Slot slot : getMenu().slots)
            if (LegacyRenderUtil.isHovering(slot, rectangle.left(), rectangle.top(), d, e)) return slot;
        return null;
    }

    default Slot findHoveredSlot() {
        return findSlotAt(ControllerManager.getInstance().getPointerX(), ControllerManager.getInstance().getPointerY());
    }

    default int getTipXOffset() {
        return -132;
    }

    default boolean allowItemPopping() {
        return false;
    }

    default boolean isMouseDragging() {
        return false;
    }
}
