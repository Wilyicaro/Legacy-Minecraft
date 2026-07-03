package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4JClient;

@Mixin(BambooStalkBlock.class)
public abstract class BambooStalkBlockMixin {
    @Unique
    private static final VoxelShape LEGACY_THIN_BAMBOO_STALK_SHAPE = Shapes.box(7.0D / 16.0D, 0.0D, 7.0D / 16.0D, 9.0D / 16.0D, 1.0D, 9.0D / 16.0D);
    @Unique
    private static final VoxelShape LEGACY_THICK_BAMBOO_STALK_SHAPE = Shapes.box(6.5D / 16.0D, 0.0D, 6.5D / 16.0D, 9.5D / 16.0D, 1.0D, 9.5D / 16.0D);

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!legacy$hasModOnServer()) return;
        Vec3 offset = state.getOffset(/*? if <1.21.5 {*/level, /*?}*/pos);
        VoxelShape shape = state.getValue(BambooStalkBlock.AGE) == 0 ? LEGACY_THIN_BAMBOO_STALK_SHAPE : LEGACY_THICK_BAMBOO_STALK_SHAPE;
        cir.setReturnValue(shape.move(offset.x, offset.y, offset.z));
    }

    @Unique
    private static boolean legacy$hasModOnServer() {
        return !FactoryAPI.isClient() || Legacy4JClient.hasModOnServer();
    }
}
