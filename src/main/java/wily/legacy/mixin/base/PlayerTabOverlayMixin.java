package wily.legacy.mixin.base;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.ScreenUtil;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0))
    public void noHeaderBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1))
    public void renderBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
        ScreenUtil.renderPointerPanel(instance,i-4,j-12,k-i+8,l-j+24);
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 2))
    public void noPlayerBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 3))
    public void noFooterBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
    }
}
