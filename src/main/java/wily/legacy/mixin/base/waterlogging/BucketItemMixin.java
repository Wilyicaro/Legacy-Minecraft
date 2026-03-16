package wily.legacy.mixin.base.waterlogging;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.LegacyWaterlogging;

@Mixin(BucketItem.class)
public class BucketItemMixin {
    @Shadow
    @Final
    private Fluid content;

    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void legacy$placeWaterIntoCustomContainer(LivingEntity user, Level level, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        if (content != Fluids.WATER || hitResult == null) {
            return;
        }

        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (LegacyWaterlogging.isSupportedCauldron(clickedState)
            && hitResult.getDirection() == Direction.UP
            && level.getBlockState(clickedPos.above()).canBeReplaced(content)) {
            BlockPos abovePos = clickedPos.above();
            if (!level.isClientSide()) {
                level.setBlock(abovePos, Fluids.WATER.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
                level.scheduleTick(abovePos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
                LegacyWaterlogging.updateCauldronFromWorld(level, clickedPos, clickedState);
                seedCauldronTopFlow(level, abovePos);
            }
            finishWaterPlacement(user, level, abovePos, cir);
            return;
        }
        if (LegacyWaterlogging.letsWaterFlowThrough(clickedState)
            || !LegacyWaterlogging.canPlaceWater(clickedState, Fluids.WATER.getSource(false))) {
            return;
        }

        if (!level.isClientSide()) {
            LegacyWaterlogging.placeWater(level, clickedPos, clickedState, Fluids.WATER.getSource(false));
        }
        finishWaterPlacement(user, level, clickedPos, cir);
    }

    private static void finishWaterPlacement(LivingEntity user, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        level.playSound(user, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(user, GameEvent.FLUID_PLACE, pos);
        cir.setReturnValue(true);
    }

    private static void seedCauldronTopFlow(Level level, BlockPos sourcePos) {
        FluidState sideFluid = ((FlowingFluid) Fluids.WATER).getFlowing(7, false);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = sourcePos.relative(direction);
            BlockState targetState = level.getBlockState(targetPos);
            if (LegacyWaterlogging.canPlaceWater(targetState, sideFluid)) {
                LegacyWaterlogging.placeWater(level, targetPos, targetState, sideFluid);
                continue;
            }
            if (!targetState.isAir() && !targetState.canBeReplaced(Fluids.WATER)) {
                continue;
            }
            level.setBlock(targetPos, sideFluid.createLegacyBlock(), Block.UPDATE_ALL);
            level.scheduleTick(targetPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }
}
