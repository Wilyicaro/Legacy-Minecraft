package wily.legacy.mixin.base.client.title;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(SplashRenderer.class)
public class SplashRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;", shift = At.Shift.AFTER, remap = false))
    public void renderAfterScale(GuiGraphics guiGraphics, int i, Font font, float f, CallbackInfo ci) {
        guiGraphics.pose().scale(1.5f, 1.5f);
        if (Minecraft.getInstance().getResourceManager().getResource(LegacyRenderUtil.MINECRAFT).isPresent()) guiGraphics.pose().translate(0,8);
    }
    @Inject(method = "render", at = @At("HEAD"))
    public void render(GuiGraphics guiGraphics, int i, Font font, float f, CallbackInfo ci) {
        LegacyFontUtil.legacyFont = false;
    }
    @Inject(method = "render", at = @At("RETURN"))
    public void renderReturn(GuiGraphics guiGraphics, int i, Font font, float f, CallbackInfo ci) {
        LegacyFontUtil.legacyFont = true;
    }
}
