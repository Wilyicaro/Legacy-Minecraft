package wily.legacy.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.inventory.LegacySlotWrapper;
import wily.legacy.util.ScreenUtil;

import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow protected int leftPos;

    @Shadow protected int topPos;

    @Shadow  private Slot clickedSlot;

    @Shadow private ItemStack draggingItem;

    @Shadow @Final protected AbstractContainerMenu menu;

    @Shadow @Final protected Set<Slot> quickCraftSlots;

    @Shadow protected boolean isQuickCrafting;

    @Shadow private boolean isSplittingStack;

    @Shadow private int quickCraftingType;

    @Shadow protected abstract void recalculateQuickCraftRemaining();

    @Shadow protected int imageWidth;

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
        LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(slot);
        int width = holder.getWidth();
        int height = holder.getHeight();
        double xCorner = holder.getXCorner() + (holder.translation != null ? holder.translation.x : 0);
        double yCorner = holder.getYCorner() + (holder.translation != null ? holder.translation.y : 0);
        cir.setReturnValue((d -= leftPos) >= xCorner && d < (xCorner + width) && (e -= topPos) >= yCorner && e < (yCorner + height));
    }
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void renderSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        ci.cancel();
        Minecraft minecraft = Minecraft.getInstance();
        graphics.pose().pushPose();
        LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(slot);
        if (slot instanceof LegacySlotWrapper wrapper) {
            if (wrapper.hasIconHolder())
                holder.render(graphics, 0, 0, 0);
            holder.applyTranslation(graphics);
        }
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
        if (itemStack.isEmpty() && (pair = slot.getNoItemIcon()) != null) {
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

}
