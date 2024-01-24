package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.LegacySlotWrapper;
import wily.legacy.util.ScreenUtil;

public class LegacyIconHolder extends SimpleLayoutRenderable implements GuiEventListener, NarratableEntry {
    public static final ResourceLocation ICON_HOLDER = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/icon_holder");
    public static final ResourceLocation SIZEABLE_ICON_HOLDER = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/sizeable_icon_holder");
    public static final ResourceLocation SELECT_ICON_HIGHLIGHT = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/select_icon_highlight");
    public static final ResourceLocation RED_ICON_HOLDER = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/red_icon_holder");
    public static final ResourceLocation GRAY_ICON_HOLDER = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/gray_icon_holder");
    public static final ResourceLocation WARNING_ICON = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/icon_warning");

    public Vec3 translation = null;
    public ResourceLocation iconSprite = null;

    public ItemStack itemIcon = ItemStack.EMPTY;

    public boolean allowItemDecorations = true;
    private boolean isWarning = false;
    public boolean isHovered;
    private boolean focused = false;

    public LegacyIconHolder(){}
    public LegacyIconHolder(Slot slot){
        slotBounds(slot);
    }
    public LegacyIconHolder(int leftPos, int topPos, Slot slot){
        slotBounds(leftPos, topPos, slot);
    }
    public LegacyIconHolder(int width, int height){
        this.width = width;
        this.height = height;
    }
    public LegacyIconHolder slotBounds(int leftPos, int topPos, Slot slot){
        width= slot instanceof LegacySlotWrapper s ? s.getWidth() : 18;
        height = slot instanceof LegacySlotWrapper s ? s.getHeight() : 18;
        setX(leftPos + slot.x);
        setY(topPos + slot.y);
        iconSprite = slot instanceof LegacySlotWrapper s ? s.getIconSprite() : null;
        translation = slot instanceof LegacySlotWrapper s ? s.getTranslation() : null;
        itemIcon = ItemStack.EMPTY;
        isWarning = false;
        allowItemDecorations = true;
        return this;
    }
    public LegacyIconHolder slotBounds(Slot slot){
        return slotBounds(0,0,slot);
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
    protected boolean isWarning(){
        return isWarning;
    }
    public void setWarning(boolean warning){
        this.isWarning = warning;
    }
    public ResourceLocation getIconHolderSprite(){
        return isWarning() ? RED_ICON_HOLDER : isSizeable() ? SIZEABLE_ICON_HOLDER : ICON_HOLDER;
    }
    @Override
    public void render(GuiGraphics graphics, int i, int j, float f) {
        isHovered = ScreenUtil.isMouseOver(getXCorner(), getYCorner(), width, height, i, j);
        graphics.pose().pushPose();
        graphics.pose().translate(getXCorner(),getYCorner(),0);
        applyTranslation(graphics);
        graphics.blitSprite(getIconHolderSprite(), 0, 0, getWidth(), getHeight());
        graphics.pose().popPose();
        if (iconSprite != null) {
            renderIcon(iconSprite, graphics, canSizeIcon(), 16, 16);
        }
        renderItem(graphics);
    }
    public void renderIcon(ResourceLocation location,GuiGraphics graphics, boolean scaled, int width, int height){
        graphics.pose().pushPose();
        graphics.pose().translate(getX(), getY(),0);
        applyTranslation(graphics);
        if (scaled) {
            graphics.pose().scale(getSelectableWidth() / 16f,getSelectableHeight() / 16f,getSelectableHeight() / 16f);
        }else graphics.pose().translate((getSelectableWidth() - 16) / 2,(getSelectableHeight() - 16) / 2,0);
        graphics.blitSprite(location, 0, 0, width, height);
        graphics.pose().popPose();
    }
    public void renderItem(GuiGraphics graphics){
        if (!itemIcon.isEmpty()){
            graphics.pose().pushPose();
            graphics.pose().translate(getX(),getY(),0);
            applyTranslation(graphics);
            graphics.pose().scale(getSelectableWidth() / 16f,getSelectableHeight() / 16f,getSelectableHeight() / 16f);
            graphics.renderItem(itemIcon, 0,0);
            if (allowItemDecorations)
                graphics.renderItemDecorations(Minecraft.getInstance().font, itemIcon,0,0);
            graphics.pose().popPose();
            if (isWarning()) {
                RenderSystem.disableDepthTest();
                graphics.pose().pushPose();
                applyTranslation(graphics);
                graphics.blitSprite(WARNING_ICON,getX(),getY(),8,8);
                graphics.pose().popPose();
                RenderSystem.enableDepthTest();
            }
        }
    }
    public void renderSelection(GuiGraphics graphics){;
        graphics.pose().pushPose();
        graphics.pose().translate(getXCorner() - 4.5f, getYCorner() - 4.5f, 400f);
        applyTranslation(graphics);
        graphics.blitSprite(SELECT_ICON_HIGHLIGHT,0,0,36,36);
        graphics.pose().popPose();
    }
    public void renderHighlight(GuiGraphics graphics, int color, int h){
        graphics.pose().pushPose();
        graphics.pose().translate(getX(),getY(),0);
        applyTranslation(graphics);
        graphics.pose().scale(getSelectableWidth() / 16f,getSelectableHeight() / 16f,getSelectableHeight() / 16f);
        graphics.fillGradient(RenderType.gui(), 0, 0, 16,16, color, color, h);
        graphics.pose().popPose();
    }
    public void renderHighlight(GuiGraphics graphics, int h){
        renderHighlight(graphics,-2130706433,h);
    }
    public void renderHighlight(GuiGraphics graphics){
        renderHighlight(graphics,0);
    }
    public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, int i, int j){
        if (isHovered && !itemIcon.isEmpty()) {
            minecraft.selectMainFont(true);
            graphics.renderTooltip(minecraft.font, itemIcon, i, j);
            minecraft.selectMainFont(false);
        }

    }
    public boolean isHoveredOrFocused(){
        return isHovered || isFocused();
    }
    @Override
    public void setFocused(boolean bl) {
        focused = bl;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarrationPriority.FOCUSED;
        }
        if (this.isHovered) {
            return NarrationPriority.HOVERED;
        }
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
    public ScreenRectangle getRectangle() {
        return super.getRectangle();
    }
}
