package wily.legacy.client;

import net.minecraft.client.renderer.state.LightmapRenderState;

public interface LegacyLightmapRenderState {
    static LegacyLightmapRenderState of(LightmapRenderState renderState) {
        return (LegacyLightmapRenderState) renderState;
    }

    float getUnderwaterVisionFactor();

    void setUnderwaterVisionFactor(float factor);
}
