package wily.legacy.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.util.LightCoordsUtil;
//? if >=26.2 {
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}
//? if <26.2 {
/*import net.minecraft.client.renderer.SubmitNodeStorage;
*///?}
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4fc;

public interface LegacyNameTag {
    LegacyNameTag NEXT_SUBMIT = new Instance();

    static LegacyNameTag of(/*? if >=26.2 {*/NameTagFeatureRenderer.Submit/*?} else {*//*SubmitNodeStorage.NameTagSubmit*//*?}*/ nameTagSubmit) {
        return (LegacyNameTag) (Object) nameTagSubmit;
    }

    static /*? if >=26.2 {*/NameTagFeatureRenderer.Submit/*?} else {*//*SubmitNodeStorage.NameTagSubmit*//*?}*/ withColor(/*? if >=26.2 {*/NameTagFeatureRenderer.Submit/*?} else {*//*SubmitNodeStorage.NameTagSubmit*//*?}*/ nameTagSubmit, float[] color) {
        of(nameTagSubmit).setNameTagColor(color);
        return nameTagSubmit;
    }

    //? if >=26.2 {
    static boolean shouldRenderOutline(NameTagFeatureRenderer.Submit submit) {
        return LegacyOptions.displayNameTagBorder.get() && getThickness(submit.distanceToCameraSq()) < 1 && of(submit).hasColor();
    }

    static net.minecraft.client.renderer.rendertype.RenderType outlineRenderType(NameTagFeatureRenderer.Submit submit) {
        return submit.displayMode() == Font.DisplayMode.SEE_THROUGH ? RenderTypes.textBackgroundSeeThrough() : RenderTypes.textBackground();
    }
    //?}

    static float getThickness(double distanceToCameraSq) {
        return Math.max(0.1f, (float) Math.sqrt(distanceToCameraSq) / 16f);
    }

    //? if >=26.2 {
    static void renderNameTagOutline(Font font, VertexConsumer consumer, NameTagFeatureRenderer.Submit submit) {
    //?} else {
    /*static void renderNameTagOutline(Font font, MultiBufferSource.BufferSource bufferSource, SubmitNodeStorage.NameTagSubmit submit, boolean seeThrough) {
    *///?}
        float thickness = getThickness(submit.distanceToCameraSq());
        float[] color = LegacyNameTag.of(submit).getNameTagColor();
        if (!LegacyOptions.displayNameTagBorder.get() || thickness >= 1 || color == null) return;
        //? if >=26.2 {
        renderOutline(consumer, submit.pose(), submit.x() - 1.1f, submit.y() - 1.1f, font.width(submit.text()) + 2.1f, 10.1f, thickness, color[0], color[1], color[2], 1.0f);
        //?} else {
        /*renderOutline(bufferSource.getBuffer(seeThrough ? RenderTypes.textBackgroundSeeThrough() : RenderTypes.textBackground()), submit.pose(), submit.x() - 1.1f, submit.y() - 1.1f, font.width(submit.text()) + 2.1f, 10.1f, thickness, color[0], color[1], color[2], 1.0f);
        *///?}
    }

    static void renderOutline(VertexConsumer consumer, Matrix4fc matrix4f, float x, float y, float width, float height, float thickness, float r, float g, float b, float a) {
        fill(consumer, matrix4f, x, y, x + width, y + thickness, r, g, b, a);
        fill(consumer, matrix4f, x, y + height - thickness, x + width, y + height, r, g, b, a);
        fill(consumer, matrix4f, x, y + thickness, x + thickness, y + height - thickness, r, g, b, a);
        fill(consumer, matrix4f, x + width - thickness, y + thickness, x + width, y + height - thickness, r, g, b, a);
    }

    static void fill(VertexConsumer vertexConsumer, Matrix4fc matrix4f, float i, float j, float k, float l, float r, float g, float b, float a) {
        float o;
        if (i < k) {
            o = i;
            i = k;
            k = o;
        }
        if (j < l) {
            o = j;
            j = l;
            l = o;
        }
        vertexConsumer.addVertex(matrix4f, i, j, 0).setColor(r, g, b, a).setLight(LightCoordsUtil.FULL_BRIGHT);
        vertexConsumer.addVertex(matrix4f, i, l, 0).setColor(r, g, b, a).setLight(LightCoordsUtil.FULL_BRIGHT);
        vertexConsumer.addVertex(matrix4f, k, l, 0).setColor(r, g, b, a).setLight(LightCoordsUtil.FULL_BRIGHT);
        vertexConsumer.addVertex(matrix4f, k, j, 0).setColor(r, g, b, a).setLight(LightCoordsUtil.FULL_BRIGHT);
    }

    float[] getNameTagColor();

    void setNameTagColor(float[] color);

    default boolean hasColor() {
        return getNameTagColor() != null;
    }

    default void copyFrom(LegacyNameTag tag) {
        setNameTagColor(tag.getNameTagColor());
    }

    class Instance implements LegacyNameTag {
        float[] color;

        @Override
        public float[] getNameTagColor() {
            return color;
        }

        @Override
        public void setNameTagColor(float[] color) {
            this.color = color;
        }
    }
}
