package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Function;

@Mixin(SugarCaneBlock.class)
public class SugarCanesBlockMixin implements BonemealableBlock {
    @Unique
    protected int getPlantHeight(BlockGetter blockGetter, Function<Integer,BlockPos> blockPos) {
        int i;
        for(i = 0; i < 2 && blockGetter.getBlockState(blockPos.apply(i + 1)).is(Blocks.SUGAR_CANE); ++i) {
        }
        return i;
    }
    @Override
    public boolean isValidBonemealTarget(LevelReader levelReader, BlockPos blockPos, BlockState blockState/*? if <=1.20.2 {*//*, boolean bl*//*?}*/) {
        return getPlantHeight(levelReader, blockPos::below) + getPlantHeight(levelReader, blockPos::above) + 1 < 3;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel serverLevel, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
        int h = 2 - getPlantHeight(serverLevel, blockPos::below);
        for (int i = 0; i < h; i++) {
            serverLevel.setBlock(blockPos.above(i + 1),Blocks.SUGAR_CANE.defaultBlockState(),3);
        }
    }
}
