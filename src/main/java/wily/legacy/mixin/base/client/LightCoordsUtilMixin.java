//? if >=26.2 {
package wily.legacy.mixin.base.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyChunkLoading;

@Mixin(LightCoordsUtil.class)
public class LightCoordsUtilMixin {
    @Inject(method = "getLightCoords(Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void getLightCoords(LightCoordsUtil.BrightnessGetter brightnessGetter, BlockAndLightGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (LegacyChunkLoading.hasPendingFeatures(pos)) {
            cir.setReturnValue(LightCoordsUtil.max(cir.getReturnValue(), LightCoordsUtil.FULL_SKY));
        }
    }
}
//?}
