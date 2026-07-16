package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.client.LegacyLightmapRenderState;

@Mixin(LightmapRenderState.class)
public class LightmapRenderStateMixin implements LegacyLightmapRenderState {
    @Unique
    private float legacy$underwaterVisionFactor;

    @Override
    public float getUnderwaterVisionFactor() {
        return legacy$underwaterVisionFactor;
    }

    @Override
    public void setUnderwaterVisionFactor(float factor) {
        legacy$underwaterVisionFactor = factor;
    }
}
