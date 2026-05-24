//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyChunkLoading;

@Mixin(value = LevelSlice.class, remap = false)
public class LevelSliceMixin {
    @Inject(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"), cancellable = true)
    private void getBlockState(int x, int y, int z, CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(LegacyChunkLoading.getFeatureState(new BlockPos(x, y, z), cir.getReturnValue()));
    }
}
//?}
