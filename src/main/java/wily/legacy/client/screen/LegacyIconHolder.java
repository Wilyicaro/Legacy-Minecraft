package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

public class LegacyIconHolder extends SimpleLayoutRenderable implements GuiEventListener, NarratableEntry, ControlTooltip.ActionHolder {
    public static final ResourceLocation ICON_HOLDER = Legacy4J.createModLocation("container/icon_holder");
    public static final ResourceLocation SIZEABLE_ICON_HOLDER = Legacy4J.createModLocation("container/sizeable_icon_holder");
    public static final ResourceLocation SELECT_ICON_HIGHLIGHT = Legacy4J.createModLocation("container/select_icon_highlight");
    public static final ResourceLocation RED_ICON_HOLDER = Legacy4J.createModLocation("container/red_icon_holder");
    public static final ResourceLocation GRAY_ICON_HOLDER = Legacy4J.createModLocation("container/gray_icon_holder");
    public static final ResourceLocation WARNING_ICON = Legacy4J.createModLocation("container/icon_warning");
    public static final ResourceLocation SLOT_HIGHLIGHT = Legacy4J.createModLocation("container/slot_highlight");

    public static final ResourceLocation MOJANGLES_11_FONT = Legacy4J.createModLocation("default_11");
    
    public Vec3 offset = Vec3.ZERO;
    public ResourceLocation iconSprite = null;
    public ArbitrarySupplier<ResourceLocation> iconHolderOverride = null;
    @NotNull
    public ItemStack itemIcon = ItemStack.EMPTY;

    public boolean allowItemDecorations = true;
    public boolean allowFocusedItemTooltip = false;
    private boolean isWarning = false;
    public boolean isHovered;
    private boolean focused = false;

    public LegacyIconHolder(){}

    public static LegacyIconHolder fromSlot(Slot slot){
        return new LegacyIconHolder(){
            @Override
            public void render(GuiGraphics graphics, int i, int j, float f) {
                slotBoundsWithItem(0, 0, slot);
                super.render(graphics, i, j, f);
            }
        };
    }


    public LegacyIconHolder(int leftPos, int topPos, Slot slot){
        slotBounds(leftPos, topPos, slot);
    }

    public LegacyIconHolder(int x, int y, int width, int height){
        this(width,height);
        setPos(x,y);
    }

    public LegacyIconHolder(int width, int height){
        setBounds(width,height);
    }

    public void setBounds(int width, int height){
        this.width = width;
        this.height = height;
    }

    public void setPos(int x, int y){
        setX(x);
        setY(y);
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        return isHovered;
    }

    public LegacyIconHolder slotBoundsWithItem(int leftPos, int topPos, Slot slot){
        return slotBounds(leftPos, topPos, slot, slot.getItem());
    }

    public LegacyIconHolder slotBounds(Slot slot){
        return slotBounds(0,0,slot);
    }

    public LegacyIconHolder slotBounds(int leftPos, int topPos, Slot slot){
        return slotBounds(leftPos, topPos, slot, ItemStack.EMPTY);
    }
    public LegacyIconHolder slotBounds(int leftPos, int topPos, Slot slot, ItemStack stack){
        return itemHolder(leftPos + slot.x,topPos + slot.y, LegacySlotDisplay.of(slot).getWidth(),LegacySlotDisplay.of(slot).getHeight(), stack, LegacySlotDisplay.of(slot).isWarning(), LegacySlotDisplay.of(slot).getIconSprite(), LegacySlotDisplay.of(slot).getOffset(), LegacySlotDisplay.of(slot).getIconHolderOverride());
    }

    public LegacyIconHolder itemHolder(ItemStack itemIcon, boolean isWarning){
        return itemHolder(itemIcon,isWarning,Vec3.ZERO);
    }

    public LegacyIconHolder itemHolder(ItemStack itemIcon, boolean isWarning, Vec3 offset){
        return itemHolder(0,0,21,21,itemIcon,isWarning,offset);
    }

    public LegacyIconHolder itemHolder(int x, int y, int width, int height, ItemStack itemIcon, boolean isWarning, Vec3 offset){
        return itemHolder(x,y,width,height,itemIcon,isWarning,null,offset,null);
    }

    public LegacyIconHolder itemHolder(int x, int y, int width, int height, ItemStack itemIcon, boolean isWarning, ResourceLocation iconSprite, Vec3 offset, ArbitrarySupplier<ResourceLocation> override){
        setPos(x,y);
        setBounds(width,height);
        this.iconSprite = iconSprite;
        this.iconHolderOverride = override;
        this.itemIcon = itemIcon;
        this.isWarning = isWarning;
        this.allowItemDecorations = true;
        this.offset = offset;
        return this;
    }

    public static LegacyIconHolder entityHolder(int x, int y, int width, int height, EntityType<?> entityType){
        return new LegacyIconHolder(x, y, width, height) {
            Entity entity;

            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                super.render(graphics, mouseX, mouseY, delta);
                if (entity == null && Minecraft.getInstance().level != null){
                    entity = entityType.create(Minecraft.getInstance().level, EntitySpawnReason.EVENT);
                }
                if (entity != null) renderEntity(graphics, entity, mouseX, mouseY, delta);
            }
        };
    }

    public double getMiddleX(){
        return getXCorner() + offset.x() + getWidth() / 2f;
    }

    public double getMiddleY(){
        return getYCorner() + offset.y() + getHeight() / 2f;
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

    public int getItemRenderSize() {
        return Math.round(Math.min(getSelectableWidth(), getSelectableHeight()));
    }

    public boolean isSizeable(){
        return Math.min(getWidth(),getHeight()) < 18 && LegacyRenderUtil.is720p();
    }
    public boolean canSizeIcon(){
        return Math.min(getWidth(),getHeight()) > 21;
    }

    public void applyOffset(GuiGraphics graphics){
        if (!offset.equals(Vec3.ZERO)) graphics.pose().translate((float) offset.x, (float) offset.y);
    }
    public boolean isWarning(){
        return isWarning;
    }
    public void setWarning(boolean warning){
        this.isWarning = warning;
    }
    public ResourceLocation getIconHolderSprite(){
        return iconHolderOverride == null ? isWarning() ? RED_ICON_HOLDER : isSizeable() ? SIZEABLE_ICON_HOLDER : ICON_HOLDER : iconHolderOverride.get();
    }

    @Override
    public void render(GuiGraphics graphics, int i, int j, float f) {
        isHovered = LegacyRenderUtil.isMouseOver(i, j, getXCorner(), getYCorner(), width, height);
        ResourceLocation sprite = getIconHolderSprite();
        if (sprite != null)
            renderChild(graphics,getXCorner(),getYCorner(),()->FactoryGuiGraphics.of(graphics).blitSprite(sprite, 0, 0, getWidth(), getHeight()));
        if (iconSprite != null) {
            renderIcon(iconSprite, graphics, canSizeIcon(), 16, 16);
        }
        renderItem(graphics,i,j,f);
    }

    public void renderIcon(ResourceLocation location,GuiGraphics graphics, boolean scaled, int width, int height){
        renderChild(graphics,getX(),getY(),()->{
            FactoryGuiGraphics.of(graphics).disableDepthTest();
            if (scaled) {
                graphics.pose().scale(getSelectableWidth() / width,getSelectableHeight() / height);
            }else graphics.pose().translate((getSelectableWidth() - width) / 2,(getSelectableHeight() - height) / 2);
            FactoryGuiGraphics.of(graphics).blitSprite(location, 0, 0, width, height);
            FactoryGuiGraphics.of(graphics).enableDepthTest();
        });
    }

    public void renderItem(GuiGraphics graphics, int i, int j, float f){
        renderItem(graphics, itemIcon, getX(), getY(), isWarning());
    }

    public void renderItem(GuiGraphics graphics, ItemStack item, int x, int y, boolean isWarning){
        if (!item.isEmpty()) renderItem(graphics,()->{
            graphics.renderFakeItem(item, 0,0);
            if (allowItemDecorations)
                graphics.renderItemDecorations(Minecraft.getInstance().font, item,0,0);
        },x,y,isWarning);
    }

    public void renderItem(GuiGraphics graphics, Runnable itemRender, int x, int y, boolean isWarning){
        renderScaled(graphics,x,y,itemRender);
        if (isWarning) renderWarning(graphics);
    }

    public void renderWarning(GuiGraphics graphics, float z){
        renderChild(graphics,x,y,()->{
            FactoryGuiGraphics.of(graphics).disableDepthTest();
            graphics.pose().translate(0,0);
            FactoryGuiGraphics.of(graphics).blitSprite(WARNING_ICON,0,0,8,8);
            FactoryGuiGraphics.of(graphics).enableDepthTest();
        });
    }

    public void renderWarning(GuiGraphics graphics){
        renderWarning(graphics, 332);
    }

    public void renderEntity(GuiGraphics graphics, Entity entity, int mouseX, int mouseY, float deltaTime){
        entity.setYRot(180);
        entity.yRotO = entity.getYRot();
        entity.setXRot(entity.xRotO = 0);
        if (entity instanceof LivingEntity e) {
            e.yBodyRotO = e.yBodyRot = 180.0f;
            e.yHeadRot = 180;
            e.yHeadRotO = e.yHeadRot;
        }
        LegacyRenderUtil.renderEntity(graphics, getX(), getY(), getX() + Math.round(getSelectableWidth()), getY() + Math.round(getSelectableHeight()*2), (int)Math.min(getSelectableWidth(),getSelectableHeight()), new Vector3f(), new Quaternionf().rotationXYZ(0.0f, (float) Math.PI/ 4, (float) Math.PI), null, entity,true);
    }

    public void renderSelection(GuiGraphics graphics, int i, int j, float f){
        renderChild(graphics,getXCorner() - 4.5f, getYCorner() - 4.5f,()-> {
            FactoryGuiGraphics.of(graphics).disableDepthTest();
            FactoryGuiGraphics.of(graphics).blitSprite(SELECT_ICON_HIGHLIGHT,0,0,36,36);
            FactoryGuiGraphics.of(graphics).enableDepthTest();
        });
    }

    public void renderScaled(GuiGraphics graphics, float x, float y, Runnable render){
        renderChild(graphics,x,y,()->{
            graphics.pose().scale(getSelectableWidth() / 16f,getSelectableHeight() / 16f);
            render.run();
        });
    }

    public void renderChild(GuiGraphics graphics, float x, float y, Runnable render){
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        applyOffset(graphics);
        render.run();
        graphics.pose().popMatrix();
    }

    public void renderHighlight(GuiGraphics graphics){
        renderScaled(graphics,getX(),getY(),()-> {
            FactoryScreenUtil.enableBlend();
            FactoryGuiGraphics.of(graphics).blitSprite(SLOT_HIGHLIGHT, 0, 0, 16, 16);
            FactoryScreenUtil.disableBlend();
        });
    }

    public void renderTooltip(Minecraft minecraft, GuiGraphics graphics,int i, int j){
        if (isHovered || (allowFocusedItemTooltip && isFocused())) renderTooltip(minecraft,graphics,itemIcon, !isHovered ? (int) getMiddleX() : i,!isHovered ? (int) getMiddleY() : j);
    }

    public void renderTooltip(Minecraft minecraft, GuiGraphics graphics,ItemStack stack, int i, int j){
        if (!stack.isEmpty()) Legacy4JClient.applyFontOverrideIf(LegacyRenderUtil.is720p(),MOJANGLES_11_FONT, b->graphics.setTooltipForNextFrame(minecraft.font, stack, i, j));
    }

    public boolean isHoveredOrFocused(){
        return isHovered || isFocused();
    }

    @Override
    public void setFocused(boolean bl) {
        focused = bl;
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (CommonInputs.selected(i)){
            onPress();
        }
        return false;
    }

    public boolean mouseClicked(double d, double e, int i) {
        if (isMouseOver(d,e) && i == 0) {
            onClick(d,e);
            return !isFocused();
        }
        return false;
    }
    public void playClickSound(){
        if (!isFocused()) LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0F);
    }
    public void onClick(double d, double e){
        playClickSound();
        onPress();
    }
    public void onPress(){

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
    public boolean isMouseOver(double d, double e) {
        return isHovered;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
    public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
        return !this.isFocused() ? ComponentPath.leaf(this) : null;
    }
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle((int)getXCorner(),(int)getYCorner(),getWidth(),getHeight());
    }

    public int getMinSize() {
        return Math.min(getWidth(),getHeight());
    }

    @Override
    public @Nullable Component getAction(Context context) {
        return ControlTooltip.getSelectAction(this,context);
    }
}
