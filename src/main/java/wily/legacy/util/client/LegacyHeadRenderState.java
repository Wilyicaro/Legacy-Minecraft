package wily.legacy.util.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;

public class LegacyHeadRenderState {
    private static final ThreadLocal<LivingEntityRenderState> HEAD_RENDER_STATE = new ThreadLocal<>();

    public static void set(LivingEntityRenderState renderState) {
        HEAD_RENDER_STATE.set(renderState);
    }

    public static void clear() {
        HEAD_RENDER_STATE.remove();
    }

    public static int getBannerTint(int color) {
        LivingEntityRenderState renderState = HEAD_RENDER_STATE.get();
        return renderState != null && LivingEntityRenderer.getOverlayCoords(renderState, 0.0f) != OverlayTexture.NO_OVERLAY ? ARGB.multiply(color, 0xFFFF8080) : color;
    }
}
