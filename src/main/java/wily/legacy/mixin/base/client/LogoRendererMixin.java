package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(LogoRenderer.class)
public class LogoRendererMixin {
    @ModifyArg(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V"), index = 3)
    public int renderLogo(int i) {
        return LegacyOptions.getUIMode().isSD() ? 5 : i;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V", at = @At("HEAD"), cancellable = true)
    public void renderLogo(GuiGraphicsExtractor GuiGraphicsExtractor, int i, float f, int j, CallbackInfo ci) {
        if (LegacyRenderUtil.hasLegacyLogo()) {
            FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, f);
            LegacyRenderUtil.renderLegacyLogo(GuiGraphicsExtractor, j);
            FactoryGuiGraphics.of(GuiGraphicsExtractor).clearBlitColor();
            ci.cancel();
        } else if (LegacyRenderUtil.getLogoScale() != 1.0f) {
            GuiGraphicsExtractor.pose().pushMatrix();
            GuiGraphicsExtractor.pose().translate(i / 2 - 256 * LegacyRenderUtil.getLogoScale(), j);
            GuiGraphicsExtractor.pose().scale(LegacyRenderUtil.getLogoScale(), LegacyRenderUtil.getLogoScale());
            GuiGraphicsExtractor.pose().translate(-(i / 2 - 256), -j);
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V", at = @At("RETURN"))
    public void renderLogoReturn(GuiGraphicsExtractor GuiGraphicsExtractor, int i, float f, int j, CallbackInfo ci) {
        if (!LegacyRenderUtil.hasLegacyLogo() && LegacyRenderUtil.getLogoScale() != 1.0f) {
            GuiGraphicsExtractor.pose().popMatrix();
        }
    }
}
