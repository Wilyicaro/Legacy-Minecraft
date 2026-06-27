//? if <1.20.2 {
/*package wily.legacy.mixin.base;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer$RenderChunkInfo")
public interface LevelRendererRenderChunkInfoAccessor {
    @Accessor("chunk")
    ChunkRenderDispatcher.RenderChunk legacy$getChunk();
}
*///?}
