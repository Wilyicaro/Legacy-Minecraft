package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(CyclingSlotBackground.class)
public class CyclingSlotBackgroundMixin {
    @Inject(method = "extractIcon", at = @At("HEAD"), cancellable = true)
    private void extractIcon(Slot slot, Identifier resourceLocation, float f, GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.slotBounds(slot);
        holder.renderScaled(GuiGraphicsExtractor, i + slot.x, j + slot.y, () -> {
            FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, f);
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(resourceLocation, 0, 0, 16, 16);
            FactoryGuiGraphics.of(GuiGraphicsExtractor).clearBlitColor();
        });
    }
}
