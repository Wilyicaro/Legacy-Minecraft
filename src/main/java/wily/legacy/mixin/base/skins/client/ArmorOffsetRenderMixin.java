package wily.legacy.mixin.base.skins.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.skins.client.render.ArmorOffsetRenderContext;

@Mixin(ModelFeatureRenderer.class)
public abstract class ArmorOffsetRenderMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), require = 0)
    private <S> void consoleskins$pushArmorOffsets(SubmitNodeStorage.ModelSubmit<S> submit,
                                                   RenderType renderType,
                                                   VertexConsumer vertexConsumer,
                                                   OutlineBufferSource outlineBufferSource,
                                                   MultiBufferSource.BufferSource bufferSource,
                                                   CallbackInfo ci) {
        ArmorOffsetRenderContext.setRenderOffsets(((ArmorOffsetRenderContext.SubmitAccess) (Object) submit).consoleskins$getArmorOffsets());
    }

    @Inject(method = "renderModel", at = @At("RETURN"), require = 0)
    private <S> void consoleskins$popArmorOffsets(SubmitNodeStorage.ModelSubmit<S> submit,
                                                  RenderType renderType,
                                                  VertexConsumer vertexConsumer,
                                                  OutlineBufferSource outlineBufferSource,
                                                  MultiBufferSource.BufferSource bufferSource,
                                                  CallbackInfo ci) {
        ArmorOffsetRenderContext.clearRenderOffsets();
    }
}
