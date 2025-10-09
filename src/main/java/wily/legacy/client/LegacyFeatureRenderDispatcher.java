package wily.legacy.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

public interface LegacyFeatureRenderDispatcher {
    static LegacyFeatureRenderDispatcher of(FeatureRenderDispatcher dispatcher) {
        return (LegacyFeatureRenderDispatcher) dispatcher;
    }

    void setBufferSource(MultiBufferSource.BufferSource bufferSource);

    MultiBufferSource.BufferSource getBufferSource();
}
