//? if >=26.1 {
package wily.legacy.mixin.base.client.gui;

import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(LocatorBarRenderer.class)
public class LocatorBarRendererMixin {

    @ModifyArg(method = "lambda$extractRenderState$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"), index = 6)
    private int extractRenderState(int color) {
        return ARGB.color((int) (ARGB.alpha(color) * LegacyRenderUtil.getHUDOpacity()), ARGB.transparent(color));
    }
}
//?}
