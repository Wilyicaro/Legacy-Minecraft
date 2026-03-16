package wily.legacy.mixin.base.waterlogging;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.LegacySimpleWaterloggedBlock;
import wily.legacy.util.LegacyWaterlogging;

@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin implements LegacySimpleWaterloggedBlock {
    @Redirect(
        method = "setPlacedBy",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
        )
    )
    private boolean legacy$syncUpperHalfFluid(Level level, BlockPos targetPos, BlockState upperState, int flags, Level originalLevel, BlockPos pos, BlockState placedState, LivingEntity placer, ItemStack stack) {
        return level.setBlock(targetPos, LegacyWaterlogging.syncPlacementFluid(level, targetPos, upperState), flags);
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void legacy$preserveHalfFluid(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess scheduledTickAccess,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random,
        CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState updatedState = cir.getReturnValue();
        if (updatedState.isAir()) {
            if (LegacyWaterlogging.isWaterloggableState(state) && state.getValue(LegacyWaterlogging.WATERLOGGED)) {
                cir.setReturnValue(Fluids.WATER.defaultFluidState().createLegacyBlock());
            }
            return;
        }

        BlockState preservedState = LegacyWaterlogging.preserveWaterloggedState(state, updatedState);
        if (preservedState != updatedState) {
            if (preservedState.getValue(LegacyWaterlogging.WATERLOGGED)) {
                scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            cir.setReturnValue(preservedState);
        }
    }
}
