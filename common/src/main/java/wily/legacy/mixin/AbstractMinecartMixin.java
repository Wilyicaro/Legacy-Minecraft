package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends VehicleEntity {
    @Shadow protected abstract double getMaxSpeed();

    public AbstractMinecartMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    protected void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue((8d / 20d) * (canHaveHigherMaxSpeed() ? 2 : 1));
    }
    @Redirect(method = "comeOffTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;getMaxSpeed()D"))
    protected double getMaxSpeedOffTrack(AbstractMinecart instance) {
        return getMaxSpeed() * 2;
    }
    @Unique
    protected boolean canHaveHigherMaxSpeed() {
        BlockPos blockPos = blockPosition();
        BlockState blockState = this.level().getBlockState(blockPos);
        Block block = blockState.getBlock();
        BlockState blockBelowState = this.level().getBlockState(blockPos.below());
        Block blockBelow = blockBelowState.getBlock();
        Vec3 minecartMovement = this.getDeltaMovement();

        if (Math.abs(minecartMovement.x) < 0.5 && Math.abs(minecartMovement.z) < 0.5) {
            return false;
        }

        if (blockState.isAir() && blockBelow instanceof BaseRailBlock baseRailBlock) {
            RailShape currentRailShape = blockBelowState.getValue(baseRailBlock.getShapeProperty());
            switch (currentRailShape) {
                case ASCENDING_EAST, ASCENDING_NORTH, ASCENDING_SOUTH, ASCENDING_WEST -> {
                    return false;
                }
            }
        }
        if (block instanceof BaseRailBlock baseRailBlock) {
            RailShape currentRailShape = blockState.getValue(baseRailBlock.getShapeProperty());
            switch (currentRailShape) {
                case SOUTH_EAST, SOUTH_WEST, NORTH_WEST, NORTH_EAST -> {
                    return false;
                }
            }
        }
        return true;
    }
}
