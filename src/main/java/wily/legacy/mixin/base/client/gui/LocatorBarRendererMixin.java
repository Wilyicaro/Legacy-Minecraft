/*? if >=1.21.10 {*/
/*package wily.legacy.mixin.base.client.gui;

import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.util.ScreenUtil;

@Mixin(LocatorBarRenderer.class)
public class LocatorBarRendererMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V"), index = 6)
    private int legacy$applyHudOpacity(int color) {
        return ARGB.color((int) (ARGB.alpha(color) * ScreenUtil.getHUDOpacity()), ARGB.transparent(color));
    }
}
*//*?}*/
