package wily.legacy.util;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;

public interface LegacySimpleWaterloggedBlock extends SimpleWaterloggedBlock {
    @Override
    default boolean canPlaceLiquid(LivingEntity placer, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        if (LegacyWaterlogging.letsWaterFlowThrough(state)) {
            return false;
        }
        return LegacyWaterlogging.isWater(fluid) && LegacyWaterlogging.canPlaceWater(state, Fluids.WATER.getSource(false));
    }

    @Override
    default boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return LegacyWaterlogging.placeWater(level, pos, state, fluidState);
    }

    @Override
    default ItemStack pickupBlock(LivingEntity livingEntity, LevelAccessor level, BlockPos pos, BlockState state) {
        return LegacyWaterlogging.pickupWater(level, pos, state);
    }

    @Override
    default Optional<SoundEvent> getPickupSound() {
        return LegacyWaterlogging.getPickupSound();
    }
}
