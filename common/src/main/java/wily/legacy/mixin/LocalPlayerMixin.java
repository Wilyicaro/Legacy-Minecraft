package wily.legacy.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;

import static wily.legacy.Legacy4JClient.*;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    @Shadow private boolean crouching;

    @Shadow protected abstract boolean isControlledCamera();

    @Shadow public Input input;

    @Shadow @Final protected Minecraft minecraft;

    @Shadow @Final public static Logger LOGGER;

    @Shadow private boolean lastOnGround;

    @Shadow protected abstract boolean hasEnoughFoodToStartSprinting();

    @Shadow public abstract void move(MoverType arg, Vec3 arg2);

    public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = 3))
    public boolean onGroundFlying(LocalPlayer instance) {
        return false;
    }
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = 0))
    public boolean onGroundCanSprint(LocalPlayer instance) {
        return true;
    }
    @Redirect(method = "aiStep", at = @At(value = "FIELD",target = "Lnet/minecraft/client/player/LocalPlayer;crouching:Z", opcode = Opcodes.PUTFIELD, ordinal = 0))
    public void aiStepCrouching(LocalPlayer instance, boolean value) {
        crouching = value && (onGround() || !isInWater()) && !getAbilities().flying && !isFallFlying();
    }
    @Inject(method = "aiStep", at = @At(value = "FIELD",target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z",opcode = Opcodes.PUTFIELD, ordinal = 1, shift = At.Shift.AFTER))
    public void aiStepStopCrouching(CallbackInfo ci) {
        minecraft.options.keyShift.setDown(false);
    }
    @Redirect(method = "aiStep", at = @At(value = "FIELD",target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z"))
    public boolean aiStepSprinting(LocalPlayer instance) {
        return false;
    }
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V", ordinal = 3))
    public void aiStepSprintingWater(LocalPlayer instance, boolean b) {
        if (!Legacy4JClient.isModEnabledOnServer() || !this.input.hasForwardImpulse() || !this.hasEnoughFoodToStartSprinting()) instance.setSprinting(b);
    }
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    public void aiStepFlyUpDown(LocalPlayer instance, Vec3 vec3) {
        if (Legacy4JClient.isModEnabledOnServer()) move(MoverType.SELF,vec3.with(Direction.Axis.Y,(vec3.y - getDeltaMovement().y) * (input.jumping ? (this.getAttributeValue(Attributes.JUMP_STRENGTH) + getJumpBoostPower()) * 6 : 3)));
        else setDeltaMovement(vec3);
    }
    @Redirect(method = "aiStep", at = @At(value = "FIELD",target = "Lnet/minecraft/client/player/Input;shiftKeyDown:Z", ordinal = 3))
    public boolean aiStepShift(Input instance) {
        if (!lastOnGround && !isSpectator()){
            checkSupportingBlock(true,null);
            lastOnGround = mainSupportingBlockPos.isPresent();
        }
        return instance.shiftKeyDown && (!Legacy4JClient.isModEnabledOnServer() || (!instance.jumping && !lastOnGround));
    }
    @Redirect(method = "aiStep", at = @At(value = "FIELD",target = "Lnet/minecraft/client/player/Input;jumping:Z", ordinal = 4))
    public boolean aiStepJump(Input instance) {
        return instance.jumping && (!Legacy4JClient.isModEnabledOnServer() || !isSprinting() || getXRot() <= 0 || !lastOnGround);
    }

    @Override
    public float maxUpStep() {
        return (Legacy4JClient.isModEnabledOnServer() && input.jumping && isSprinting() && getAbilities().flying ? 0.5f : 0) + super.maxUpStep();
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"))
    public void setYElytraFlightElevation(CallbackInfo ci) {
        if (!Legacy4JClient.isModEnabledOnServer()) return;
        if (isFallFlying() && getAbilities().mayfly && getAbilities().invulnerable && this.isControlledCamera() && jumping)
            setDeltaMovement(getDeltaMovement().with(Direction.Axis.Y, this.input.jumping ? this.getAbilities().getFlyingSpeed() * 12 : 0));

    }

    @Override
    public void moveRelative(float f, Vec3 vec3) {
        if (Legacy4JClient.isModEnabledOnServer() && getAbilities().flying && isCreative() && !isSprinting()) this.setDeltaMovement(getDeltaMovement().add(Legacy4J.getRelativeMovement(this,f,vec3,(keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown()) && input.leftImpulse == 0 ? 90 : 45)));
        else super.moveRelative(f, vec3);
    }

    @Inject(method = "aiStep", at = @At(value = "RETURN"))
    public void setYFlightElevation(CallbackInfo ci) {
        if (!Legacy4JClient.isModEnabledOnServer()) return;
        if (this.getAbilities().flying && this.isControlledCamera()) {
            if (keyFlyDown.isDown() && !keyFlyUp.isDown() || !keyFlyDown.isDown() && keyFlyUp.isDown() || keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown())
                setDeltaMovement(getDeltaMovement().add(0,(keyFlyUp.isDown() ? 1.5 : keyFlyDown.isDown() ? -1.5 : 0) * this.getAbilities().getFlyingSpeed(),0));
            if (getXRot() != 0 && (!lastOnGround || getXRot() < 0) && input.hasForwardImpulse() && isSprinting()) move(MoverType.SELF,new Vec3(0,-(getXRot() / 90) * input.forwardImpulse * getFlyingSpeed() * 2, 0));
        }
    }
    @Redirect(method = "handleConfusionTransitionEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;isPauseScreen()Z"))
    public boolean handleConfusionTransitionEffect(Screen instance) {
        return instance.isPauseScreen() || Legacy4JClient.isModEnabledOnServer();
    }
    @Inject(method = "serverAiStep", at = @At("RETURN"))
    public void serverAiStep(CallbackInfo ci) {
        if (!Legacy4JClient.isModEnabledOnServer()) return;
        if (this.isControlledCamera() && this.getAbilities().flying) {
            if (keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown()) xxa+= (keyFlyLeft.isDown() ? 12 : -12) * this.getAbilities().getFlyingSpeed();
            if (getXRot() != 0 && input.hasForwardImpulse() && isSprinting()) zza*=Math.max(0.1f,1 - Math.abs(getXRot() / 90));
        }
    }
    @Inject(method = "rideTick", at = @At("HEAD"))
    public void rideTick(CallbackInfo ci) {
        if (!Legacy4JClient.isModEnabledOnServer()) return;
        if (wantsToStopRiding() && this.isPassenger()) minecraft.options.keyShift.setDown(false);
    }
}
