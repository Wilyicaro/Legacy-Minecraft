package wily.legacy.mixin.base.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void render(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        LegacyRenderUtil.actualPlayerTabHeight.set(0);
        LegacyRenderUtil.actualPlayerTabWidth.set(0);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0))
    public void noHeaderBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
        LegacyRenderUtil.actualPlayerTabHeight.set(LegacyRenderUtil.actualPlayerTabHeight.get() + l-j);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1))
    public void renderBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
        LegacyRenderUtil.actualPlayerTabWidth.set(k-i+8);
        LegacyRenderUtil.actualPlayerTabHeight.set(LegacyRenderUtil.actualPlayerTabHeight.get() + l-j);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 2))
    public void noPlayerBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 3))
    public void noFooterBackground(GuiGraphics instance, int i, int j, int k, int l, int m) {
        LegacyRenderUtil.actualPlayerTabHeight.set(LegacyRenderUtil.actualPlayerTabHeight.get() + l-j);
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void renderReturn(GuiGraphics guiGraphics, int i, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0, 0);
        LegacyRenderUtil.blitTranslucentSprite(guiGraphics, LegacySprites.POINTER_PANEL, (guiGraphics.guiWidth() - LegacyRenderUtil.actualPlayerTabWidth.get()) / 2,6, LegacyRenderUtil.actualPlayerTabWidth.get(), LegacyRenderUtil.actualPlayerTabHeight.get()+8);
        guiGraphics.pose().popMatrix();
    }
}
