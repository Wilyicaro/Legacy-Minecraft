package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
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
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.client.screen.LegacySlotWidget;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyItemUtil;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen implements LegacyMenuAccess, ControlTooltip.Event {
    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;
    @Shadow
    @Final
    protected AbstractContainerMenu menu;
    @Shadow
    @Final
    protected Set<Slot> quickCraftSlots;
    @Shadow
    protected boolean isQuickCrafting;
    @Shadow
    protected int imageWidth;
    @Shadow
    protected Slot hoveredSlot;
    @Shadow
    protected int imageHeight;
    @Shadow
    private Slot clickedSlot;
    @Shadow
    private ItemStack draggingItem;
    @Shadow
    private boolean isSplittingStack;
    @Shadow
    private int quickCraftingType;
    @Shadow
    private boolean skipNextRelease;
    @Shadow
    private boolean doubleclick;
    @Unique
    private long lastUpPressedTime;

    @Unique
    private final List<LegacySlotWidget> slotWidgets = new ArrayList<>();

    protected AbstractContainerScreenMixin(Component component) {
        super(component);
    }

    @Shadow
    protected abstract void recalculateQuickCraftRemaining();

    @Shadow
    protected abstract boolean hasClickedOutside(double d, double e, int j, int k);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(AbstractContainerMenu abstractContainerMenu, Inventory inventory, Component component, CallbackInfo ci) {
        for (Slot slot : abstractContainerMenu.slots) {
            slotWidgets.add(new LegacySlotWidget(slot));
        }
        UIAccessor.of(this).addStatic(UIDefinition.createAfterInit(accessor -> {
            for (LegacySlotWidget slotWidget : slotWidgets) {
                addWidget(slotWidget);
            }
        }));
    }

    @ModifyArg(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 4)
    private int renderLabels(int i) {
        return CommonColor.INVENTORY_GRAY_TEXT.get();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (Legacy4JClient.keyCrafting.matches(keyEvent)) {
            this.onClose();
            cir.setReturnValue(true);
        }
        if (keyEvent.key() == InputConstants.KEY_W && hoveredSlot != null && hoveredSlot.hasItem() && !this.minecraft.screen.isDragging() && LegacyTipManager.setTip(LegacyTipManager.getTip(hoveredSlot.getItem().copy()))) {
            LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
        }
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void mouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        boolean downPressed = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.DOWN_BUTTON).justPressed;
        boolean upPressed = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).justPressed;
        if (Util.getMillis() - lastUpPressedTime < 250L && downPressed) this.doubleclick = false;
        if (upPressed) lastUpPressedTime = Util.getMillis();
        if (!this.skipNextRelease) {
            boolean leftPressed = Legacy4JClient.controllerManager.getButtonState(ControllerBinding.LEFT_BUTTON).justPressed;
            if (downPressed || upPressed || leftPressed) {
                this.mouseReleased(new MouseButtonEvent(Legacy4JClient.controllerManager.getPointerX(), Legacy4JClient.controllerManager.getPointerY(), new MouseButtonInfo(leftPressed ? 1 : 0, event.modifiers())));
                this.skipNextRelease = true;
            }
        }
    }

    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    private boolean mouseClickedWidgets(AbstractContainerScreen instance, MouseButtonEvent event, boolean b, Operation<Boolean> original) {
        if (original.call(instance, event, b))
            return true;
        LegacySoundUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
        return false;
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    public void mouseReleasedNoDoubleClick(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (Legacy4JClient.controllerManager.isControllerTheLastInput() && !LegacyOptions.controllerDoubleClick.get())
            this.doubleclick = false;
    }

    @Inject(method = "renderFloatingItem", at = @At(value = "HEAD"), cancellable = true)
    private void renderFloatingItem(GuiGraphics guiGraphics, ItemStack itemStack, int i, int j, String string, CallbackInfo ci) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float) Legacy4JClient.controllerManager.getPointerX() - 10, (float) Legacy4JClient.controllerManager.getPointerY() - 10);
        if (!LegacyOptions.getUIMode().isSD()) guiGraphics.pose().scale(1.5f, 1.5f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.renderItemDecorations(Minecraft.getInstance().font, itemStack, 0, (this.draggingItem.isEmpty() ? 0 : -8), string == null && this.isQuickCrafting && this.quickCraftSlots.size() > 1 && itemStack.getCount() == 1 ? String.valueOf(itemStack.getCount()) : string);
        guiGraphics.pose().popMatrix();
        ci.cancel();
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void isHovering(Slot slot, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(LegacyRenderUtil.isHovering(slot, leftPos, topPos, d, e));
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
        return slotWidgets.get(slot.index).isVisible = original && LegacySlotDisplay.of(slot).isVisible();
    }

    //?}
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void renderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        ci.cancel();
        LegacySlotWidget widget = slotWidgets.get(slot.index);
        ItemStack itemStack = slot.getItem();
        boolean bl = false;
        boolean bl2 = slot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
        ItemStack itemStack2 = this.menu.getCarried();
        String string = null;
        if (slot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemStack.isEmpty()) {
            itemStack = itemStack.copyWithCount(itemStack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !itemStack2.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                bl2 = true;
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
        widget.slotBounds(leftPos, topPos, slot, bl2 ? ItemStack.EMPTY : itemStack);
        widget.quickCraftHighlight = !bl2 && bl;
        widget.isHovered = hoveredSlot == slot;
        widget.quickCraftText = string;
        widget.itemSeed = slot.x + slot.y * this.imageWidth;
        if (widget.iconSprite == null && widget.itemIcon.isEmpty())
            widget.iconSprite = slot.getNoItemIcon();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(-leftPos, -topPos);
        widget.render(guiGraphics, 0, 0, 0);
        guiGraphics.pose().popMatrix();
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    protected void renderTooltip(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (hoveredSlot == null || !LegacySlotDisplay.isVisibleAndActive(hoveredSlot)) ci.cancel();
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/MouseButtonEvent;hasShiftDown()Z"))
    public boolean mouseClickedShift(boolean original) {
        return original || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).pressed;
    }

    @ModifyExpressionValue(method = "mouseReleased", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/MouseButtonEvent;hasShiftDown()Z"))
    public boolean mouseReleasedShift0(boolean original) {
        return original || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).released || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).justPressed;
    }

    @Inject(method = "onClose", at = @At("RETURN"))
    public void onClose(CallbackInfo ci) {
        if (LegacyItemUtil.anyArmorSlotMatch(minecraft.player.getInventory(), i -> !i.isEmpty())) {
            AnimatedCharacterRenderer.updateTime(1500);
        }
        menu.slots.forEach(s -> LegacySlotDisplay.override(s, LegacySlotDisplay.VANILLA));
    }

    @Override
    public boolean isOutsideClick(int i) {
        return hasClickedOutside(Legacy4JClient.controllerManager.getPointerX(), Legacy4JClient.controllerManager.getPointerY(), leftPos, topPos);
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
        return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public ScreenRectangle getMenuRectangleLimit() {
        return LegacyMenuAccess.createMenuRectangleLimit(this, leftPos, topPos, imageWidth, imageHeight);
    }

    @Redirect(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    public boolean renderTooltip(ItemStack instance) {
        return true;
    }
}
