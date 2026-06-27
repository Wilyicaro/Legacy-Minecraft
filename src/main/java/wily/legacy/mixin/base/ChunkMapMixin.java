package wily.legacy.mixin.base;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.world.LegacyGeneratedChunks;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    //? if <1.20.2 {
    /*
    @Inject(method = "isChunkInRange", at = @At("HEAD"), cancellable = true)
    private static void isWithinDistance(int i, int j, int k, int l, int m, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCommonOptions.squaredViewDistance.get()) cir.setReturnValue(Legacy4J.isChunkPosVisibleInSquare(i, j, m, k, l, false));
    }

    @Inject(method = "playerLoadedChunk", at = @At("HEAD"))
    private void legacy$syncFreshChunk(ServerPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> packet, LevelChunk chunk, CallbackInfo ci) {
        LegacyGeneratedChunks.sync(player, chunk);
    }
    *///?} else {
    @Inject(method = "markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("HEAD"))
    private static void legacy$syncFreshChunk(ServerPlayer player, LevelChunk chunk, CallbackInfo ci) {
        LegacyGeneratedChunks.sync(player, chunk);
    }
    //?}
}
