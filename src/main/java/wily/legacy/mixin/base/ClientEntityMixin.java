package wily.legacy.mixin.base;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOption;

import static wily.legacy.Legacy4JClient.keyFlyLeft;
import static wily.legacy.Legacy4JClient.keyFlyRight;

@Mixin(Entity.class)
public abstract class ClientEntityMixin {
    @Unique
    private float ridingEntityYRotDelta;

    @Unique
    private float ridingEntityXRotDelta;


    @Shadow
    public abstract Entity getVehicle();

    @Shadow public abstract void setYRot(float f);

    @Shadow public abstract void setXRot(float f);

    @Shadow public abstract float getXRot();

    @Shadow public abstract float getYRot();

    @Shadow public abstract boolean isUnderWater();

    @Shadow private boolean onGround;

    @Shadow protected abstract Vec3 collide(Vec3 arg);

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("HEAD"))
    private void startRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
        this.ridingEntityXRotDelta = 0.0F;
        this.ridingEntityYRotDelta = 0.0F;
    }
    @Inject(method = "rideTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;positionRider(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER))
    private void modifyYawAndPitch(CallbackInfo ci) {
        //? if >=1.21.2 {
        if (getVehicle() instanceof Minecart) return;
        //?}
        if (getVehicle() == null || getVehicle().getControllingPassenger() == (Object) this || LegacyOption.vehicleCameraRotation.get() == LegacyOption.VehicleCameraRotation.NONE || !(getVehicle() instanceof LivingEntity && LegacyOption.vehicleCameraRotation.get().isForLivingEntities() || !(getVehicle() instanceof LivingEntity) && LegacyOption.vehicleCameraRotation.get().isForNonLivingEntities())) return;

        this.ridingEntityYRotDelta = this.ridingEntityYRotDelta + (this.getVehicle().getYRot() - this.getVehicle().yRotO);

        while (this.ridingEntityYRotDelta >= 180.0) this.ridingEntityYRotDelta -= 360.0F;

        while (this.ridingEntityYRotDelta < -180.0) this.ridingEntityYRotDelta += 360.0F;

        while (this.ridingEntityXRotDelta >= 180.0) this.ridingEntityXRotDelta -= 360.0F;

        while (this.ridingEntityXRotDelta < -180.0) this.ridingEntityXRotDelta += 360.0F;

        float ridingEntityYRotDeltaSmooth = this.ridingEntityYRotDelta * 0.5F;
        float ridingEntityXRotDeltaSmooth = this.ridingEntityXRotDelta * 0.5F;

        float maxTurn = 10F;

        if (ridingEntityYRotDeltaSmooth > maxTurn) ridingEntityYRotDeltaSmooth = maxTurn;

        if (ridingEntityYRotDeltaSmooth < -maxTurn) ridingEntityYRotDeltaSmooth = -maxTurn;

        if (ridingEntityXRotDeltaSmooth > maxTurn) ridingEntityXRotDeltaSmooth = maxTurn;

        if (ridingEntityXRotDeltaSmooth < -maxTurn) ridingEntityXRotDeltaSmooth = -maxTurn;

        this.ridingEntityYRotDelta -= ridingEntityYRotDeltaSmooth;
        this.ridingEntityXRotDelta -= ridingEntityXRotDeltaSmooth;
        this.setYRot(this.getYRot() + ridingEntityYRotDeltaSmooth);
        this.setXRot(this.getXRot() + ridingEntityXRotDeltaSmooth);
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    protected Vec3 move(Entity instance, Vec3 vec31) {
        boolean lastOnGround = onGround;
        onGround = onGround || instance instanceof Player p && p.getAbilities().flying;
        Vec3 collision = collide(vec31);
        onGround = lastOnGround;
        return collision;
    }
    @Redirect(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"))
    protected boolean updateSwimming(Entity instance) {
        return (!instance.level().isClientSide || FactoryAPIClient.hasModOnServer || isUnderWater()) && instance.isInWater();
    }

    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    public void moveRelative(float f, Vec3 vec3, CallbackInfo ci) {
        if (((Object)this) instanceof LocalPlayer p && FactoryAPIClient.hasModOnServer && p.getAbilities().flying && p.isCreative() && !p.isSprinting()){
            p.setDeltaMovement(p.getDeltaMovement().add(Legacy4J.getRelativeMovement(p,f,vec3,(keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown()) && p.input.leftImpulse == 0 ? 90 : 45)));
            ci.cancel();
        }
    }
}