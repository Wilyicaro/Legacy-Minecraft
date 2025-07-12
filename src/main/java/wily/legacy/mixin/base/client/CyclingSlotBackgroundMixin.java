package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(CyclingSlotBackground.class)
public class CyclingSlotBackgroundMixin {
    @Inject(method = "renderIcon", at = @At("HEAD"), cancellable = true)
    private void renderIcon(Slot slot, ResourceLocation resourceLocation, float f, GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        ci.cancel();
        LegacyIconHolder holder = LegacyRenderUtil.iconHolderRenderer.slotBounds(slot);
        holder.renderScaled(guiGraphics, i + slot.x,j + slot.y, ()-> {
            FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f,1.0f,1.0f, f);
            FactoryGuiGraphics.of(guiGraphics).blitSprite(resourceLocation, 0, 0, 16, 16);
            FactoryGuiGraphics.of(guiGraphics).clearBlitColor();
        });
    }
}
