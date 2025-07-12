//? if >=1.20.2 {
package wily.legacy.mixin.base;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyOptions;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {
    @Shadow private @Nullable ViewArea viewArea;
    //? if <1.21.2 {
    /*@Inject(method = "isInViewDistance", at = @At("HEAD"), cancellable = true)
    private void isWithinDistance(BlockPos blockPos, BlockPos blockPos2, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCommonOptions.squaredViewDistance.get()) cir.setReturnValue(Legacy4J.isChunkPosVisibleInSquare(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()), viewArea.getViewDistance(), SectionPos.blockToSectionCoord(blockPos2.getX()), SectionPos.blockToSectionCoord(blockPos2.getZ()), false));
    }
    *///?} else {
    @Inject(method = "isInViewDistance", at = @At("HEAD"), cancellable = true)
    private void isWithinDistance(long l, long m, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCommonOptions.squaredViewDistance.get()) cir.setReturnValue(Legacy4J.isChunkPosVisibleInSquare(SectionPos.x(l), SectionPos.z(l), viewArea.getViewDistance(), SectionPos.x(m), SectionPos.z(m), false));
    }
    //?}
}
//?}
