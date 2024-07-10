package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends Entity {
    public AbstractMinecartMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    protected void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        BlockPos blockPos = blockPosition();
        BlockState blockState = this.level().getBlockState(blockPos);
        Block block = blockState.getBlock();
        BlockState blockBelowState = this.level().getBlockState(blockPos.below());
        Block blockBelow = blockBelowState.getBlock();
        Vec3 minecartMovement = this.getDeltaMovement();

        if (Math.abs(minecartMovement.x) < 0.5 && Math.abs(minecartMovement.z) < 0.5) {
            return;
        }

        if (blockState.isAir() && blockBelow instanceof BaseRailBlock baseRailBlock) {
            RailShape currentRailShape = blockBelowState.getValue(baseRailBlock.getShapeProperty());
            switch (currentRailShape) {
                case ASCENDING_EAST, ASCENDING_NORTH, ASCENDING_SOUTH, ASCENDING_WEST -> {
                    return;
                }
            }
        }

        if (block instanceof BaseRailBlock baseRailBlock) {
            RailShape currentRailShape = blockState.getValue(baseRailBlock.getShapeProperty());
            switch (currentRailShape) {
                case SOUTH_EAST, SOUTH_WEST, NORTH_WEST, NORTH_EAST, ASCENDING_EAST, ASCENDING_NORTH, ASCENDING_SOUTH, ASCENDING_WEST -> {
                    return;
                }
            }

            Vec3i runningDirection = getMinecartRunningDirection(currentRailShape, minecartMovement);

            for (int i = 0; i < 2; i++) {
                RailShape railShapeAtOffset;

                if (runningDirection == null) {
                    return;
                }

                railShapeAtOffset = getRailShapeAtOffset(new Vec3i(runningDirection.getX() * i, 0, runningDirection.getZ() * i), blockPos, level());
                if (railShapeAtOffset == null) {
                    return;
                }

                switch (railShapeAtOffset) {
                    case SOUTH_EAST, SOUTH_WEST, NORTH_WEST, NORTH_EAST, ASCENDING_EAST, ASCENDING_NORTH, ASCENDING_SOUTH, ASCENDING_WEST -> {
                        return;
                    }
                }
            }
        }
        cir.setReturnValue(cir.getReturnValueD() * 2);
    }
    private static RailShape getRailShapeAtOffset(Vec3i blockOffset, BlockPos blockPos, Level level) {
        BlockState blockState = level.getBlockState(blockPos.offset(blockOffset));
        if (blockState.getBlock() instanceof BaseRailBlock abstractRailBlock) {
            return blockState.getValue(abstractRailBlock.getShapeProperty());
        } else {
            return null;
        }
    }

    private static Vec3i getMinecartRunningDirection(RailShape railShape, Vec3 vec) {
        if (railShape == RailShape.EAST_WEST || railShape == RailShape.NORTH_SOUTH ) {
            return new Vec3i(railShape == RailShape.EAST_WEST ? (int) Math.signum(vec.x) : 0, 0, railShape == RailShape.NORTH_SOUTH ? (int) Math.signum(vec.z) : 0);
        }
        return null;
    }
}
