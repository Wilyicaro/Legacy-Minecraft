package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.LegacySlotWrapper;

public class LegacyIconHolder extends SimpleLayoutRenderable {
    public static ResourceLocation ICON_HOLDER = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/icon_holder");
    public static ResourceLocation SIZEABLE_ICON_HOLDER = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/sizeable_icon_holder");
    public Vec3 translation = null;
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
        translation = slot instanceof LegacySlotWrapper s ? s.getTranslation() : null;
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
    public boolean canSizeIcon(){
        return Math.min(getWidth(),getHeight()) > 21;
    }

    public void applyTranslation(GuiGraphics graphics){
        if (translation != null) graphics.pose().translate(translation.x,translation.y,translation.z);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getXCorner(),getYCorner(),0);
        applyTranslation(guiGraphics);
        guiGraphics.blitSprite(isSizeable() ? SIZEABLE_ICON_HOLDER : ICON_HOLDER, 0, 0, getWidth(), getHeight());
        if (iconSprite != null) {
            guiGraphics.pose().translate(getX() - getXCorner() ,getY() - getYCorner(),0);
            if (canSizeIcon()) {
                guiGraphics.pose().scale(getSelectableWidth() / 16f,getSelectableHeight() / 16f,getSelectableHeight() / 16f);
            }else guiGraphics.pose().translate((getSelectableWidth() - 16) / 2,(getSelectableHeight() - 16) / 2,0);
            guiGraphics.blitSprite(iconSprite, 0, 0, 16, 16);
        }
        guiGraphics.pose().popPose();
    }
}
