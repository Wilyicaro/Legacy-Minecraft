package wily.legacy.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class LegacyGamma implements AutoCloseable {
    public static final LegacyGamma INSTANCE = new LegacyGamma();

    private final MappableRingBuffer ubo;

    public LegacyGamma() {
        ubo = new MappableRingBuffer(() -> "gamma SamplerInfo", 130, 4);
    }

    public void render() {
        Double value = LegacyOptions.legacyGamma.get();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(this.ubo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(mappedView.data()).putFloat(value.floatValue() >= 0.5f ? (value.floatValue() - 0.5f) * 1.12f + 1.08f : value.floatValue() * 0.96f + 0.6f);
        }

        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        ProfilerFiller profilerFiller = Profiler.get();
        profilerFiller.push("legacyGamma");
        RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer gpuBuffer2 = autoStorageIndexBuffer.getBuffer(6);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "Display Legacy Gamma", target.getColorTextureView(), OptionalInt.empty(), target.useDepth ? target.getDepthTextureView() : null, OptionalDouble.empty())) {
            renderPass.setPipeline(LegacyRenderPipelines.GAMMA);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("GammaInfo", this.ubo.currentBuffer());
            renderPass.setVertexBuffer(0, RenderSystem.getQuadVertexBuffer());
            renderPass.setIndexBuffer(gpuBuffer2, autoStorageIndexBuffer.type());
            renderPass.bindSampler("InSampler", target.getColorTextureView());
            renderPass.drawIndexed(0, 0, 6, 1);
        }
        this.ubo.rotate();
        profilerFiller.pop();
    }

    @Override
    public void close() {
        ubo.close();
    }
}
