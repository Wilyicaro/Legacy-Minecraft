//? if >=1.20.5 {
package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Panorama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(Panorama.class)
public class PanoramaRendererMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, boolean bl, CallbackInfo ci) {
        if (LegacyOptions.legacyPanorama.get()) {
            LegacyRenderUtil.renderLegacyPanorama(GuiGraphicsExtractor);
            ci.cancel();
        }
    }
}
//?}
