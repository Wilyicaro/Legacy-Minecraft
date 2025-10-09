package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyFeatureRenderDispatcher;
import wily.legacy.client.LoyaltyLinesRenderer;

@Mixin(FeatureRenderDispatcher.class)
public class FeatureRenderDispatcherMixin implements LegacyFeatureRenderDispatcher {

    @Mutable
    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;

    @Inject(method = "renderAllFeatures", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer;render(Lnet/minecraft/client/renderer/SubmitNodeCollection;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/resources/model/AtlasManager;)V"))
    private void renderLoyaltyLinesFeatures(CallbackInfo ci, @Local SubmitNodeCollection submitNodeCollection) {
        LoyaltyLinesRenderer.render(submitNodeCollection, bufferSource);
    }

    @Override
    public void setBufferSource(MultiBufferSource.BufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    @Override
    public MultiBufferSource.BufferSource getBufferSource() {
        return bufferSource;
    }
}
