package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;

@Mixin(BambooSaplingBlock.class)
public abstract class BambooSaplingBlockMixin {
    @Unique
    private static final VoxelShape LEGACY_BAMBOO_SAPLING_SHAPE = Shapes.box(1.75D / 16.0D, 0.0D, 1.75D / 16.0D, 14.25D / 16.0D, 12.0D / 16.0D, 14.25D / 16.0D);

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!Legacy4J.canApplyServerAuthoritativeChanges()) return;
        cir.setReturnValue(LEGACY_BAMBOO_SAPLING_SHAPE);
    }
}
