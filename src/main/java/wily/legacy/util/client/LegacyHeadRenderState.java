package wily.legacy.util.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if <1.21.2 {
import net.minecraft.world.entity.LivingEntity;
//?} else {
/*import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
*///?}

public class LegacyHeadRenderState {
    private static final ThreadLocal</*? if <1.21.2 {*/LivingEntity/*?} else {*//*LivingEntityRenderState*//*?}*/> HEAD_RENDER_STATE = new ThreadLocal<>();
    //? if >=1.21.3 {
    /*private static final ThreadLocal<LivingEntityRenderState> WINGS_RENDER_STATE = new ThreadLocal<>();
    *///?}

    public static void set(/*? if <1.21.2 {*/LivingEntity/*?} else {*//*LivingEntityRenderState*//*?}*/ renderState) {
        HEAD_RENDER_STATE.set(renderState);
    }

    public static void clear() {
        HEAD_RENDER_STATE.remove();
    }

    //? if >=1.21.3 {
    /*public static void setWings(LivingEntityRenderState renderState) {
        WINGS_RENDER_STATE.set(renderState);
    }

    public static void clearWings() {
        WINGS_RENDER_STATE.remove();
    }
    *///?}

    public static int getHeadOverlay(int fallback) {
        return getOverlay(HEAD_RENDER_STATE.get(), fallback);
    }

    //? if >=1.21.3 {
    /*public static int getWingsOverlay(int fallback) {
        return getOverlay(WINGS_RENDER_STATE.get(), fallback);
    }
    *///?}

    public static int getBannerTint(int color) {
        return tint(color, getHeadOverlay(OverlayTexture.NO_OVERLAY));
    }

    //? if >=1.21.3 {
    /*public static int getWingsTint(int color) {
        return tint(color, getWingsOverlay(OverlayTexture.NO_OVERLAY));
    }
    *///?}

    public static float[] getBannerTint(float[] color) {
        if (getHeadOverlay(OverlayTexture.NO_OVERLAY) == OverlayTexture.NO_OVERLAY) return color;
        float[] tinted = color.clone();
        tinted[1] *= 0.5f;
        tinted[2] *= 0.5f;
        return tinted;
    }

    private static int getOverlay(/*? if <1.21.2 {*/LivingEntity/*?} else {*//*LivingEntityRenderState*//*?}*/ renderState, int fallback) {
        return renderState == null ? fallback : LivingEntityRenderer.getOverlayCoords(renderState, 0.0f);
    }

    private static int tint(int color, int overlay) {
        if (overlay == OverlayTexture.NO_OVERLAY) return color;
        return multiply(color, 0xFFFF8080);
    }

    private static int multiply(int color, int tint) {
        int alpha = (color >>> 24) * (tint >>> 24) / 255;
        int red = ((color >>> 16) & 255) * ((tint >>> 16) & 255) / 255;
        int green = ((color >>> 8) & 255) * ((tint >>> 8) & 255) / 255;
        int blue = (color & 255) * (tint & 255) / 255;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
