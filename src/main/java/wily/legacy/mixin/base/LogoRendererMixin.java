package wily.legacy.mixin.base;

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
    public int renderLogoY(int y) {
        return LegacyOptions.getUIMode().isSD() ? 5 : y;
    }

    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V", at = @At("HEAD"), cancellable = true)
    public void renderLogo(GuiGraphics guiGraphics, int i, float f, int j, CallbackInfo ci) {
        int y = LegacyOptions.getUIMode().isSD() ? 5 : j;
        if (LegacyRenderUtil.hasLegacyLogo()){
            FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, f, true);
            LegacyRenderUtil.renderLegacyLogo(guiGraphics, y);
            FactoryGuiGraphics.of(guiGraphics).clearColor(true);
            ci.cancel();
        } else if (LegacyRenderUtil.getLogoScale() != 1.0f) {
            float scale = LegacyRenderUtil.getLogoScale();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(i / 2.0f - 256 * scale, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.pose().translate(-(i / 2.0f - 256), -y, 0);
        }
    }

    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V", at = @At("RETURN"))
    public void renderLogoReturn(GuiGraphics guiGraphics, int i, float f, int j, CallbackInfo ci) {
        if (!LegacyRenderUtil.hasLegacyLogo() && LegacyRenderUtil.getLogoScale() != 1.0f) {
            guiGraphics.pose().popPose();
        }
    }
}
