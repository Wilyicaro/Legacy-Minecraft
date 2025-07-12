//? if >=1.20.5 {
package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(PanoramaRenderer.class)
public class PanoramaRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, boolean bl, CallbackInfo ci) {
        if (LegacyOptions.legacyPanorama.get()){
            LegacyRenderUtil.renderLegacyPanorama(guiGraphics);
            ci.cancel();
        }
    }
}
//?}
