package wily.legacy.forge.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow protected int leftPos;

    @Shadow protected int topPos;
    @Shadow protected Slot hoveredSlot;

    @Redirect(method = "render", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V",remap = false))
    private void renderSlotHightlight(GuiGraphics graphics, int i, int j, int k, int color) {
        graphics.pose().pushPose();
        LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(hoveredSlot);
        graphics.pose().translate(-leftPos,-topPos,0);
        graphics.pose().scale(holder.getSelectableWidth() / 16f,holder.getSelectableHeight() / 16f,holder.getSelectableHeight() / 16f);
        graphics.pose().translate((leftPos + i) * 16f / holder.getSelectableWidth() ,(topPos + j) * 16f / holder.getSelectableHeight(),0);
        graphics.fillGradient(RenderType.guiOverlay(), 0, 0, 16,16, color, color, k);
        graphics.pose().popPose();
    }
}
