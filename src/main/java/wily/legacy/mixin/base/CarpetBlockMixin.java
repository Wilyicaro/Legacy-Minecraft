package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
*///?} else {
import net.minecraft.world.entity.vehicle.AbstractMinecart;
//?}
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class CarpetBlockMixin {
    private static final VoxelShape LEGACY_PLAYER_CARPET_COLLISION = Shapes.box(0.0D, -1.0D / 16.0D, 0.0D, 1.0D, 0.0D, 1.0D);

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext, CallbackInfoReturnable<VoxelShape> cir) {
        if (blockState.getBlock() instanceof CarpetBlock && collisionContext instanceof EntityCollisionContext entityCollisionContext) {
            if (entityCollisionContext.getEntity() instanceof AbstractMinecart minecart && isOnRails(minecart)) {
                cir.setReturnValue(Shapes.empty());
            } else if (entityCollisionContext.getEntity() instanceof Player) {
                cir.setReturnValue(LEGACY_PLAYER_CARPET_COLLISION);
            }
        }
    }

    @Unique
    private static boolean isOnRails(AbstractMinecart minecart) {
        BlockPos blockPos = minecart.blockPosition();
        return minecart.level().getBlockState(blockPos).is(BlockTags.RAILS) || minecart.level().getBlockState(blockPos.below()).is(BlockTags.RAILS);
    }
}
