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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
//? if >=1.21.2 {
/*import net.minecraft.world.level.ScheduledTickAccess;
*///?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
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
    /*private void updateShape(BlockState blockState, LevelReader levelReader, ScheduledTickAccess scheduledTickAccess, BlockPos blockPos, Direction direction, BlockPos blockPos2, BlockState blockState2, RandomSource randomSource, CallbackInfoReturnable<BlockState> cir) {
        scheduledTickAccess.scheduleTick(blockPos, this, this.getDelayAfterPlace());
        cir.setReturnValue(super.updateShape(blockState, levelReader, scheduledTickAccess, blockPos, direction, blockPos2, blockState2, randomSource));
    }
    *///?} else {
    private void updateShape(BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2, CallbackInfoReturnable<BlockState> cir) {
        levelAccessor.scheduleTick(blockPos, this, this.getDelayAfterPlace());
        cir.setReturnValue(super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2));
    }
    //?}

    @Override
    public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        if (!FallingBlock.isFree(serverLevel.getBlockState(blockPos.below())) || blockPos.getY() < serverLevel./*? if >=1.21.2 {*//*getMinY*//*?} else {*/getMinBuildHeight/*?}*/())
            return;
        FallingBlockEntity.fall(serverLevel, blockPos, blockState);
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos blockPos, FallingBlockEntity fallingBlockEntity) {
        BlockState state = level.getBlockState(blockPos);
        if (state.is(Blocks.SNOW)) {
            ((FallingBlockAccessor) fallingBlockEntity).setCancelDrop(true);
            mergeSnowLayers(level, blockPos, fallingBlockEntity.getBlockState(), fallingBlockEntity.getBlockState().getValue(SnowLayerBlock.LAYERS));
        }
    }

    private void mergeSnowLayers(Level level, BlockPos blockPos, BlockState fallingState, int layers) {
        BlockPos targetPos = blockPos;
        int remainingLayers = layers;
        while (remainingLayers > 0) {
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.is(Blocks.SNOW)) {
                int totalLayers = targetState.getValue(SnowLayerBlock.LAYERS) + remainingLayers;
                level.setBlock(targetPos, targetState.setValue(SnowLayerBlock.LAYERS, Math.min(SnowLayerBlock.MAX_HEIGHT, totalLayers)), Block.UPDATE_ALL);
                remainingLayers = Math.max(0, totalLayers - SnowLayerBlock.MAX_HEIGHT);
            } else if (targetState.isAir() || targetState.canBeReplaced()) {
                int placedLayers = Math.min(SnowLayerBlock.MAX_HEIGHT, remainingLayers);
                level.setBlock(targetPos, fallingState.setValue(SnowLayerBlock.LAYERS, placedLayers), Block.UPDATE_ALL);
                remainingLayers -= placedLayers;
            } else {
                return;
            }
            targetPos = targetPos.above();
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
