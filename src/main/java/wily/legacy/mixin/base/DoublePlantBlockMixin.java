package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.Legacy4J;

@Mixin(DoublePlantBlock.class)
public abstract class DoublePlantBlockMixin extends VegetationBlock {
    @Unique
    private static final VoxelShape LEGACY_TALL_FLOWER_SHAPE = Block.column(6.0, 0.0, 16.0);

    protected DoublePlantBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!Legacy4J.canApplyServerAuthoritativeChanges()) return super.getShape(state, level, pos, context);
        if ((Object) this instanceof TallFlowerBlock) {
            return LEGACY_TALL_FLOWER_SHAPE;
        }
        if (state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN)) {
            Vec3 offset = state.getOffset(pos);
            return LEGACY_TALL_FLOWER_SHAPE.move(offset.x, offset.y, offset.z);
        }
        return super.getShape(state, level, pos, context);
    }
}
