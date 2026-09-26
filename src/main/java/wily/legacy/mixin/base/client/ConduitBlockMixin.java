package wily.legacy.mixin.base.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ConduitBlock;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;

@Mixin(ConduitBlock.class)
public abstract class ConduitBlockMixin {
    @Unique
    private static final VoxelShape LEGACY_CONDUIT_INACTIVE_SHAPE = Shapes.box(0.25D, 0.0D, 0.25D, 0.75D, 0.5D, 0.75D);
    @Unique
    private static final VoxelShape LEGACY_CONDUIT_ACTIVE_SHAPE = Shapes.box(0.25D, 0.2D, 0.25D, 0.75D, 0.7D, 0.75D);

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!Legacy4JClient.hasModOnServer()) return;
        boolean active = level.getBlockEntity(pos) instanceof ConduitBlockEntity conduit && conduit.isActive();
        cir.setReturnValue(active ? LEGACY_CONDUIT_ACTIVE_SHAPE : LEGACY_CONDUIT_INACTIVE_SHAPE);
    }
}
