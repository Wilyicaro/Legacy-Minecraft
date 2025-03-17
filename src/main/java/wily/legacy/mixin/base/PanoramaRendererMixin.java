//? if >=1.20.5 {
package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

@Mixin(PanoramaRenderer.class)
public class PanoramaRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, int i, int j, float f, float g, CallbackInfo ci) {
        if (LegacyOptions.legacyPanorama.get()){
            ScreenUtil.renderLegacyPanorama(guiGraphics);
            ci.cancel();
        }
    }
}
//?}
