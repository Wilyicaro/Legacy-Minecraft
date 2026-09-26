package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.client.LegacyFeatureRenderDispatcher;

@Mixin(FeatureRenderDispatcher.class)
public class FeatureRenderDispatcherMixin implements LegacyFeatureRenderDispatcher {

    @Mutable
    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Override
    public MultiBufferSource.BufferSource getBufferSource() {
        return bufferSource;
    }

    @Override
    public void setBufferSource(MultiBufferSource.BufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }
}
