package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;

@Mixin(GrowingPlantBlock.class)
public abstract class GrowingPlantBlockMixin {
    @Unique
    private static final VoxelShape LEGACY_KELP_SHAPE = Block.column(12.0, 0.0, 16.0);

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!Legacy4J.canApplyServerAuthoritativeChanges()) return;
        if (state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)) {
            cir.setReturnValue(LEGACY_KELP_SHAPE);
        }
    }
}
