package wily.legacy.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
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
    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("HEAD"))
    private void startRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
        this.ridingEntityXRotDelta = 0.0F;
        this.ridingEntityYRotDelta = 0.0F;
    }
    @Inject(method = "rideTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;positionRider(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER))
    private void modifyYawAndPitch(CallbackInfo ci) {
        if (this.getVehicle().getControllingPassenger() == (Object) this || getVehicle() instanceof LivingEntity) return;

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

}