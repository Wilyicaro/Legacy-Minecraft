package wily.legacy.util.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.SequencedMap;

public final class LegacyTntFlash {
    private static final Direction[] FACES = Direction.values();
    private static final RenderType FLASH = createRenderType("legacy_tnt_flash", LayeringTransform.NO_LAYERING);
    private static final RenderType OFFSET_FLASH = createRenderType("legacy_tnt_offset_flash", LayeringTransform.VIEW_OFFSET_Z_LAYERING);

    private LegacyTntFlash() {
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float fuse, boolean offset) {
        if (fuse < 0.0F || (int) fuse / 5 % 2 != 0) return;
        float alpha = Mth.clamp((1.0F - fuse / 100.0F) * 0.8F, 0.0F, 1.0F);
        collector.submitCustomGeometry(poseStack, offset ? OFFSET_FLASH : FLASH, (pose, consumer) -> {
            for (Direction face : FACES) renderFace(pose, consumer, face, alpha);
        });
    }

    public static void registerBuffers(SequencedMap<RenderType, ByteBufferBuilder> buffers) {
        buffers.put(FLASH, new ByteBufferBuilder(FLASH.bufferSize()));
        buffers.put(OFFSET_FLASH, new ByteBufferBuilder(OFFSET_FLASH.bufferSize()));
    }

    private static RenderType createRenderType(String name, LayeringTransform layering) {
        return RenderType.create(name, RenderSetup.builder(RenderPipelines.DEBUG_QUADS).bufferSize(1536).sortOnUpload().setLayeringTransform(layering).createRenderSetup());
    }

    private static void renderFace(PoseStack.Pose pose, VertexConsumer consumer, Direction face, float alpha) {
        switch (face) {
            case DOWN -> {
                vertex(pose, consumer, 0.0F, 0.0F, 0.0F, alpha);
                vertex(pose, consumer, 1.0F, 0.0F, 0.0F, alpha);
                vertex(pose, consumer, 1.0F, 0.0F, 1.0F, alpha);
                vertex(pose, consumer, 0.0F, 0.0F, 1.0F, alpha);
            }
            case UP -> {
                vertex(pose, consumer, 0.0F, 1.0F, 0.0F, alpha);
                vertex(pose, consumer, 0.0F, 1.0F, 1.0F, alpha);
                vertex(pose, consumer, 1.0F, 1.0F, 1.0F, alpha);
                vertex(pose, consumer, 1.0F, 1.0F, 0.0F, alpha);
            }
            case NORTH -> {
                vertex(pose, consumer, 0.0F, 0.0F, 0.0F, alpha);
                vertex(pose, consumer, 0.0F, 1.0F, 0.0F, alpha);
                vertex(pose, consumer, 1.0F, 1.0F, 0.0F, alpha);
                vertex(pose, consumer, 1.0F, 0.0F, 0.0F, alpha);
            }
            case SOUTH -> {
                vertex(pose, consumer, 0.0F, 0.0F, 1.0F, alpha);
                vertex(pose, consumer, 1.0F, 0.0F, 1.0F, alpha);
                vertex(pose, consumer, 1.0F, 1.0F, 1.0F, alpha);
                vertex(pose, consumer, 0.0F, 1.0F, 1.0F, alpha);
            }
            case WEST -> {
                vertex(pose, consumer, 0.0F, 0.0F, 0.0F, alpha);
                vertex(pose, consumer, 0.0F, 0.0F, 1.0F, alpha);
                vertex(pose, consumer, 0.0F, 1.0F, 1.0F, alpha);
                vertex(pose, consumer, 0.0F, 1.0F, 0.0F, alpha);
            }
            case EAST -> {
                vertex(pose, consumer, 1.0F, 0.0F, 0.0F, alpha);
                vertex(pose, consumer, 1.0F, 1.0F, 0.0F, alpha);
                vertex(pose, consumer, 1.0F, 1.0F, 1.0F, alpha);
                vertex(pose, consumer, 1.0F, 0.0F, 1.0F, alpha);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float alpha) {
        consumer.addVertex(pose, x, y, z).setColor(1.0F, 1.0F, 1.0F, alpha);
    }
}
