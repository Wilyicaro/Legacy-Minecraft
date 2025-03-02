//? if <1.20.2 {
/*package wily.legacy.mixin.base;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
    @Inject(method = "isChunkInRange", at = @At("HEAD"), cancellable = true)
    private static void isWithinDistance(int i, int j, int k, int l, int m, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCommonOptions.squaredViewDistance.get()) cir.setReturnValue(Legacy4J.isChunkPosVisibleInSquare(i, j, m, k, l, false));
    }
}
*///?}
