package wily.legacy.mixin.base.waterlogging;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.LegacyWaterlogging;

import java.util.Map;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Shadow
    protected abstract void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState);

    @Inject(method = "canPassThroughWall", at = @At("HEAD"), cancellable = true)
    private static void legacy$allowPassThroughWall(
        Direction direction,
        BlockGetter level,
        BlockPos pos,
        BlockState state,
        BlockPos neighborPos,
        BlockState neighborState,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (LegacyWaterlogging.letsWaterFlowThrough(state) || LegacyWaterlogging.letsWaterFlowThrough(neighborState)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canHoldAnyFluid", at = @At("HEAD"), cancellable = true)
    private static void legacy$allowPassThroughHoldAny(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyWaterlogging.letsWaterFlowThrough(state)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canHoldSpecificFluid", at = @At("HEAD"), cancellable = true)
    private static void legacy$allowPassThroughHoldWater(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyWaterlogging.isWater(fluid) && LegacyWaterlogging.canPlaceWater(state, Fluids.WATER.getSource(false))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void legacy$spreadIntoWaterloggable(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo ci) {
        if (!LegacyWaterlogging.isWater(fluidState)) {
            return;
        }
        if (direction == Direction.DOWN && LegacyWaterlogging.isSupportedCauldron(state)) {
            LegacyWaterlogging.updateCauldronFromWorld(level, pos, state);
            ci.cancel();
            return;
        }
        if (LegacyWaterlogging.placeWater(level, pos, state, fluidState)) {
            ci.cancel();
        }
    }

    @Inject(method = "spreadToSides", at = @At("HEAD"))
    private void legacy$spreadToAdjacentPassThrough(ServerLevel level, BlockPos pos, FluidState fluidState, BlockState state, CallbackInfo ci) {
        if (!LegacyWaterlogging.isWater(fluidState)) {
            return;
        }

        int amount = fluidState.isSource() || fluidState.getValue(FlowingFluid.FALLING) ? 7 : fluidState.getAmount() - 1;
        if (amount <= 0) {
            return;
        }

        FluidState sideFluid = ((FlowingFluid) (Object) this).getFlowing(amount, false);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = pos.relative(direction);
            BlockState targetState = level.getBlockState(targetPos);
            if (LegacyWaterlogging.letsWaterFlowThrough(targetState) && LegacyWaterlogging.canPlaceWater(targetState, sideFluid)) {
                spreadTo(level, targetPos, targetState, direction, sideFluid);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void legacy$tickPassThrough(ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState, CallbackInfo ci) {
        if (!LegacyWaterlogging.isPassThroughState(state) || !LegacyWaterlogging.isWater(fluidState)) {
            return;
        }

        BlockState recalculatedState = LegacyWaterlogging.recalculatePassThroughState(level, pos, state);
        if (recalculatedState != state) {
            level.setBlock(pos, recalculatedState, Block.UPDATE_ALL);
            state = recalculatedState;
            fluidState = recalculatedState.getFluidState();
        }
        if (!LegacyWaterlogging.isWater(fluidState)) {
            ci.cancel();
            return;
        }

        FlowingFluid flowingFluid = (FlowingFluid) (Object) this;
        spreadIntoAdjacentPassThrough(level, pos, fluidState, flowingFluid);
        spreadFromPassThrough(level, pos, fluidState, flowingFluid);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void legacy$tickWater(ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState, CallbackInfo ci) {
        if (LegacyWaterlogging.isWater(fluidState)) {
            FlowingFluid flowingFluid = (FlowingFluid) (Object) this;
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            if (LegacyWaterlogging.isSupportedCauldron(belowState)) {
                LegacyWaterlogging.updateCauldronFromWorld(level, belowPos, belowState);
            }
            spreadIntoAdjacentPassThrough(level, pos, fluidState, flowingFluid);
            spreadAroundCauldronTop(level, pos, fluidState, flowingFluid);
        }
    }

    @Inject(method = "getSpread", at = @At("RETURN"), cancellable = true)
    private void legacy$getSpread(ServerLevel level, BlockPos pos, BlockState state, CallbackInfoReturnable<Map<Direction, FluidState>> cir) {
        FluidState fluidState = state.getFluidState();
        if (!LegacyWaterlogging.isWater(fluidState)) {
            return;
        }

        int amount = fluidState.isSource() || fluidState.getValue(FlowingFluid.FALLING) ? 7 : fluidState.getAmount() - 1;
        if (amount <= 0) {
            return;
        }

        Map<Direction, FluidState> spread = cir.getReturnValue();
        FluidState spreadFluid = ((FlowingFluid) (Object) this).getFlowing(amount, false);
        boolean sourcePassThrough = LegacyWaterlogging.letsWaterFlowThrough(state);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (spread.containsKey(direction)) {
                continue;
            }

            BlockPos targetPos = pos.relative(direction);
            BlockState targetState = level.getBlockState(targetPos);
            if (LegacyWaterlogging.letsWaterFlowThrough(targetState) && LegacyWaterlogging.canPlaceWater(targetState, spreadFluid)) {
                spread.put(direction, spreadFluid);
                continue;
            }

            if (!sourcePassThrough || !targetState.isAir()) {
                continue;
            }

            if (targetState.getFluidState().canBeReplacedWith(level, targetPos, spreadFluid.getType(), direction)) {
                spread.put(direction, spreadFluid);
            }
        }
        cir.setReturnValue(spread);
    }

    private void spreadFromPassThrough(ServerLevel level, BlockPos pos, FluidState fluidState, FlowingFluid flowingFluid) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        FluidState downwardFluid = flowingFluid.getFlowing(8, true);
        if (belowState.isAir() || LegacyWaterlogging.canPlaceWater(belowState, downwardFluid)) {
            spreadTo(level, belowPos, belowState, Direction.DOWN, downwardFluid);
        }

        int amount = fluidState.isSource() || fluidState.getValue(FlowingFluid.FALLING) ? 7 : fluidState.getAmount() - 1;
        if (amount <= 0) {
            return;
        }

        FluidState sideFluid = flowingFluid.getFlowing(amount, false);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = pos.relative(direction);
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.isAir() || LegacyWaterlogging.canPlaceWater(targetState, sideFluid)) {
                spreadTo(level, targetPos, targetState, direction, sideFluid);
            }
        }
    }

    private void spreadIntoAdjacentPassThrough(ServerLevel level, BlockPos pos, FluidState fluidState, FlowingFluid flowingFluid) {
        FluidState downwardFluid = flowingFluid.getFlowing(8, true);
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (LegacyWaterlogging.letsWaterFlowThrough(belowState) && LegacyWaterlogging.canPlaceWater(belowState, downwardFluid)) {
            spreadTo(level, belowPos, belowState, Direction.DOWN, downwardFluid);
        }

        int amount = fluidState.isSource() || fluidState.getValue(FlowingFluid.FALLING) ? 7 : fluidState.getAmount() - 1;
        if (amount <= 0) {
            return;
        }

        FluidState sideFluid = flowingFluid.getFlowing(amount, false);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = pos.relative(direction);
            BlockState targetState = level.getBlockState(targetPos);
            if (LegacyWaterlogging.letsWaterFlowThrough(targetState) && LegacyWaterlogging.canPlaceWater(targetState, sideFluid)) {
                spreadTo(level, targetPos, targetState, direction, sideFluid);
            }
        }
    }

    private void spreadAroundCauldronTop(ServerLevel level, BlockPos pos, FluidState fluidState, FlowingFluid flowingFluid) {
        BlockState belowState = level.getBlockState(pos.below());
        if (!belowState.is(Blocks.CAULDRON) && !belowState.is(Blocks.WATER_CAULDRON)) {
            return;
        }

        int amount = fluidState.isSource() || fluidState.getValue(FlowingFluid.FALLING) ? 7 : fluidState.getAmount() - 1;
        if (amount <= 0) {
            return;
        }

        FluidState sideFluid = flowingFluid.getFlowing(amount, false);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = pos.relative(direction);
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.isAir() || targetState.canBeReplaced(sideFluid.getType()) || LegacyWaterlogging.canPlaceWater(targetState, sideFluid)) {
                spreadTo(level, targetPos, targetState, direction, sideFluid);
            }
        }
    }
}
