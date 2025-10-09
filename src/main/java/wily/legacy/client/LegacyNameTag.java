package wily.legacy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public interface LegacyNameTag {
    static LegacyNameTag of(SubmitNodeStorage.NameTagSubmit nameTagSubmit) {
        return (LegacyNameTag) (Object) nameTagSubmit;
    }

    static SubmitNodeStorage.NameTagSubmit withColor(SubmitNodeStorage.NameTagSubmit nameTagSubmit, float[] color) {
        of(nameTagSubmit).setNameTagColor(color);
        return nameTagSubmit;
    }

    void setNameTagColor(float[] color);

    float[] getNameTagColor();

    default boolean hasColor() {
        return getNameTagColor() != null;
    }

    static float getThickness(double distanceToCameraSq) {
        return Math.max(0.1f, (float) Math.sqrt(distanceToCameraSq) / 16f);
    }

    static void renderNameTagOutline(Font font, MultiBufferSource.BufferSource bufferSource, SubmitNodeStorage.NameTagSubmit submit, boolean seeThrough) {
        float thickness = getThickness(submit.distanceToCameraSq());
        float[] color = LegacyNameTag.of(submit).getNameTagColor();
        if (!LegacyOptions.displayNameTagBorder.get() || thickness >= 1 || color == null) return;
        renderOutline(bufferSource.getBuffer(seeThrough ? RenderType.textBackgroundSeeThrough() : RenderType.textBackground()), submit.pose(), submit.x() - 1.1f, submit.y() - 1.1f, font.width(submit.text()) + 2.1f,10.1f, thickness, color[0],color[1],color[2],1.0f);
    }

    static void renderOutline(VertexConsumer consumer, Matrix4f matrix4f, float x, float y, float width, float height, float thickness, float r, float g, float b , float a) {
        fill(consumer, matrix4f, x, y, x + width, y + thickness, r,g,b,a);
        fill(consumer, matrix4f, x, y + height - thickness, x + width, y + height, r,g,b,a);
        fill(consumer, matrix4f, x, y + thickness, x + thickness, y + height - thickness, r,g,b,a);
        fill(consumer, matrix4f, x + width - thickness, y + thickness, x + width, y + height - thickness, r,g,b,a);
    }

    static void fill(VertexConsumer vertexConsumer, Matrix4f matrix4f, float i, float j, float k, float l, float r, float g, float b , float a) {
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
        vertexConsumer.addVertex(matrix4f, i, j, 0).setColor(r,g,b,a).setLight(LightTexture.FULL_BRIGHT);
        vertexConsumer.addVertex(matrix4f, i, l, 0).setColor(r,g,b,a).setLight(LightTexture.FULL_BRIGHT);
        vertexConsumer.addVertex(matrix4f, k, l, 0).setColor(r,g,b,a).setLight(LightTexture.FULL_BRIGHT);
        vertexConsumer.addVertex(matrix4f, k, j, 0).setColor(r,g,b,a).setLight(LightTexture.FULL_BRIGHT);
    }

    interface Storage {
        void add(PoseStack poseStack, @Nullable Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState, float[] color);
    }
}
