package wily.legacy.mixin.base.waterlogging;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.LegacyWaterlogging;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {
    @Inject(method = "getFluidState", at = @At("RETURN"), cancellable = true)
    private void legacy$getStoredFluidState(CallbackInfoReturnable<FluidState> cir) {
        FluidState fluidState = LegacyWaterlogging.getStoredFluidState((BlockState) (Object) this);
        if (LegacyWaterlogging.isWater(fluidState)) {
            cir.setReturnValue(fluidState);
        }
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void legacy$scheduleStoredFluidTick(
        LevelReader level,
        ScheduledTickAccess scheduledTickAccess,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random,
        CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState state = cir.getReturnValue();
        if (!LegacyWaterlogging.isWaterloggableState(state) || !state.getValue(LegacyWaterlogging.WATERLOGGED)) {
            return;
        }

        FluidState fluidState = LegacyWaterlogging.getStoredFluidState(state);
        if (!LegacyWaterlogging.isWater(fluidState)) {
            return;
        }

        Fluid fluid = fluidState.getType();
        scheduledTickAccess.scheduleTick(pos, fluid, fluid.getTickDelay(level));
    }
}
