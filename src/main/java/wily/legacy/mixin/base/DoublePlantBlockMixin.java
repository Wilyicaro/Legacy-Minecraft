package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
//? if >=1.21.5 {
/*import net.minecraft.world.level.block.VegetationBlock;
*///?}
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4JClient;

@Mixin(DoublePlantBlock.class)
public abstract class DoublePlantBlockMixin extends /*? if <1.21.5 {*/BushBlock/*?} else {*//*VegetationBlock*//*?}*/ {
    @Unique
    private static final VoxelShape LEGACY_TALL_FLOWER_SHAPE = Shapes.box(5.0D / 16.0D, 0.0D, 5.0D / 16.0D, 11.0D / 16.0D, 1.0D, 11.0D / 16.0D);

    protected DoublePlantBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!legacy$hasModOnServer()) {
            return super.getShape(state, level, pos, context);
        }
        if ((Object) this instanceof TallFlowerBlock) {
            return LEGACY_TALL_FLOWER_SHAPE;
        }
        if (state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN)) {
            Vec3 offset = state.getOffset(/*? if <1.21.2 {*/level, /*?}*/pos);
            return LEGACY_TALL_FLOWER_SHAPE.move(offset.x, offset.y, offset.z);
        }
        return super.getShape(state, level, pos, context);
    }

    @Unique
    private static boolean legacy$hasModOnServer() {
        return !FactoryAPI.isClient() || Legacy4JClient.hasModOnServer();
    }
}
