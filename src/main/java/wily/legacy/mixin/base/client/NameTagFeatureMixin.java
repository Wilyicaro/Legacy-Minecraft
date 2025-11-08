package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.LegacyNameTag;
import wily.legacy.client.LegacyOptions;

@Mixin(NameTagFeatureRenderer.class)
public abstract class NameTagFeatureMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V", shift = At.Shift.AFTER, ordinal = 0))
    protected void renderNameTagSeeThrough(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, Font font, CallbackInfo ci, @Local SubmitNodeStorage.NameTagSubmit submit) {
        LegacyNameTag.renderNameTagOutline(font, bufferSource, submit, true);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V", shift = At.Shift.AFTER, ordinal = 0))
    protected void renderNameTagSee(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, Font font, CallbackInfo ci, @Local SubmitNodeStorage.NameTagSubmit submit) {
        LegacyNameTag.renderNameTagOutline(font, bufferSource, submit, false);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V", ordinal = 0), index = 9)
    protected int renderNameTag(int original, @Local SubmitNodeStorage.NameTagSubmit submit) {
        float thickness = LegacyNameTag.getThickness(submit.distanceToCameraSq());
        float[] color = thickness < 1 || !LegacyOptions.displayNameTagBorder.get() ? null : LegacyNameTag.of(submit).getNameTagColor();

        return color == null ? original : ColorUtil.colorFromFloat(color[0], color[1], color[2], 1.0f);
    }
}
