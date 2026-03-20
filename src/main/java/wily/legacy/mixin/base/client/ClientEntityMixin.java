package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ClientEntityAccessor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.init.LegacyGameRules;

import static wily.legacy.Legacy4JClient.keyFlyLeft;
import static wily.legacy.Legacy4JClient.keyFlyRight;

@Mixin(Entity.class)
public abstract class ClientEntityMixin implements ClientEntityAccessor {
    @Unique
    private float ridingEntityYRotDelta;

    @Unique
    private float ridingEntityXRotDelta;

    @Unique
    private boolean allowDisplayFireAnimation = true;
    @Shadow
    private boolean onGround;

    @Shadow
    public abstract Entity getVehicle();

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract void setXRot(float f);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract void setYRot(float f);

    @Shadow
    public abstract Level level();

    @Shadow
    public abstract boolean isInWater();

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void startRiding(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        this.ridingEntityXRotDelta = 0.0F;
        this.ridingEntityYRotDelta = 0.0F;
    }

    @Inject(method = "rideTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;positionRider(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER))
    private void modifyYawAndPitch(CallbackInfo ci) {
        //? if >=1.21.2 {
        if (getVehicle() instanceof AbstractMinecart && AbstractMinecart.useExperimentalMovement(level())) return;
        //?}
        if (getVehicle() == null || getVehicle().getControllingPassenger() == (Object) this || LegacyOptions.vehicleCameraRotation.get() == LegacyOptions.VehicleCameraRotation.NONE || !(getVehicle() instanceof LivingEntity && LegacyOptions.vehicleCameraRotation.get().isForLivingEntities() || !(getVehicle() instanceof LivingEntity) && LegacyOptions.vehicleCameraRotation.get().isForNonLivingEntities()))
            return;

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

    @ModifyReturnValue(method = "displayFireAnimation", at = @At("RETURN"))
    private boolean displayFireAnimation(boolean original) {
        return original && allowDisplayFireAnimation;
    }

    @Override
    public void setAllowDisplayFireAnimation(boolean displayFireAnimation) {
        this.allowDisplayFireAnimation = displayFireAnimation;
    }

    @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    protected Vec3 move(Entity instance, Vec3 vec3, Operation<Vec3> original) {
        boolean lastOnGround = onGround;
        onGround = onGround || instance instanceof Player p && p.getAbilities().flying;
        Vec3 collision = original.call(instance, vec3);
        onGround = lastOnGround;
        return collision;
    }

    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    public void moveRelative(float f, Vec3 vec3, CallbackInfo ci) {
        if (((Object) this) instanceof LocalPlayer p && LegacyGameRules.getSidedBooleanGamerule((Entity) (Object) this, LegacyGameRules.LEGACY_FLIGHT) && p.getAbilities().flying && p.isCreative() && !p.isSprinting()) {
            p.setDeltaMovement(p.getDeltaMovement().add(Legacy4J.getRelativeMovement(p, f, vec3, (keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown()) && p.input./*? if <1.21.5 {*//*leftImpulse*//*?} else {*/getMoveVector().x/*?}*/ == 0 ? 90 : 45)));
            ci.cancel();
        }
    }
}