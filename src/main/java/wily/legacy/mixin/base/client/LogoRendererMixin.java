package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphics;
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
    @ModifyArg(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V"), index = 3)
    public int renderLogo(int i) {
        return LegacyOptions.getUIMode().isSD() ? 5 : i;
    }

    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V", at = @At("HEAD"), cancellable = true)
    public void renderLogo(GuiGraphics guiGraphics, int i, float f, int j, CallbackInfo ci) {
        if (LegacyRenderUtil.hasLegacyLogo()) {
            FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f, 1.0f, 1.0f, f);
            LegacyRenderUtil.renderLegacyLogo(guiGraphics, j);
            FactoryGuiGraphics.of(guiGraphics).clearBlitColor();
            ci.cancel();
        } else if (LegacyRenderUtil.getLogoScale() != 1.0f) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(i / 2 - 256 * LegacyRenderUtil.getLogoScale(), j);
            guiGraphics.pose().scale(LegacyRenderUtil.getLogoScale(), LegacyRenderUtil.getLogoScale());
            guiGraphics.pose().translate(-(i / 2 - 256), -j);
        }
    }

    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V", at = @At("RETURN"))
    public void renderLogoReturn(GuiGraphics guiGraphics, int i, float f, int j, CallbackInfo ci) {
        if (!LegacyRenderUtil.hasLegacyLogo() && LegacyRenderUtil.getLogoScale() != 1.0f) {
            guiGraphics.pose().popMatrix();
        }
    }
}
