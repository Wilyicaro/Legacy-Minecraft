package wily.legacy.neoforge.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {


    @Redirect(method = "renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;IIF)V", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"))
    private void renderSlotHightlight(GuiGraphics graphics, int i, int j, int k, int color, GuiGraphics guiGraphics, Slot s) {
        ScreenUtil.iconHolderRenderer.slotBounds(s).renderHighlight(graphics, color, k);
    }
}
