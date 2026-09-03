package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
//? if >=26.2 {
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;

import java.util.List;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.LegacyNameTag;
import wily.legacy.client.LegacyOptions;

@Mixin(NameTagFeatureRenderer.class)
public abstract class NameTagFeatureMixin
        //? if >=26.2 {
        extends RenderTypeFeatureRenderer<NameTagFeatureRenderer.Submit>
        //?}
{
    //? if >=26.2 {
    // 26.2 replaced NameTagFeatureRenderer#renderTranslucent(SubmitNodeCollection, BufferSource, Font)
    // with buildGroup(FeatureFrameContext, List<Submit>), and Font#drawInBatch with
    // prepareText(...) + PreparedText#visit(GlyphVisitor). The old see-through/normal split is gone:
    // a single submit now carries its own displayMode, so one injector covers both cases.
    @Inject(method = "buildGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font$PreparedText;visit(Lnet/minecraft/client/gui/Font$GlyphVisitor;)V", shift = At.Shift.AFTER))
    protected void legacy$nameTagOutline(FeatureFrameContext context, List<NameTagFeatureRenderer.Submit> submits, CallbackInfo ci, @Local NameTagFeatureRenderer.Submit submit) {
        LegacyNameTag.renderNameTagOutline(context.font(), getVertexBuilder(LegacyNameTag.outlineRenderType(submit)), submit);
    }

    // backgroundColor is argument 6 of Font#prepareText(FormattedCharSequence,float,float,int,boolean,boolean,int)
    @ModifyArg(method = "prepareText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"), index = 6)
    private static int legacy$nameTagBackground(int original, @Local(argsOnly = true) NameTagFeatureRenderer.Submit submit) {
        float thickness = LegacyNameTag.getThickness(submit.distanceToCameraSq());
        float[] color = thickness < 1 || !LegacyOptions.displayNameTagBorder.get() ? null : LegacyNameTag.of(submit).getNameTagColor();

        return color == null ? original : ColorUtil.colorFromFloat(color[0], color[1], color[2], 1.0f);
    }
    //?} else {
    /*@Inject(method = "renderTranslucent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V", shift = At.Shift.AFTER, ordinal = 0))
    protected void renderNameTagSeeThrough(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, Font font, CallbackInfo ci, @Local SubmitNodeStorage.NameTagSubmit submit) {
        LegacyNameTag.renderNameTagOutline(font, bufferSource, submit, true);
    }

    @Inject(method = "renderTranslucent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V", shift = At.Shift.AFTER, ordinal = 1))
    protected void renderNameTagSee(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, Font font, CallbackInfo ci, @Local SubmitNodeStorage.NameTagSubmit submit) {
        LegacyNameTag.renderNameTagOutline(font, bufferSource, submit, false);
    }

    @ModifyArg(method = "renderTranslucent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4fc;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V", ordinal = 0), index = 8)
    protected int renderNameTag(int original, @Local SubmitNodeStorage.NameTagSubmit submit) {
        float thickness = LegacyNameTag.getThickness(submit.distanceToCameraSq());
        float[] color = thickness < 1 || !LegacyOptions.displayNameTagBorder.get() ? null : LegacyNameTag.of(submit).getNameTagColor();

        return color == null ? original : ColorUtil.colorFromFloat(color[0], color[1], color[2], 1.0f);
    }
    *///?}
}
