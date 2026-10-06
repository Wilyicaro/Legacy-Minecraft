package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
//? if <1.21.2 {
/*import net.minecraft.client.player.Input;
 *///?} else {
import net.minecraft.client.player.ClientInput;
//?}
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.FirstPersonDropAnimation;
import wily.legacy.client.control.ControllerManager;
import wily.legacy.entity.LegacyLocalPlayer;
import wily.legacy.init.LegacyGameRules;

import static wily.legacy.Legacy4JClient.*;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer implements LegacyLocalPlayer {
    @Shadow
    public /*? if >=1.21.2 {*/ ClientInput/*?} else {*//*Input*//*?}*/ input;
    @Shadow
    @Final
    protected Minecraft minecraft;
    @Shadow
    private boolean crouching;
    @Shadow
    private boolean lastOnGround;

    private float legacyUnderwaterVisionTime;
    private int legacyFlightSprintTicks;

    public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Shadow
    protected abstract boolean isControlledCamera();

    @Shadow
    public abstract void move(MoverType arg, Vec3 arg2);

    @Shadow
    public abstract boolean isMovingSlowly();

    @Inject(method = "drop", at = @At("RETURN"))
    private void playDropAnimation(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) FirstPersonDropAnimation.start();
    }

    @Override
    public float getLegacyUnderwaterVisionClarity() {
        if (legacyUnderwaterVisionTime < 5.0F) return Mth.lerp(legacyUnderwaterVisionTime / 5.0F, 0.0F, 0.6F);
        return Mth.lerp((legacyUnderwaterVisionTime - 5.0F) / 5.0F, 0.6F, 1.0F);
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void legacy$updateUnderwaterVisionTime(CallbackInfo ci) {
        float change = isAlive() && isInWater() && isEyeInFluid(FluidTags.WATER) ? 0.05F : -0.5F;
        legacyUnderwaterVisionTime = Mth.clamp(legacyUnderwaterVisionTime + change, 0.0F, 10.0F);
    }

    public boolean canSprintController() {
        return !this.isSprinting() && !this.isFallFlying() && /*? if <1.21.5 {*//*this.hasEnoughFoodToStartSprinting()*//*?} else {*/this.hasEnoughFoodToDoExhaustiveManoeuvres()/*?}*/ && !this.isUsingItem() && !this.isMovingSlowly() && this.minecraft.screen == null;
    }

    @ModifyExpressionValue(method = /*? if <1.21.5 {*//*"aiStep"*//*?} else {*/"shouldStopRunSprinting"/*?}*/, at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z"))
    public boolean ignoreSprintCollision(boolean original) {
        return false;
    }

    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V", ordinal = 0))
    public boolean allowKeyboardSprint(LocalPlayer instance, boolean b) {
        return !ControllerManager.getInstance().isControllerTheLastInput();
    }

    @ModifyExpressionValue(method = {"shouldStopRunSprinting", "canStartSprinting"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z"))
    public boolean legacySwimmingSprintCheck(boolean original) {
        return (LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SWIMMING.get()) && isInWater()) || original;
    }

    @ModifyExpressionValue(method = "isSprintingPossible", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isInShallowWater()Z"))
    public boolean legacyShallowWaterSprintCheck(boolean original) {
        return !(LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SWIMMING.get()) && isInWater()) && original;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = /*? if <1.20.5 {*//*2*//*?} else if <1.21.5 {*//*3*//*?} else {*/1/*?}*/))
    public boolean legacyFlightLandingCheck(boolean original) {
        return !LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT.get()) && original;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSwimming()Z", ordinal = 2))
    public boolean legacyFlightSwimmingCheck(boolean original) {
        return !LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SWIMMING.get()) && original;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = 0))
    public boolean legacyFlightTakeoffCheck(boolean original) {
        return true;
    }

    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;crouching:Z", opcode = Opcodes.PUTFIELD, ordinal = 0))
    public void applyLegacyCrouching(LocalPlayer instance, boolean value) {
        crouching = value && (!gameRules.get(LegacyGameRules.LEGACY_FLIGHT.get()) || ((onGround() || !isInWater()) && !getAbilities().flying && !isFallFlying()));
    }

    @Inject(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z", opcode = Opcodes.PUTFIELD, ordinal = 1, shift = At.Shift.AFTER))
    public void releaseCrouchAfterFlightToggle(CallbackInfo ci) {
        minecraft.options.keyShift.setDown(false);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    public void applyLegacyVerticalFlight(LocalPlayer instance, Vec3 vec3) {
        if (LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT.get())) {
            double speed = getAbilities().getFlyingSpeed() / 0.05f;
            double vertical = vec3.y > getDeltaMovement().y ? getAttributeValue(Attributes.JUMP_STRENGTH) + getJumpBoostPower() : -0.57;
            move(MoverType.SELF, new Vec3(0, vertical * speed, 0));
        } else setDeltaMovement(vec3);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(/*? if <1.21.2 {*//*value = "FIELD",target = "Lnet/minecraft/client/player/Input;shiftKeyDown:Z"*//*?} else {*/value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;shift()Z"/*?}*/, ordinal = /*? if <1.21.5 {*//*2*//*?} else {*/2/*?}*/))
    public boolean applyLegacyFlightDescentInput(boolean original) {
        if (!lastOnGround && !isSpectator()) {
            checkSupportingBlock(true, null);
            lastOnGround = mainSupportingBlockPos.isPresent();
        }
        return original && (!LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT.get()) || (!input./*? if >=1.21.2 {*/keyPresses.jump()/*?} else {*//*jumping*//*?}*/ && !lastOnGround));
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(/*? if <1.21.2 {*//*value = "FIELD",target = "Lnet/minecraft/client/player/Input;jumping:Z"*//*?} else {*/value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;jump()Z"/*?}*/, ordinal = 3))
    public boolean applyLegacyFlightAscentInput(boolean original) {
        return original && (!LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT.get()) || !isSprinting() || getXRot() <= 0 || !lastOnGround);
    }

    @Override
    public float maxUpStep() {
        return (LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT.get()) && input./*? if >=1.21.2 {*/keyPresses.jump()/*?} else {*//*jumping*//*?}*/ && isSprinting() && getAbilities().flying ? 0.5f : 0) + super.maxUpStep();
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void updateLegacyFlightSprint(CallbackInfo ci) {
        legacyFlightSprintTicks = isSprinting() ? Math.min(legacyFlightSprintTicks + 1, 10) : 0;
    }

    @Inject(method = "aiStep", at = @At(value = "RETURN"))
    public void applyLegacyFlightElevation(CallbackInfo ci) {
        if (!LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT.get())) return;
        if (this.getAbilities().flying && this.isControlledCamera()) {
            double speed = getAbilities().getFlyingSpeed() / 0.05f;
            if (isSprinting()) {
                float boost = legacyFlightSprintTicks / 10.0f;
                move(MoverType.SELF, getLookAngle().scale(input.getMoveVector().y * boost * boost * speed));
            } else if (keyFlyUp.isDown() != keyFlyDown.isDown()) {
                move(MoverType.SELF, new Vec3(0, (keyFlyUp.isDown() ? 0.1 : -0.1) * speed, 0));
            }
        }
    }

    @Inject(method = /*? if <1.21.5 {*//*"serverAiStep"*//*?} else {*/"applyInput"/*?}*/, at = @At("RETURN"))
    public void applyLegacyMovementInput(CallbackInfo ci) {
        if (this.isControlledCamera() && this.getAbilities().flying && LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT.get())) {
            if (keyFlyLeft.isDown() != keyFlyRight.isDown()) {
                float yaw = Mth.floor(getYRot() / 90.0f + 0.5f) * Mth.HALF_PI;
                double speed = (keyFlyLeft.isDown() ? 3 : -3) * getAbilities().getFlyingSpeed();
                setDeltaMovement(Mth.cos(yaw) * speed, getDeltaMovement().y, Mth.sin(yaw) * speed);
            }
        }
        if (Legacy4JClient.hasModOnServer() && wantsToStopRiding() && this.isPassenger()) {
            minecraft.options.keyShift.setDown(false);
        }
    }

    @Override
    public boolean isLegacyElytraBoosting() {
        return isFallFlying() && getAbilities().mayfly && getAbilities().invulnerable && isControlledCamera()
                && input./*? if >=1.21.2 {*/keyPresses.jump()/*?} else {*//*jumping*//*?}*/
                && LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT.get());
    }

    @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;itemUseSpeedMultiplier()F"))
    private float legacyShieldSpeedMultiplier(float original) {
        return isMovingSlowly() && getUseItem().getItem() instanceof ShieldItem
                && LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SHIELD_CONTROLS.get()) ? 1.0f : original;
    }

    @ModifyExpressionValue(method = /*? if <1.20.5 {*//*"handleNetherPortalClient"*//*?} else if <1.21.5 {*//*"handleConfusionTransitionEffect"*//*?} else {*/"handlePortalTransitionEffect"/*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;isAllowedInPortal()Z"))
    public boolean allowMenusInPortalOnLegacyServer(boolean original) {
        return original || Legacy4JClient.hasModOnServer();
    }

    @ModifyArg(method = /*? if <1.20.5 {*//*"handleNetherPortalClient"*//*?} else if <1.21.5 {*//*"handleConfusionTransitionEffect"*//*?} else {*/"handlePortalTransitionEffect"/*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forLocalAmbience(Lnet/minecraft/sounds/SoundEvent;FF)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"), index = 1)
    private float legacyPortalTriggerPitch(float pitch) {
        return 1.0f;
    }

    @ModifyArg(method = /*? if <1.20.5 {*//*"handleNetherPortalClient"*//*?} else if <1.21.5 {*//*"handleConfusionTransitionEffect"*//*?} else {*/"handlePortalTransitionEffect"/*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forLocalAmbience(Lnet/minecraft/sounds/SoundEvent;FF)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"), index = 2)
    private float legacyPortalTriggerVolume(float volume) {
        return 1.0f;
    }
}
