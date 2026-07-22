package wily.legacy.util.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.SequencedMap;

public final class LegacyTntFlash {
    private static final Direction[] FACES = Direction.values();
    private static final RenderType FLASH = createRenderType("legacy_tnt_flash", RenderStateShard.NO_LAYERING);
    private static final RenderType OFFSET_FLASH = createRenderType("legacy_tnt_offset_flash", RenderStateShard.VIEW_OFFSET_Z_LAYERING);

    private LegacyTntFlash() {
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float fuse, boolean offset) {
        if (fuse < 0.0F || (int) fuse / 5 % 2 != 0) return;
        float alpha = Mth.clamp((1.0F - fuse / 100.0F) * 0.8F, 0.0F, 1.0F);
        collector.submitCustomGeometry(poseStack, offset ? OFFSET_FLASH : FLASH, (pose, consumer) -> {
            for (Direction face : FACES) ShapeRenderer.renderFace(pose.pose(), consumer, face, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha);
        });
    }

    public static void registerBuffers(SequencedMap<RenderType, ByteBufferBuilder> buffers) {
        buffers.put(FLASH, new ByteBufferBuilder(FLASH.bufferSize()));
        buffers.put(OFFSET_FLASH, new ByteBufferBuilder(OFFSET_FLASH.bufferSize()));
    }

    private static RenderType createRenderType(String name, RenderStateShard.LayeringStateShard layering) {
        return RenderType.create(name, 1536, false, true, RenderPipelines.DEBUG_QUADS, RenderType.CompositeState.builder().setLayeringState(layering).createCompositeState(false));
    }
}
