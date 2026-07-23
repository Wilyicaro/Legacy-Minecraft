package wily.legacy.util.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class LegacyTntFlash {
    private LegacyTntFlash() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, float fuse, boolean offset) {
        if (fuse < 0.0F || (int) fuse / 5 % 2 != 0) return;
        float alpha = Mth.clamp((1.0F - fuse / 100.0F) * 0.8F, 0.0F, 1.0F);
        poseStack.pushPose();
        if (offset) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.scale(1.002F, 1.002F, 1.002F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        face(pose, consumer, alpha, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1);
        face(pose, consumer, alpha, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0);
        face(pose, consumer, alpha, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0);
        face(pose, consumer, alpha, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1);
        face(pose, consumer, alpha, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0);
        face(pose, consumer, alpha, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1);
        poseStack.popPose();
    }

    private static void face(Matrix4f pose, VertexConsumer consumer, float alpha, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        vertex(pose, consumer, alpha, x0, y0, z0);
        vertex(pose, consumer, alpha, x1, y1, z1);
        vertex(pose, consumer, alpha, x2, y2, z2);
        vertex(pose, consumer, alpha, x3, y3, z3);
    }

    private static void vertex(Matrix4f pose, VertexConsumer consumer, float alpha, float x, float y, float z) {
        //? if <1.21 {
        /*consumer.vertex(pose, x, y, z).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        *///?} else {
        consumer.addVertex(pose, x, y, z).setColor(1.0F, 1.0F, 1.0F, alpha);
        //?}
    }
}
