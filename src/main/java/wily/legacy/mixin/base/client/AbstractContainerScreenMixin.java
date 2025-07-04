package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyItemUtil;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen implements LegacyMenuAccess,ControlTooltip.Event {
    @Shadow protected int leftPos;

    @Shadow protected int topPos;

    @Shadow  private Slot clickedSlot;

    @Shadow private ItemStack draggingItem;

    @Shadow @Final protected AbstractContainerMenu menu;

    @Shadow @Final protected Set<Slot> quickCraftSlots;

    @Shadow protected boolean isQuickCrafting;

    @Shadow private boolean isSplittingStack;

    @Shadow private int quickCraftingType;

    protected AbstractContainerScreenMixin(Component component) {
        super(component);
    }

    @Shadow protected abstract void recalculateQuickCraftRemaining();

    @Shadow protected int imageWidth;

    @Shadow protected Slot hoveredSlot;

    @Shadow protected int imageHeight;

    @Shadow protected abstract boolean hasClickedOutside(double d, double e, int i, int j, int k);

    @Shadow private boolean skipNextRelease;

    @Shadow private boolean doubleclick;

    @Unique private long lastUpPressedTime;

    @ModifyArg(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 4)
    private int renderLabels(int i){
        return CommonColor.INVENTORY_GRAY_TEXT.get();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir) {
        if (Legacy4JClient.keyCrafting.matches(i, j)) {
            this.onClose();
            cir.setReturnValue(true);
        }
        if (i == InputConstants.KEY_W && hoveredSlot != null && hoveredSlot.hasItem() && !this.minecraft.screen.isDragging() && LegacyTipManager.setTip(LegacyTipManager.getTip(hoveredSlot.getItem().copy()))) {
            LegacyRenderUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
        }
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void mouseClicked(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        if (getChildAt(d,e).isEmpty()) LegacyRenderUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);

        boolean downPressed = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.DOWN_BUTTON).justPressed;
        boolean upPressed = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).justPressed;
        if (Util.getMillis() - lastUpPressedTime < 250L && downPressed) this.doubleclick = false;
        if (upPressed) lastUpPressedTime = Util.getMillis();
        if (!this.skipNextRelease) {
            boolean leftPressed = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.LEFT_BUTTON).justPressed;
            if (downPressed || upPressed || leftPressed) {
                this.mouseReleased(Legacy4JClient.controllerManager.getPointerX(), Legacy4JClient.controllerManager.getPointerY(), leftPressed ? 1 : 0);
                this.skipNextRelease = true;
            }
        }
    }
    @Inject(method = "mouseReleased", at = @At("HEAD"))
    public void mouseReleasedNoDoubleClick(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        if (Legacy4JClient.controllerManager.isControllerTheLastInput() && !LegacyOptions.controllerDoubleClick.get())
            this.doubleclick = false;
    }

    @Inject(method = "renderFloatingItem", at = @At(value = "HEAD"), cancellable = true)
    private void renderFloatingItem(GuiGraphics guiGraphics, ItemStack itemStack, int i, int j, String string, CallbackInfo ci) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float)Legacy4JClient.controllerManager.getPointerX() - i - 10, (float)Legacy4JClient.controllerManager.getPointerY() - j - 10);
        guiGraphics.pose().scale(27/18f, 27/18f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.renderItemDecorations(Minecraft.getInstance().font, itemStack, 0, (this.draggingItem.isEmpty() ? 0 : -8), string == null && this.isQuickCrafting && this.quickCraftSlots.size() > 1 && itemStack.getCount() == 1 ? String.valueOf(itemStack.getCount()) : string);
        guiGraphics.pose().popMatrix();
        ci.cancel();
    }
    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void isHovering(Slot slot, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(LegacyRenderUtil.isHovering(slot,leftPos,topPos,d,e));
    }
    //? if <1.21.2 {
    /*@ModifyExpressionValue(method = /^? if neoforge {^//^"renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;IIF)V"^//^?} else {^/"render"/^?}^/, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;isHighlightable()Z"))
    private boolean renderSlotHighlight(boolean original) {
        return false;
    }
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;isActive()Z"))
    private boolean renderSlots(boolean original, @Local Slot slot) {
        return original && LegacySlotDisplay.of(slot).isVisible();
    }
    *///?} else {
    @Inject(method = "renderSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void renderSlotHighlightFront(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
    }
    @Inject(method = "renderSlotHighlightBack", at = @At("HEAD"), cancellable = true)
    private void renderSlotHighlightBack(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
    }

    @ModifyExpressionValue(method = "renderSlots", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;isActive()Z"))
    private boolean renderSlots(boolean original, @Local Slot slot) {
        return original && LegacySlotDisplay.of(slot).isVisible();
    }
    //?}
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void renderSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        ci.cancel();
        LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.slotBounds(slot);
        holder.render(graphics, 0, 0, 0);
        if (LegacyRenderUtil.isHovering(slot,leftPos,topPos,Legacy4JClient.controllerManager.getPointerX(),Legacy4JClient.controllerManager.getPointerY())) holder.renderHighlight(graphics);
        graphics.pose().pushMatrix();
        holder.applyOffset(graphics);
        graphics.pose().translate(slot.x, slot.y);
        graphics.pose().scale(holder.getSelectableWidth() / 16f,holder.getSelectableHeight() / 16f);

        ItemStack itemStack = slot.getItem();
        boolean bl = false;
        boolean bl2 = slot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
        ItemStack itemStack2 = this.menu.getCarried();
        String string = null;
        if (slot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemStack.isEmpty()) {
            itemStack = itemStack.copyWithCount(itemStack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !itemStack2.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                graphics.pose().popMatrix();
                return;
            }
            if (AbstractContainerMenu.canItemQuickReplace(slot, itemStack2, true) && this.menu.canDragTo(slot)) {
                bl = true;
                int k = Math.min(itemStack2.getMaxStackSize(), slot.getMaxStackSize(itemStack2));
                int l = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                int m = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, itemStack2) + l;
                if (m > k) {
                    m = k;
                    string = ChatFormatting.YELLOW.toString() + k;
                }
                itemStack = itemStack2.copyWithCount(m);
            } else {
                this.quickCraftSlots.remove(slot);
                this.recalculateQuickCraftRemaining();
            }
        }

        graphics.nextStratum();
        //? if <1.21.4 {
        /*Pair<ResourceLocation, ResourceLocation> pair;
        if (itemStack.isEmpty() && (pair = slot.getNoItemIcon()) != null && holder.iconSprite == null) {
            TextureAtlasSprite textureAtlasSprite = minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
            FactoryGuiGraphics.of(graphics).blit(0, 0, 0, 16, 16, textureAtlasSprite);
            bl2 = true;
        }
        *///?} else {
        if (itemStack.isEmpty() && slot.getNoItemIcon() != null && holder.iconSprite == null) {
            FactoryGuiGraphics.of(graphics).blitSprite(slot.getNoItemIcon(), 0, 0, 16, 16);
            bl2 = true;
        }
        //?}
        if (!bl2) {
            if (bl) {
                graphics.fill(0, 0, 16, 16, -2130706433);
            }
            graphics.renderItem(itemStack, 0, 0, slot.x + slot.y * this.imageWidth);
            graphics.renderItemDecorations(minecraft.font, itemStack, 0, 0, string);
        }
        graphics.pose().popMatrix();

        if (holder.isWarning()) holder.renderWarning(graphics, 600);
    }
    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    protected void renderTooltip(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (hoveredSlot == null || !LegacySlotDisplay.isVisibleAndActive(hoveredSlot)) ci.cancel();
    }
    @Redirect(method = "mouseClicked",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(JI)Z"))
    public boolean mouseClickedShift(long l, int i) {
        return InputConstants.isKeyDown(l,i) || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).pressed;
    }
    @Redirect(method = "mouseReleased",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hasShiftDown()Z"))
    public boolean mouseReleasedShift0(double d, double e, int i) {
        return hasShiftDown() || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).released || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).justPressed;
    }
    @Redirect(method = "mouseReleased",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(JI)Z"))
    public boolean mouseReleasedShift1(long l, int i) {
        return InputConstants.isKeyDown(l,i) || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).released || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).justPressed;
    }

    @Inject(method = "onClose",at = @At("RETURN"))
    public void onClose(CallbackInfo ci) {
        if (LegacyItemUtil.anyArmorSlotMatch(minecraft.player.getInventory(), i-> !i.isEmpty())) {
            LegacyRenderUtil.updateAnimatedCharacterTime(1500);
        }
        menu.slots.forEach(s-> LegacySlotDisplay.override(s, LegacySlotDisplay.VANILLA));
    }

    @Override
    public boolean isOutsideClick(int i) {
        return hasClickedOutside(Legacy4JClient.controllerManager.getPointerX(),Legacy4JClient.controllerManager.getPointerY(),leftPos,topPos,i);
    }

    @Override
    public boolean isMouseDragging() {
        return this.isDragging();
    }

    @Override
    public Slot getHoveredSlot() {
        return hoveredSlot;
    }

    @Override
    public ScreenRectangle getMenuRectangle() {
        return new ScreenRectangle(leftPos,topPos,imageWidth,imageHeight);
    }

    @Redirect(method = "renderTooltip",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    public boolean renderTooltip(ItemStack instance) {
        return true;
    }
}
