package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CrafterSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(CrafterScreen.class)
public abstract class CrafterScreenMixin extends AbstractContainerScreen<CrafterMenu> {



    @Shadow @Final private static Component DISABLED_SLOT_TOOLTIP;

    @Shadow @Final private static ResourceLocation POWERED_REDSTONE_LOCATION_SPRITE;

    @Shadow @Final private static ResourceLocation UNPOWERED_REDSTONE_LOCATION_SPRITE;

    public CrafterScreenMixin(CrafterMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }


    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Inject(method = "renderSlot",at = @At("HEAD"), cancellable = true)
    public void renderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        ci.cancel();
        super.renderSlot(guiGraphics, slot);
    }
    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        imageWidth = 215;
        imageHeight = 202;
        inventoryLabelX = 14;
        inventoryLabelY = 90;
        titleLabelX = (imageWidth - font.width(title)) / 2;
        titleLabelY = 11;
        super.init();
    }
    @Inject(method = "render",at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        ci.cancel();
        super.render(guiGraphics, i, j, f);
        this.renderTooltip(guiGraphics, i, j);
        if (this.hoveredSlot instanceof CrafterSlot && !this.menu.isSlotDisabled(this.hoveredSlot.index) && this.menu.getCarried().isEmpty() && !this.hoveredSlot.hasItem()) guiGraphics.renderTooltip(this.font, DISABLED_SLOT_TOOLTIP, i, j);
    }
    @Inject(method = "renderBg",at = @At("HEAD"), cancellable = true)
    public void renderBg(GuiGraphics guiGraphics, float f, int i, int j, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderPanel(guiGraphics,leftPos,topPos,imageWidth,imageHeight,2f);
        guiGraphics.blitSprite(menu.isPowered() ? POWERED_REDSTONE_LOCATION_SPRITE : UNPOWERED_REDSTONE_LOCATION_SPRITE,leftPos + 105,topPos + 43,24,24);
    }

}
