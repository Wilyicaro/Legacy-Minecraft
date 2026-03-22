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
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.entity.LegacyLocalPlayer;
import wily.legacy.init.LegacyGameRules;

import static wily.legacy.Legacy4JClient.*;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer implements LegacyLocalPlayer {
    private static final float LEGACY_ELYTRA_STORAGE_PITCH = 25.0f;
    private static final double LEGACY_ELYTRA_STORAGE_BASE = 0.026;
    private static final double LEGACY_ELYTRA_STORAGE_SPEED_FACTOR = 0.02;
    private static final double LEGACY_ELYTRA_MAX_STORAGE = 3.0;
    private static final double LEGACY_ELYTRA_IDLE_DECAY = 0.85;
    private static final double LEGACY_ELYTRA_RELEASE_DECAY = 0.88;
    private static final double LEGACY_ELYTRA_RELEASE_END = 0.02;
    private static final double LEGACY_ELYTRA_RELEASE_IMPULSE = 0.18;

    @Shadow
    @Final
    public static Logger LOGGER;
    @Shadow
    public /*? if >=1.21.2 {*/ ClientInput/*?} else {*//*Input*//*?}*/ input;
    @Shadow
    @Final
    protected Minecraft minecraft;
    @Shadow
    private boolean crouching;
    @Shadow
    private boolean lastOnGround;


    private double legacyStoredElytraBoost;
    private double legacyActiveElytraReleaseBoost;
    private boolean legacyWasJumpHeld;

    public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Shadow
    protected abstract boolean isControlledCamera();

    //? if >=1.21.11 {
    @Unique
    boolean hasEnoughFoodToSprint() {
        return hasEnoughFoodToDoExhaustiveManoeuvres();
    }
    //?} else {
    /*protected abstract boolean hasEnoughFoodToSprint();
    *///?}

    @Shadow
    public abstract void move(MoverType arg, Vec3 arg2);

    @Shadow
    public abstract boolean isMovingSlowly();

    public boolean canSprintController() {
        return !this.isSprinting() && /*? if <1.21.5 {*//*this.hasEnoughFoodToStartSprinting()*//*?} else {*/this.hasEnoughFoodToSprint()/*?}*/ && !this.isUsingItem() && !this.isMovingSlowly() && this.minecraft.screen == null;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = /*? if <1.20.5 {*//*2*//*?} else if <1.21.5 {*//*3*//*?} else {*/1/*?}*/))
    public boolean onGroundFlying(boolean original) {
        return !LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT) && original;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSwimming()Z", ordinal = 2))
    public boolean swimmingFlying(boolean original) {
        return !LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SWIMMING) && original;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = 0))
    public boolean onGroundCanSprint(boolean original) {
        return true;
    }

    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;crouching:Z", opcode = Opcodes.PUTFIELD, ordinal = 0))
    public void aiStepCrouching(LocalPlayer instance, boolean value) {
        crouching = value && (!gameRules./*? if >=1.21.11 {*/get/*?} else {*//*getBoolean*//*?}*/(LegacyGameRules.LEGACY_FLIGHT) || ((onGround() || !isInWater()) && !getAbilities().flying && !isFallFlying()));
    }

    @Inject(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z", opcode = Opcodes.PUTFIELD, ordinal = 1, shift = At.Shift.AFTER))
    public void aiStepStopCrouching(CallbackInfo ci) {
        minecraft.options.keyShift.setDown(false);
    }

    @ModifyExpressionValue(method = /*? if <1.21.5 {*//*"aiStep"*//*?} else {*/"shouldStopRunSprinting"/*?}*/, at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z"))
    public boolean aiStepSprinting(boolean original) {
        return false;
    }

    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V", ordinal = 0))
    public boolean aiStepSprintController(LocalPlayer instance, boolean b) {
        return !controllerManager.isControllerTheLastInput();
    }

    @ModifyExpressionValue(method = {"shouldStopRunSprinting", "canStartSprinting"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z"))
    public boolean shouldStopRunSprinting(boolean original) {
        return (LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SWIMMING) && isInWater()) || original;
    }

    @ModifyExpressionValue(method = "isSprintingPossible", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isInShallowWater()Z"))
    public boolean isSprintingPossible(boolean original) {
        return !(LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SWIMMING) && isInWater()) && original;
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    public void aiStepFlyUpDown(LocalPlayer instance, Vec3 vec3) {
        if (LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT))
            move(MoverType.SELF, vec3.with(Direction.Axis.Y, (vec3.y - getDeltaMovement().y) * (input./*? if >=1.21.2 {*/keyPresses.jump()/*?} else {*//*jumping*//*?}*/ ? (/*? if <1.20.5 {*//*0.42f*//*?} else {*/this.getAttributeValue(Attributes.JUMP_STRENGTH)/*?}*/ + getJumpBoostPower()) * 6 : 3)));
        else setDeltaMovement(vec3);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(/*? if <1.21.2 {*//*value = "FIELD",target = "Lnet/minecraft/client/player/Input;shiftKeyDown:Z"*//*?} else {*/value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;shift()Z"/*?}*/, ordinal = /*? if <1.21.5 {*//*2*//*?} else {*/2/*?}*/))
    public boolean aiStepShift(boolean original) {
        if (!lastOnGround && !isSpectator()) {
            checkSupportingBlock(true, null);
            lastOnGround = mainSupportingBlockPos.isPresent();
        }
        return original && (!LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT) || (!input./*? if >=1.21.2 {*/keyPresses.jump()/*?} else {*//*jumping*//*?}*/ && !lastOnGround));
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(/*? if <1.21.2 {*//*value = "FIELD",target = "Lnet/minecraft/client/player/Input;jumping:Z"*//*?} else {*/value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;jump()Z"/*?}*/, ordinal = 3))
    public boolean aiStepJump(boolean original) {
        return original && (!LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT) || !isSprinting() || getXRot() <= 0 || !lastOnGround);
    }

    @Override
    public float maxUpStep() {
        return (LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT) && input./*? if >=1.21.2 {*/keyPresses.jump()/*?} else {*//*jumping*//*?}*/ && isSprinting() && getAbilities().flying ? 0.5f : 0) + super.maxUpStep();
    }

  
    private boolean legacy$isJumpHeld() {
        return input./*? if >=1.21.2 {*/keyPresses.jump()/*?} else {*//*jumping*//*?}*/;
    }

    
    private boolean legacy$canUseStoredElytraBoost() {
        return isFallFlying() && getAbilities().mayfly && getAbilities().invulnerable && this.isControlledCamera() &&
                //? if >=1.21.11 {
                gameRules.get(LegacyGameRules.LEGACY_FLIGHT);
                //?} else {
                /*gameRules.getRule(LegacyGameRules.LEGACY_FLIGHT).get();
                *///?}
    }

    // Charges extra  speed while the player is climbing and looking downward 
    private void legacy$chargeStoredElytraBoost(Vec3 deltaMovement) {
        if (getXRot() <= LEGACY_ELYTRA_STORAGE_PITCH) return;
        double pitchScale = Math.min(1.0, (getXRot() - LEGACY_ELYTRA_STORAGE_PITCH) / (90.0 - LEGACY_ELYTRA_STORAGE_PITCH));
        double storedThisTick = (LEGACY_ELYTRA_STORAGE_BASE + deltaMovement.horizontalDistance() * LEGACY_ELYTRA_STORAGE_SPEED_FACTOR) * pitchScale;
        legacyStoredElytraBoost = Math.min(LEGACY_ELYTRA_MAX_STORAGE, legacyStoredElytraBoost + storedThisTick);
    }

    // fires the stored climb speed along the player's current look vector upon climb release
    private void legacy$releaseStoredElytraBoost() {
        if (legacyStoredElytraBoost <= 0.0) return;
        legacy$cancelActiveElytraReleaseBoost();
        legacyActiveElytraReleaseBoost = legacyStoredElytraBoost;
        legacyStoredElytraBoost = 0.0;
    }

    // bleeds off stored speed when the player leaves elytra climb
    private void legacy$decayStoredElytraBoost(boolean hardReset) {
        if (hardReset) {
            legacyStoredElytraBoost = 0.0;
            legacy$cancelActiveElytraReleaseBoost();
            legacyWasJumpHeld = false;
            return;
        }
        legacyStoredElytraBoost *= LEGACY_ELYTRA_IDLE_DECAY;
        if (legacyStoredElytraBoost < 0.01) legacyStoredElytraBoost = 0.0;
    }

    // Clears the feed when the player exits state or starts charging again
    private Vec3 legacy$clearActiveElytraReleaseBoost(Vec3 deltaMovement) {
        legacy$cancelActiveElytraReleaseBoost();
        return deltaMovement;
    }

    //
    private Vec3 legacy$updateActiveElytraReleaseBoost(Vec3 deltaMovement) {
        if (legacyActiveElytraReleaseBoost <= LEGACY_ELYTRA_RELEASE_END) {
            legacyActiveElytraReleaseBoost = 0.0;
            return deltaMovement;
        }
        Vec3 lookAngle = getLookAngle();
        double lookLength = lookAngle.lengthSqr();
        if (lookLength <= 1.0E-4) return deltaMovement;
        Vec3 releaseImpulse = lookAngle.scale((legacyActiveElytraReleaseBoost * LEGACY_ELYTRA_RELEASE_IMPULSE) / Math.sqrt(lookLength));
        legacyActiveElytraReleaseBoost *= LEGACY_ELYTRA_RELEASE_DECAY;
        return deltaMovement.add(releaseImpulse);
    }

    
    private void legacy$cancelActiveElytraReleaseBoost() {
        legacyActiveElytraReleaseBoost = 0.0;
    }

    
    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"))
    public void setYElytraFlightElevation(CallbackInfo ci) {
        Vec3 deltaMovement = getDeltaMovement();
        if (!LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT)) {
            setDeltaMovement(legacy$clearActiveElytraReleaseBoost(deltaMovement));
            legacy$decayStoredElytraBoost(true);
            return;
        }
        if (!legacy$canUseStoredElytraBoost()) {
            setDeltaMovement(legacy$clearActiveElytraReleaseBoost(deltaMovement));
            legacy$decayStoredElytraBoost(!isFallFlying());
            return;
        }
        boolean jumpHeld = legacy$isJumpHeld();
        boolean releasedJumpThisTick = legacyWasJumpHeld && !jumpHeld;
        legacyWasJumpHeld = jumpHeld;
        if (jumpHeld) {
            deltaMovement = legacy$clearActiveElytraReleaseBoost(deltaMovement);
            deltaMovement = deltaMovement.with(Direction.Axis.Y, this.getAbilities().getFlyingSpeed() * 12);
            legacy$chargeStoredElytraBoost(deltaMovement);
        } else if (!releasedJumpThisTick) {
            legacy$decayStoredElytraBoost(false);
        }
        if (releasedJumpThisTick) {
            legacy$releaseStoredElytraBoost();
        }
        setDeltaMovement(legacy$updateActiveElytraReleaseBoost(deltaMovement));
    }

    @Inject(method = "aiStep", at = @At(value = "RETURN"))
    public void setYFlightElevation(CallbackInfo ci) {
        if (!LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT)) return;
        if (this.getAbilities().flying && this.isControlledCamera()) {
            if (keyFlyDown.isDown() && !keyFlyUp.isDown() || !keyFlyDown.isDown() && keyFlyUp.isDown() || keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown())
                setDeltaMovement(getDeltaMovement().add(0, (keyFlyUp.isDown() ? 1.5 : keyFlyDown.isDown() ? -1.5 : 0) * this.getAbilities().getFlyingSpeed(), 0));
            if (getXRot() != 0 && (!lastOnGround || getXRot() < 0) && input.hasForwardImpulse() && isSprinting())
                move(MoverType.SELF, new Vec3(0, -(getXRot() / 90) * input./*? if <1.21.5 {*//*forwardImpulse*//*?} else {*/getMoveVector().y/*?}*/ * getFlyingSpeed() * 2, 0));
        }
    }

    @ModifyExpressionValue(method = /*? if <1.20.5 {*//*"handleNetherPortalClient"*//*?} else if <1.21.5 {*//*"handleConfusionTransitionEffect"*//*?} else {*/"handlePortalTransitionEffect"/*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;isAllowedInPortal()Z"))
    public boolean handleConfusionTransitionEffect(boolean original) {
        return original || Legacy4JClient.hasModOnServer();
    }

    @Inject(method = /*? if <1.21.5 {*//*"serverAiStep"*//*?} else {*/"applyInput"/*?}*/, at = @At("RETURN"))
    public void applyInput(CallbackInfo ci) {
        if (this.isControlledCamera() && this.getAbilities().flying && LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT)) {
            if (keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown())
                xxa += (keyFlyLeft.isDown() ? 12 : -12) * this.getAbilities().getFlyingSpeed();
            if (getXRot() != 0 && input.hasForwardImpulse() && isSprinting())
                zza *= Math.max(0.1f, 1 - Math.abs(getXRot() / 90));
        }
        if (Legacy4JClient.hasModOnServer() && wantsToStopRiding() && this.isPassenger()) {
            minecraft.options.keyShift.setDown(false);
        }
    }
}
