package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends Entity {
    //? if <1.21.2 {
    
    /*@Shadow protected abstract void comeOffTrack();

    @Shadow public abstract void activateMinecart(int i, int j, int k, boolean bl);

    @Shadow protected abstract void moveAlongTrack(BlockPos arg, BlockState arg2);

    @Shadow private boolean onRails;

    @Shadow public abstract AbstractMinecart.Type getMinecartType();

    @Shadow private boolean flipped;

    @Redirect(method = "moveAlongTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 4))
    public Vec3 movePlayerAlongTrack(Vec3 instance, double d, double e, double f) {
        Player p = (Player) getFirstPassenger();
        if (p.zza <= 0 || (this.getDeltaMovement().horizontalDistanceSqr()) >= 0.01D) return instance;
        Vec3 movement = Legacy4J.getRelativeMovement(p,1.0f,new Vec3(0,0,p.zza),0);
        return instance.add(movement.x*0.1f,e,movement.z*0.1f);
    }
    @Redirect(method = "moveAlongTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;isInWater()Z"))
    public boolean moveAlongTrack(AbstractMinecart instance) {
        return false;
    }
    @Inject(method = "tick", at = @At("RETURN"))
    public void tick(CallbackInfo ci) {
        if (!isAlive() || this.level().isClientSide) return;
        int i = Mth.floor(this.getX());
        int j = Mth.floor(this.getY());
        int k = Mth.floor(this.getZ());
        if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
            --j;
        }

        BlockPos blockPos = new BlockPos(i, j, k);
        BlockState blockState = this.level().getBlockState(blockPos);
        this.onRails = BaseRailBlock.isRail(blockState);
        if (this.onRails) {
            this.moveAlongTrack(blockPos, blockState);
            if (blockState.is(Blocks.ACTIVATOR_RAIL)) {
                this.activateMinecart(i, j, k, blockState.getValue(PoweredRailBlock.POWERED));
            }
        } else {
            this.comeOffTrack();
        }

        this.checkInsideBlocks();
        this.setXRot(0.0F);
        double d = this.xo - this.getX();
        double e = this.zo - this.getZ();
        if (d * d + e * e > 0.001) {
            this.setYRot((float)(Mth.atan2(e, d) * 180.0 / Math.PI));
            if (this.flipped) {
                this.setYRot(this.getYRot() + 180.0F);
            }
        }

        double f = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        if (f < -170.0 || f >= 170.0) {
            this.setYRot(this.getYRot() + 180.0F);
            this.flipped = !this.flipped;
        }

        this.setRot(this.getYRot(), this.getXRot());
        if (this.getMinecartType() == AbstractMinecart.Type.RIDEABLE && this.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            this.level().getEntities(this, this.getBoundingBox().inflate(0.20000000298023224, 0.0, 0.20000000298023224), EntitySelector.pushableBy(this)).forEach(entity -> {
                if (!(entity instanceof Player) && !(entity instanceof IronGolem) && !(entity instanceof AbstractMinecart) && !this.isVehicle() && !entity.isPassenger()) {
                    entity.startRiding(this);
                } else {
                    entity.push(this);
                }
            });
        } else {
            for (Entity entity2 : this.level().getEntities(this, this.getBoundingBox().inflate(0.20000000298023224, 0.0, 0.20000000298023224))) {
                if (!this.hasPassenger(entity2) && entity2.isPushable() && entity2 instanceof AbstractMinecart) {
                    entity2.push(this);
                }
            }
        }

        this.updateInWaterStateAndDoFluidPushing();
        if (this.isInLava()) {
            this.lavaHurt();
            this.fallDistance *= 0.5F;
        }
    }

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    protected void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(8d / 20d);
    }

    *///?}

    public AbstractMinecartMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = "applyNaturalSlowdown", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;isInWater()Z"))
    public boolean applyNaturalSlowdown(AbstractMinecart instance) {
        return false;
    }

}
