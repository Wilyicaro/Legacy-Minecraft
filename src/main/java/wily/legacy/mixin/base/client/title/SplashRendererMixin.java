package wily.legacy.mixin.base.client.title;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(SplashRenderer.class)
public class SplashRendererMixin {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2f;scale(F)Lorg/joml/Matrix3x2f;"), remap = false)
    public Matrix3x2f renderAfterScale(Matrix3x2f instance, float xy, Operation<Matrix3x2f> original) {
        original.call(instance, xy);
        if (LegacyOptions.getUIMode().isSD())
            instance.scale(0.75f, 0.75f);
        else
            instance.scale(1.5f, 1.5f);
        if (Minecraft.getInstance().getResourceManager().getResource(LegacyRenderUtil.MINECRAFT).isPresent())
            return instance.translate(0, 8);
        return instance;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2f;translate(FF)Lorg/joml/Matrix3x2f;"), remap = false)
    public Matrix3x2f renderAfterTranslate(Matrix3x2f instance, float x, float y, Operation<Matrix3x2f> original) {
        original.call(instance, x, y);
        if (LegacyOptions.getUIMode().isSD()) {
            return instance.translate(-45, -30);
        }
        return instance;
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
