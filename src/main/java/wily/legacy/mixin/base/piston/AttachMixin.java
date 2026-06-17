package wily.legacy.mixin.base.piston;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.Legacy4J;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AttachMixin {
    @Unique
    private static final TagKey<Block> LEGACY$PISTON_ATTACHED_BLOCK = TagKey.create(Registries.BLOCK, Legacy4J.createModLocation("piston_attached"));

    @Shadow
    protected abstract BlockState asState();

    @ModifyReturnValue(method = "canSurvive", at = @At("RETURN"))
    private boolean canSurvive(boolean original, LevelReader levelReader, BlockPos blockPos) {
        if (original) return true;
        BlockState state = asState();
        if (!state.is(LEGACY$PISTON_ATTACHED_BLOCK)) return false;
        Direction supportFace = getSupportFace(state);
        if (supportFace == null) return false;
        Direction pistonFacing = getPistonFacing(levelReader.getBlockState(blockPos.relative(supportFace.getOpposite())));
        return pistonFacing != null && supportFace != pistonFacing;
    }

    @Unique
    private static Direction getSupportFace(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof FaceAttachedHorizontalDirectionalBlock) {
            return switch (state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)) {
                case CEILING -> Direction.DOWN;
                case FLOOR -> Direction.UP;
                case WALL -> state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
            };
        }
        return state.hasProperty(WallTorchBlock.FACING) ? state.getValue(WallTorchBlock.FACING) : null;
    }

    @Unique
    private static Direction getPistonFacing(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof PistonBaseBlock) return state.getValue(PistonBaseBlock.FACING);
        if (block instanceof PistonHeadBlock) return state.getValue(PistonHeadBlock.FACING);
        return block instanceof MovingPistonBlock ? state.getValue(MovingPistonBlock.FACING) : null;
    }
}
