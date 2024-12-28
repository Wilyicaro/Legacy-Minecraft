package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.controller.Controller;
import wily.legacy.util.ScreenUtil;

import java.util.Comparator;

public interface LegacyMenuAccess<T extends AbstractContainerMenu> extends MenuAccess<T>, GuiEventListener, Controller.Event {
    default void movePointerToSlotIn(ScreenDirection direction){
        if (getMenu().slots.isEmpty() || Legacy4JClient.controllerManager.isCursorDisabled || findHoveredSlot() == null) return;
        double pointerX = Legacy4JClient.controllerManager.getPointerX();
        double pointerY = Legacy4JClient.controllerManager.getPointerY();
        int height = getRectangle().height();
        int width = getRectangle().width();
        boolean horizontal = direction.getAxis() == ScreenAxis.HORIZONTAL;
        boolean positive = direction.isPositive();
        if (getMenu().slots.size() == 1 && movePointerToSlot(getMenu().slots.get(0),false)) return;
        int part = getMenu().slots.stream().map(s->Math.round(ScreenUtil.iconHolderRenderer.slotBounds(s).getMinSize() / 2f)).sorted().findFirst().orElse(9);
        double r = horizontal ? height - pointerY : width - pointerX;
        double l = (horizontal ? pointerY : pointerX);
        for (int i = 0; i < Math.max(r,l); i+=part) {
            if (i <= r && movePointerToSlotIn(positive, horizontal, horizontal ? width : height, i, (int) pointerX, (int) pointerY, part)) return;
            if (i <= l && movePointerToSlotIn(positive, horizontal, horizontal ? width : height, -i, (int) pointerX, (int) pointerY, part)) return;
        }
        for (int i = 0; i < Math.max(r,l); i+=part) {
            if (i <= r && movePointerToSlotInReverse(positive, horizontal, horizontal ? width : height, i, (int) pointerX, (int) pointerY, part)) return;
            if (i <= l && movePointerToSlotInReverse(positive, horizontal, horizontal ? width : height, -i, (int) pointerX, (int) pointerY, part)) return;
        }

    }
    default boolean movePointerToSlotIn(boolean positive, boolean horizontal, int size, int pos, int pointerX, int pointerY, int part){
        if (positive) {
            for (int j = part * 2; j < size - (horizontal ? pointerX : pointerY); j+=part) if (movePointerToSlot(findSlotAt(pointerX + (horizontal ? j : pos), pointerY + (horizontal ? pos : j)),false)) return true;
        } else {
            for (int j = -part * 2; j >= -(horizontal ? pointerX : pointerY); j-=part) if (movePointerToSlot(findSlotAt(pointerX + (horizontal ? j : pos), pointerY + (horizontal ? pos : j)),false)) return true;
        } return false;
    }
    default boolean movePointerToSlotInReverse(boolean positive, boolean horizontal, int size, int pos, int pointerX, int pointerY, int part){
        if (positive) {
            for (int j = -(horizontal ? pointerX : pointerY) + part * 2; j < 0; j+=part) if (movePointerToSlot(findSlotAt(pointerX + (horizontal ? j : pos), pointerY + (horizontal ? pos : j)),false)) return true;
        } else {
            for (int j = size -(horizontal ? pointerX : pointerY) - part * 2; j >= 0; j-=part) if (movePointerToSlot(findSlotAt(pointerX + (horizontal ? j : pos), pointerY + (horizontal ? pos : j)),false)) return true;
        } return false;
    }
    default boolean movePointerToSlot(Slot s){
        return movePointerToSlot(s,true);
    }
    default boolean movePointerToSlot(Slot s, boolean allowHovered){
        if (s == null || (!allowHovered && s == findHoveredSlot()) || !s.isActive()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(getMenuRectangle().left(), getMenuRectangle().top(), s);
        Legacy4JClient.controllerManager.setPointerPos(holder.getMiddleX() * ((double)minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth()), holder.getMiddleY() * ((double)minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()));
        return true;
    }
    default void movePointerToNextSlot(){
        if (getMenu().slots.isEmpty() || Legacy4JClient.controllerManager.isCursorDisabled || getHoveredSlot() == null) return;
        double pointerX = Legacy4JClient.controllerManager.getPointerX();
        double pointerY = Legacy4JClient.controllerManager.getPointerY();
        if (getMenu().slots.size() == 1 && movePointerToSlot(getMenu().slots.get(0))) return;
        getMenu().slots.stream().min(Comparator.comparingInt(s->{
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(getMenuRectangle().left(),getMenuRectangle().top(),s);
            double deltaX = pointerX - holder.getMiddleX();
            double deltaY = pointerY - holder.getMiddleY();
            return (int) (deltaX *deltaX + deltaY * deltaY);
        })).ifPresent(this::movePointerToSlot);
    }
    ScreenRectangle getMenuRectangle();
    boolean isOutsideClick(int i);
    Slot getHoveredSlot();
    default Slot findSlotAt(double d, double e){
        for (Slot slot : getMenu().slots)
            if (ScreenUtil.isHovering(slot,getMenuRectangle().left(), getMenuRectangle().top(),d,e)) return slot;
        return null;
    }
    default Slot findHoveredSlot(){
        return findSlotAt(Legacy4JClient.controllerManager.getPointerX(),Legacy4JClient.controllerManager.getPointerY());
    }
    default int getTipXDiff(){
        return -132;
    }
    default boolean allowItemPopping(){
        return false;
    }
}
