//? >1.20.1 {
package wily.legacy.mixin.base;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(ChunkTrackingView.Positioned.class)
public class ChunkTrackingViewMixin {

    @Shadow @Final private int viewDistance;

    @Shadow @Final private ChunkPos center;

    @Inject(method = "contains", at = @At("HEAD"), cancellable = true)
    private void isWithinDistance(int i, int j, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCommonOptions.squaredViewDistance.get()) cir.setReturnValue(Legacy4J.isChunkPosVisibleInSquare(center.x, center.z, viewDistance, i, j, false));
    }
}
//?}
