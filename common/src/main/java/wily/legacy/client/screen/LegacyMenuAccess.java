package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.util.ScreenUtil;

import java.util.Comparator;

public interface LegacyMenuAccess<T extends AbstractContainerMenu> extends MenuAccess<T>, GuiEventListener {
    default void movePointerToSlotIn(ScreenDirection direction){
        if (getMenu().slots.isEmpty() || LegacyMinecraftClient.controllerHandler.isCursorDisabled || getHoveredSlot() == null) return;
        double pointerX = LegacyMinecraftClient.controllerHandler.getPointerX();
        double pointerY = LegacyMinecraftClient.controllerHandler.getPointerY();
        int height = getRectangle().height();
        int width = getRectangle().width();
        boolean horizontal = direction.getAxis() == ScreenAxis.HORIZONTAL;
        boolean positive = direction.isPositive();
        if (getMenu().slots.size() == 1 && movePointerToSlot(getMenu().slots.get(0),false)) return;
        int part = getMenu().slots.stream().map(s->Math.round(ScreenUtil.iconHolderRenderer.slotBounds(s).getMinSize() / 2f)).sorted().findFirst().orElse(9);
        for (int i = 0; i < (horizontal ? height - pointerY : width - pointerX); i+=part)
            if (movePointerToSlotIn(positive,horizontal,horizontal ? width : height, i, (int)pointerX, (int)pointerY,part)) return;
        for (int i = 0; i >= (horizontal ? -pointerY : -pointerX); i-=part)
            if (movePointerToSlotIn(positive,horizontal,horizontal ? width : height, i, (int)pointerX, (int)pointerY,part)) return;
        for (int i = 0; i < (horizontal ? height - pointerY : width - pointerX); i+=part)
            if (movePointerToSlotInReverse(positive,horizontal,horizontal ? width : height, i, (int)pointerX, (int)pointerY,part)) return;
        for (int i = 0; i >= (horizontal ? -pointerY : -pointerX); i-=part)
            if (movePointerToSlotInReverse(positive,horizontal,horizontal ? width : height, i, (int)pointerX, (int)pointerY,part)) return;

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
        if (s == null || (s == getHoveredSlot() && !allowHovered) || !s.isActive()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(getMenuRectangle().left(), getMenuRectangle().top(), s);
        LegacyMinecraftClient.controllerHandler.setPointerPos(holder.getMiddleX() * ((double)minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth()), holder.getMiddleY() * ((double)minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()));
        return true;
    }
    default void movePointerToNextSlot(){
        if (getMenu().slots.isEmpty() || LegacyMinecraftClient.controllerHandler.isCursorDisabled || getHoveredSlot() == null) return;
        double pointerX = LegacyMinecraftClient.controllerHandler.getPointerX();
        double pointerY = LegacyMinecraftClient.controllerHandler.getPointerY();
        if (getMenu().slots.size() == 1 && movePointerToSlot(getMenu().slots.get(0))) return;
        getMenu().slots.stream().min(Comparator.comparingInt(s->{
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(getMenuRectangle().left(),getMenuRectangle().top(),s);
            double deltaX = pointerX - holder.getMiddleX();
            double deltaY = pointerY - holder.getMiddleY();
            return (int) (deltaX *deltaX + deltaY * deltaY);
        })).ifPresent(this::movePointerToSlot);
    }
    ScreenRectangle getMenuRectangle();
    Slot getHoveredSlot();
    default Slot findSlotAt(double d, double e){
        for (Slot slot : getMenu().slots)
            if (ScreenUtil.isHovering(slot,getMenuRectangle().left(), getMenuRectangle().top(),d,e)) return slot;
        return null;
    }
}
