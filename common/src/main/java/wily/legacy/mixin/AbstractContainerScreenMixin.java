package wily.legacy.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.ScreenUtil;

import java.util.Set;

import static wily.legacy.client.screen.ControlTooltip.*;

@Mixin(AbstractContainerScreen.class)
public abstract class
AbstractContainerScreenMixin extends Screen implements LegacyMenuAccess {
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

    @Shadow protected abstract void init();

    @ModifyArg(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"), index = 4)
    private int renderLabels(int i){
        return 0x383838;
    }

    private ControlTooltip.Renderer controlTooltipRenderer = new ControlTooltip.Renderer(this).add(()-> getActiveType().isKeyboard() ? getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT,true) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(true),()->getHoveredSlot() != null && (getHoveredSlot().hasItem() || !menu.getCarried().isEmpty()) ? CONTROL_ACTION_CACHE.getUnchecked(getHoveredSlot().hasItem() && !getHoveredSlot().getItem().is(menu.getCarried().getItem()) ? menu.getCarried().isEmpty() ? "legacy.action.take" : "legacy.action.swap" : "legacy.action.place") : null).
            add(()->getActiveType().isKeyboard() ? getKeyIcon(InputConstants.KEY_ESCAPE,true) : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(true),()->CONTROL_ACTION_CACHE.getUnchecked("legacy.action.exit")).
            add(()->getActiveType().isKeyboard() ? getKeyIcon(InputConstants.MOUSE_BUTTON_RIGHT,true) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(true),()->getHoveredSlot() != null && getHoveredSlot().getItem().getCount() > 1 ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.take_half") : getHoveredSlot() != null && !menu.getCarried().isEmpty() && getHoveredSlot().hasItem() && Legacy4J.canRepair(getHoveredSlot().getItem(),menu.getCarried()) ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.repair") : null).
            add(()->getActiveType().isKeyboard() ? COMPOUND_COMPONENT_FUNCTION.apply(new Component[]{getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT,true),PLUS,getKeyIcon(InputConstants.KEY_LSHIFT,true)}) : ControllerBinding.UP_BUTTON.bindingState.getIcon(true),()-> getHoveredSlot() != null && getHoveredSlot().hasItem() ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.quick_move") : null).
            add(()->getActiveType().isKeyboard() ?  getKeyIcon(InputConstants.KEY_W,true) : ControllerBinding.RIGHT_TRIGGER.bindingState.getIcon(true),()->getHoveredSlot() != null && getHoveredSlot().hasItem() ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.whats_this") : null).
            add(()->getActiveType().isKeyboard() ?  getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT,true) : ControllerBinding.LEFT_TRIGGER.bindingState.getIcon(true),()-> menu.getCarried().getCount() > 1 ? CONTROL_ACTION_CACHE.getUnchecked("legacy.action.distribute") : null);

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void keyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir) {
        if (Legacy4JClient.keyCrafting.matches(i, j)) {
            this.onClose();
            cir.setReturnValue(true);
        }
        if (i == InputConstants.KEY_W && hoveredSlot != null && hoveredSlot.hasItem() && ScreenUtil.hasTip(hoveredSlot.getItem())) {
            ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f);
            LegacyTip oldTip = LegacyTipManager.tips.isEmpty() ? null : LegacyTipManager.tips.get(0);
            if (oldTip == null) ScreenUtil.addTip(hoveredSlot.getItem().copy());
            else oldTip.tip(ScreenUtil.getTip(hoveredSlot.getItem())).autoHeight().title(Component.translatable(hoveredSlot.getItem().getDescriptionId())).itemStack(hoveredSlot.getItem().copy());
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void slotClicked(Slot slot, int i, int j, ClickType clickType, CallbackInfo ci) {
        ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1.0f, true);
    }
    @Inject(method = "render", at = @At(value = "HEAD"))
    private void renderBackground(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        controlTooltipRenderer.render(guiGraphics, i, j, f);
    }
    @Inject(method = "renderFloatingItem", at = @At(value = "HEAD"), cancellable = true)
    private void renderFloatingItem(GuiGraphics guiGraphics, ItemStack itemStack, int i, int j, String string, CallbackInfo ci) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(i, j, 232.0f);
        guiGraphics.pose().scale(27/18f, 27/18f, 27/18f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.renderItemDecorations(Minecraft.getInstance().font, itemStack, 0, (this.draggingItem.isEmpty() ? 0 : -8), string);
        guiGraphics.pose().popPose();
        ci.cancel();
    }
    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void isHovering(Slot slot, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(ScreenUtil.isHovering(slot,leftPos,topPos,d,e));
    }
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void renderSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        ci.cancel();
        graphics.pose().pushPose();
        LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(slot);
        holder.render(graphics, 0, 0, 0);
        holder.applyOffset(graphics);
        graphics.pose().translate(slot.x,slot.y,0);
        graphics.pose().scale(holder.getSelectableWidth() / 16f,holder.getSelectableHeight() / 16f,holder.getSelectableHeight() / 16f);
        Pair<ResourceLocation, ResourceLocation> pair;
        ItemStack itemStack = slot.getItem();
        boolean bl = false;
        boolean bl2 = slot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
        ItemStack itemStack2 = this.menu.getCarried();
        String string = null;
        if (slot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemStack.isEmpty()) {
            itemStack = itemStack.copyWithCount(itemStack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !itemStack2.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                graphics.pose().popPose();
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
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 100.0f);
        if (itemStack.isEmpty() && (pair = slot.getNoItemIcon()) != null && holder.iconSprite == null) {
            TextureAtlasSprite textureAtlasSprite = minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
            graphics.blit(0, 0, 0, 16, 16, textureAtlasSprite);
            bl2 = true;
        }
        if (!bl2) {
            if (bl) {
                graphics.fill(0, 0, 16, 16, -2130706433);
            }
            graphics.renderItem(itemStack, 0, 0, slot.x + slot.y * this.imageWidth);
            graphics.renderItemDecorations(minecraft.font, itemStack, 0, 0, string);
        }
        graphics.pose().popPose();
        graphics.pose().popPose();
    }
    @Redirect(method = "mouseClicked",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(JI)Z"))
    public boolean mouseClicked(long l, int i) {
        return InputConstants.isKeyDown(l,i) || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).pressed;
    }
    @Redirect(method = "mouseReleased",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hasShiftDown()Z"))
    public boolean mouseReleased(double d, double e, int i) {
        return hasShiftDown() || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).released;
    }
    @Redirect(method = "mouseReleased",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;isKeyDown(JI)Z"))
    public boolean mouseReleased(long l, int i) {
        return InputConstants.isKeyDown(l,i) || Legacy4JClient.controllerManager.getButtonState(ControllerBinding.UP_BUTTON).released;
    }

    @Override
    public Slot getHoveredSlot() {
        return hoveredSlot;
    }

    @Override
    public ScreenRectangle getMenuRectangle() {
        return new ScreenRectangle(leftPos,topPos,width,height);
    }
    public ControlTooltip.Renderer getControlTooltipRenderer() {
        return controlTooltipRenderer;
    }

}
