package wily.legacy.mixin.base.client;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.OldGlassRendering;

@Mixin(HalfTransparentBlock.class)
public abstract class HalfTransparentBlockMixin {
    // Stops vanilla from culling faces between touching glass blocks when the toggle is on.
    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void legacy$showSharedGlassFaces(BlockState state, BlockState adjacentState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (OldGlassRendering.shouldRenderSharedFace(state, adjacentState)) {
            cir.setReturnValue(false);
        }
    }
}
