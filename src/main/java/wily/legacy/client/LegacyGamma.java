package wily.legacy.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class LegacyGamma implements AutoCloseable {
    public static final LegacyGamma INSTANCE = new LegacyGamma();

    private final MappableRingBuffer ubo;
    private GpuTexture inputTexture;
    private GpuTextureView inputView;

    public LegacyGamma() {
        ubo = new MappableRingBuffer(() -> "gamma SamplerInfo", 130, 4);
    }

    public void render() {
        float value = LegacyOptions.legacyGamma.get().floatValue();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(this.ubo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(mappedView.data()).putFloat(value >= 0.5f ? (value - 0.5f) * 1.12f + 1.08f : value * 0.96f + 0.6f);
        }

        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        resizeInput(target);
        commandEncoder.copyTextureToTexture(target.getColorTexture(), inputTexture, 0, 0, 0, 0, 0, target.width, target.height);
        commandEncoder.clearDepthTexture(target.getDepthTexture(), 1.0);
        ProfilerFiller profilerFiller = Profiler.get();
        profilerFiller.push("legacyGamma");
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "Display Legacy Gamma", target.getColorTextureView(), OptionalInt.empty(), target.useDepth ? target.getDepthTextureView() : null, OptionalDouble.empty())) {
            renderPass.setPipeline(LegacyRenderPipelines.GAMMA);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("InSampler", inputView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass.setUniform("GammaInfo", this.ubo.currentBuffer());
            renderPass.draw(0, 3);
        }
        this.ubo.rotate();
        profilerFiller.pop();
    }

    private void resizeInput(RenderTarget target) {
        if (inputTexture != null && inputTexture.getWidth(0) == target.width && inputTexture.getHeight(0) == target.height) return;
        closeInput();
        inputTexture = RenderSystem.getDevice().createTexture("legacy gamma input", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, target.getColorTexture().getFormat(), target.width, target.height, 1, 1);
        inputView = RenderSystem.getDevice().createTextureView(inputTexture);
    }

    private void closeInput() {
        if (inputView != null) {
            inputView.close();
            inputView = null;
        }
        if (inputTexture != null) {
            inputTexture.close();
            inputTexture = null;
        }
    }

    @Override
    public void close() {
        closeInput();
        ubo.close();
    }
}
