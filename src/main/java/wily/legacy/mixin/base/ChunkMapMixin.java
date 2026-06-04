package wily.legacy.mixin.base;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.world.LegacyGeneratedChunks;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Inject(method = "markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("HEAD"))
    private static void legacy$syncFreshChunk(ServerPlayer player, LevelChunk chunk, CallbackInfo ci) {
        LegacyGeneratedChunks.sync(player, chunk);
    }
}
