package wily.legacy.mixin.base.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonEggBlock.class)
public class DragonEggBlockMixin {
    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
    private void skipPredictedDragonEggParticles(BlockState blockState, Level level, BlockPos blockPos, CallbackInfo ci) {
        if (level.isClientSide()) {
            ci.cancel();
        }
    }
}
