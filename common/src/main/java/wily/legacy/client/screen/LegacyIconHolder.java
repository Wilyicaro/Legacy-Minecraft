package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.LegacySlotWrapper;

public class LegacyIconHolder extends SimpleLayoutRenderable {
    public static ResourceLocation ICON_HOLDER = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/icon_holder");
    public static ResourceLocation SIZEABLE_ICON_HOLDER = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/sizeable_icon_holder");
    public ResourceLocation iconSprite = null;
    public LegacyIconHolder(){}
    public LegacyIconHolder(Slot slot){
        slotBounds(slot);
    }
    public LegacyIconHolder(int width, int height){
        this.width = width;
        this.height = height;
    }
    public LegacyIconHolder slotBounds(Slot slot){
        width= slot instanceof LegacySlotWrapper s ? s.getWidth() : 18;
        height = slot instanceof LegacySlotWrapper s ? s.getHeight() : 18;
        setX(slot.x);
        setY(slot.y);
        iconSprite = slot instanceof LegacySlotWrapper s ? s.getIconSprite() : null;
        return this;
    }
    public float getXCorner(){
        return getX() - (isSizeable() ?  1 : getWidth() / 20f);
    }
    public float getYCorner(){
        return getY() - (isSizeable() ?  1 : getHeight() / 20f);
    }
    public float getSelectableWidth(){
        return getWidth() - 2 * (isSizeable() ?  1 : getWidth() / 20f);
    }
    public float getSelectableHeight(){
        return getHeight() - 2 * (isSizeable() ?  1 : getHeight() / 20f);
    }
    public boolean isSizeable(){
        return Math.min(getWidth(),getHeight()) < 18;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getXCorner(),getYCorner(),0);
        guiGraphics.blitSprite(isSizeable() ? SIZEABLE_ICON_HOLDER : ICON_HOLDER, 0, 0, getWidth(), getHeight());
        if (iconSprite != null) {
            guiGraphics.pose().translate(getX() - getXCorner() + (getSelectableWidth() - 16) / 2,getY() - getYCorner() + (getSelectableHeight() - 16) / 2,0);
            guiGraphics.blitSprite(iconSprite, 0, 0, 16, 16);
        }
        guiGraphics.pose().popPose();
    }
}
