package wily.legacy.mixin.base.client;

//? if >=26.2 {
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import wily.legacy.client.LegacyItemTranslucency;

// Replaces the pre-26.2 trick of swapping FeatureRenderDispatcher's MultiBufferSource for a wrapper.
// 26.2 has no buffer source; every feature renderer instead obtains its VertexConsumer here, so this
// is the single point at which the translucent-item remap can still be applied.
@Mixin(RenderTypeFeatureRenderer.class)
public class RenderTypeFeatureRendererMixin {
    @ModifyVariable(method = "getVertexBuilder", at = @At("HEAD"), argsOnly = true)
    private RenderType legacy$remapTranslucentItemRenderType(RenderType renderType) {
        return LegacyItemTranslucency.remapRenderType(renderType);
    }

    @ModifyReturnValue(method = "getVertexBuilder", at = @At("RETURN"))
    private VertexConsumer legacy$tintTranslucentItemVertices(VertexConsumer original) {
        return LegacyItemTranslucency.wrapVertexConsumer(original);
    }
}
//?}
