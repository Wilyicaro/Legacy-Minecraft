package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Mixin(FlowerBlock.class)
public abstract class FlowerBlockMixin extends BushBlock implements BonemealableBlock {

    protected FlowerBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader levelReader, BlockPos blockPos, BlockState blockState, boolean bl) {
        for(int xd = -1; xd <= 1; ++xd) {
            for (int zd = -1; zd <= 1; ++zd) {
                if (zd == 0 && xd == 0) continue;
                BlockState side = levelReader.getBlockState(blockPos.offset(xd,0,zd));
                if (!side.isCollisionShapeFullBlock(levelReader,blockPos) || side.is(Blocks.GRASS_BLOCK)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel serverLevel, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
        for(int xd = -3; xd <= 3; ++xd) {
            for(int zd = -3; zd <= 3; ++zd) {
                if (zd == 0 && xd == 0) continue;
                for(int yd = -1; yd <= 1; ++yd) {
                    BlockPos blockPos2 = blockPos.offset(xd,yd,zd);
                    if (serverLevel.getBlockState(blockPos2).isAir() && randomSource.nextInt((int)Math.pow(2,Math.abs(xd) + Math.abs(zd) + Math.abs(yd))) == 0) {
                        BlockState blockState2 = serverLevel.getBlockState(blockPos2.below());
                        if (blockState2.is(Blocks.GRASS_BLOCK)) {
                            serverLevel.setBlock(blockPos2, (this == Blocks.POPPY || this == Blocks.DANDELION) && randomSource.nextInt(5) == 0 ? (this == Blocks.POPPY ? Blocks.DANDELION : Blocks.POPPY).defaultBlockState() : defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }
        }

    }
}
