//? if <1.21 {
/*package wily.legacy.mixin.base;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.world.LegacyGeneratedChunks;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

@Mixin(ChunkStatus.class)
public class ChunkStatusMixin {
    @Inject(method = "generate", at = @At("HEAD"))
    private void legacy$markFreshGeneratedChunk(Executor executor, ServerLevel level, ChunkGenerator generator, StructureTemplateManager structureTemplateManager, ThreadedLevelLightEngine lightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> loadingFunction, List<ChunkAccess> chunks, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        if ((Object) this != ChunkStatus.FULL || chunks.isEmpty()) {
            return;
        }

        ChunkAccess chunk = chunks.get(chunks.size() / 2);
        if (chunk instanceof ProtoChunk && !(chunk instanceof ImposterProtoChunk) && !chunk.getStatus().isOrAfter(ChunkStatus.FULL)) {
            LegacyGeneratedChunks.mark(level, chunk.getPos());
        }
    }
}
*///?}
