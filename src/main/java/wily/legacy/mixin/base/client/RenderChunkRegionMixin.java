package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyChunkLoading;

@Mixin(RenderChunkRegion.class)
public class RenderChunkRegionMixin {
    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(LegacyChunkLoading.getFeatureState(pos, cir.getReturnValue()));
    }
}
