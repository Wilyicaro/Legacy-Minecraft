package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
//? if >=1.21.2 {
import net.minecraft.world.level.ScheduledTickAccess;
//?}
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.block.LegacyBlockExtension;

@Mixin(SnowLayerBlock.class)
public class SnowLayerBlockMixin extends Block implements Fallable, LegacyBlockExtension {
    public SnowLayerBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        level.scheduleTick(blockPos, this, this.getDelayAfterPlace());
    }

    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
            //? if >=1.21.2 {
    private void updateShape(BlockState blockState, LevelReader levelReader, ScheduledTickAccess scheduledTickAccess, BlockPos blockPos, Direction direction, BlockPos blockPos2, BlockState blockState2, RandomSource randomSource, CallbackInfoReturnable<BlockState> cir) {
        scheduledTickAccess.scheduleTick(blockPos, this, this.getDelayAfterPlace());
        cir.setReturnValue(super.updateShape(blockState, levelReader, scheduledTickAccess, blockPos, direction, blockPos2, blockState2, randomSource));
    }
    //?} else {
    /*private void updateShape(BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2, CallbackInfoReturnable<BlockState> cir) {
        levelAccessor.scheduleTick(blockPos, this, this.getDelayAfterPlace());
        cir.setReturnValue(super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2));
    }
    *///?}

    @Override
    public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        if (!FallingBlock.isFree(serverLevel.getBlockState(blockPos.below())) || blockPos.getY() < serverLevel./*? if >=1.21.2 {*/getMinY/*?} else {*//*getMinBuildHeight*//*?}*/())
            return;
        FallingBlockEntity.fall(serverLevel, blockPos, blockState);
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos blockPos, FallingBlockEntity fallingBlockEntity) {
        BlockState fallenState = level.getBlockState(blockPos);
        if (fallenState.is(Blocks.SNOW)) {
            ((FallingBlockAccessor)fallingBlockEntity).setCancelDrop(true);
            if (fallenState.getValue(SnowLayerBlock.LAYERS) >= SnowLayerBlock.MAX_HEIGHT) {
                BlockPos above = blockPos.above();
                BlockState aboveState = level.getBlockState(above);
                if (aboveState.is(Blocks.SNOW)) level.setBlock(above, fallingBlockEntity.getBlockState().setValue(SnowLayerBlock.LAYERS, Math.min(SnowLayerBlock.MAX_HEIGHT, aboveState.getValue(SnowLayerBlock.LAYERS) + fallingBlockEntity.getBlockState().getValue(SnowLayerBlock.LAYERS))), Block.UPDATE_ALL);
                else level.setBlock(above, fallingBlockEntity.getBlockState(), Block.UPDATE_ALL);
            } else {
                int total = fallenState.getValue(SnowLayerBlock.LAYERS) + fallingBlockEntity.getBlockState().getValue(SnowLayerBlock.LAYERS);
                level.setBlock(blockPos, fallenState.setValue(SnowLayerBlock.LAYERS, Math.min(SnowLayerBlock.MAX_HEIGHT, total)), Block.UPDATE_ALL);
                if (total > SnowLayerBlock.MAX_HEIGHT)
                    level.setBlock(blockPos.above(), fallingBlockEntity.getBlockState().setValue(SnowLayerBlock.LAYERS, total - SnowLayerBlock.MAX_HEIGHT), Block.UPDATE_ALL);
            }
        }
    }

    protected int getDelayAfterPlace() {
        return 2;
    }

    @Override
    public void animateTick(BlockState blockState, Level level, BlockPos blockPos, RandomSource randomSource) {
        if (randomSource.nextInt(16) == 0 && FallingBlock.isFree(level.getBlockState(blockPos.below()))) {
            ParticleUtils.spawnParticleBelow(level, blockPos, randomSource, new BlockParticleOption(ParticleTypes.FALLING_DUST, blockState));
        }
    }

    @Override
    public boolean l4j$isFreeForFalling(BlockState state) {
        return state.getValue(SnowLayerBlock.LAYERS) < SnowLayerBlock.MAX_HEIGHT;
    }
}
