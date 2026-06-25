package wily.legacy.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import wily.factoryapi.mixin.base.RenderPipelinesAccessor;
import wily.legacy.Legacy4J;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class LegacyHaloRing {
    private static final int RADIUS = 100;
    private static final int SEGMENTS = 50;
    private static final int VERTEX_COUNT = (SEGMENTS + 1) * 2;
    private static final ResourceLocation TEXTURE = Legacy4J.createModLocation("textures/misc/halo_ring.png");
    private static final RenderPipeline PIPELINE = RenderPipelinesAccessor.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withLocation(Legacy4J.createModLocation("pipeline/halo_ring"))
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );
    private static GpuBuffer ringBuffer;

    private LegacyHaloRing() {
    }

    public static void render(PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || DimensionSpecialEffects.forType(level.dimensionType()).skyType() != DimensionSpecialEffects.SkyType.OVERWORLD || !CommonValue.HALO_RING.get()) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int skyColor = level.getSkyColor(camera.getPosition(), partialTick);
        float luminance = (ARGB.redFloat(skyColor) * 2.0f + ARGB.greenFloat(skyColor) * 3.0f + ARGB.blueFloat(skyColor)) / 6.0f;
        float brightness = 0.6f + Mth.clamp(luminance, 0.0f, 1.0f) * 0.4f;

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        draw(poseStack.last().pose(), brightness);
        poseStack.popPose();
    }

    private static void draw(Matrix4f pose, float brightness) {
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(modelView, new Vector4f(brightness, brightness, brightness, 1.0f), new Vector3f(), new Matrix4f(), 0.0f);
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        AbstractTexture haloTexture = Minecraft.getInstance().getTextureManager().getTexture(TEXTURE);

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Legacy halo ring", target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", transforms);
            renderPass.bindSampler("Sampler0", haloTexture.getTextureView());
            renderPass.setVertexBuffer(0, getRingBuffer());
            renderPass.draw(0, VERTEX_COUNT);
        }
    }

    private static GpuBuffer getRingBuffer() {
        if (ringBuffer == null || ringBuffer.isClosed()) {
            ringBuffer = buildRingBuffer();
        }
        return ringBuffer;
    }

    private static GpuBuffer buildRingBuffer() {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX_COLOR);
        float u = 0.0f;
        float verticalOffset = RADIUS * 999.0f / 1000.0f;
        float arcRadians = Mth.TWO_PI / SEGMENTS;
        float halfSegments = SEGMENTS / 2.0f;
        float wideSegments = SEGMENTS / 8.0f;
        float wideSegmentsSqr = wideSegments * wideSegments;

        for (int i = 0; i <= SEGMENTS; i++) {
            float diff = Math.abs(i - halfSegments);
            float edge = halfSegments - wideSegments;
            diff = diff < edge ? 0.0f : diff - edge;
            float width = 1.0f + diff * diff / wideSegmentsSqr * 10.0f;
            float x = RADIUS * Mth.cos(i * arcRadians) - verticalOffset;
            float y = RADIUS * Mth.sin(i * arcRadians);
            int alpha = getFogAlpha(i);
            builder.addVertex(x, y, -width).setUv(u, 0.0f).setColor(255, 255, 255, alpha);
            builder.addVertex(x, y, width).setUv(u, 1.0f).setColor(255, 255, 255, alpha);
            u -= 0.25f;
        }

        try (MeshData meshData = builder.buildOrThrow()) {
            return RenderSystem.getDevice().createBuffer(() -> "Legacy halo ring", GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
        }
    }

    private static int getFogAlpha(int segment) {
        float edgeFade = Mth.clamp(Math.min(segment, SEGMENTS - segment) / 14.0f, 0.0f, 1.0f);
        float alpha = edgeFade * edgeFade * (3.0f - 2.0f * edgeFade);
        return Mth.floor(alpha * 255.0f);
    }
}
