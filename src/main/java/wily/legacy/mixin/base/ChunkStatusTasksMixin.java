package wily.legacy.mixin.base;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.world.LegacyGeneratedChunks;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkStatusTasks.class)
public class ChunkStatusTasksMixin {
    @Inject(method = "full", at = @At("HEAD"))
    private static void legacy$markFreshChunk(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> cache, ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        if (chunk instanceof ProtoChunk && !(chunk instanceof ImposterProtoChunk) && chunk.getPersistedStatus().isBefore(ChunkStatus.FULL)) {
            LegacyGeneratedChunks.mark(context.level(), chunk.getPos());
        }
    }
}
