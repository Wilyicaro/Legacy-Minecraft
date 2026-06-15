package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
//? if <1.21.2 {
import net.minecraft.client.player.Input;
 //?} else {
/*import net.minecraft.client.player.ClientInput;
*///?}
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ShieldItem;
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
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.entity.LegacyLocalPlayer;
import wily.legacy.entity.LegacyShieldPlayer;
import wily.legacy.init.LegacyGameRules;

import static wily.legacy.Legacy4JClient.*;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer implements LegacyLocalPlayer {
    @Unique
    private static final float LEGACY_ELYTRA_STORAGE_PITCH = 25.0f;
    @Unique
    private static final double LEGACY_ELYTRA_BOOST_BOB_GRAVITY = 0.08;
    @Unique
    private static final double LEGACY_ELYTRA_BOOST_BOB_DRAG = 0.98;
    @Unique
    private static final double LEGACY_ELYTRA_BOOST_BOB_HANDOFF_END = 0.02;
    @Unique
    private static final double LEGACY_ELYTRA_BOOST_BOB_EXIT_HANDOFF = 0.12;
    @Unique
    private static final double LEGACY_ELYTRA_STORAGE_BASE = 0.026;
    @Unique
    private static final double LEGACY_ELYTRA_STORAGE_SPEED_FACTOR = 0.02;
    @Unique
    private static final double LEGACY_ELYTRA_MAX_STORAGE = 3.0;
    @Unique
    private static final double LEGACY_ELYTRA_IDLE_DECAY = 0.85;
    @Unique
    private static final double LEGACY_ELYTRA_RELEASE_DECAY = 0.88;
    @Unique
    private static final double LEGACY_ELYTRA_RELEASE_END = 0.02;
    @Unique
    private static final double LEGACY_ELYTRA_RELEASE_IMPULSE = 0.18;

    @Shadow private boolean crouching;

    @Shadow protected abstract boolean isControlledCamera();

    @Shadow public /*? if >=1.21.2 {*//*ClientInput*//*?} else {*/Input/*?}*/ input;

    @Shadow @Final protected Minecraft minecraft;

    @Shadow @Final public static Logger LOGGER;

    @Shadow private boolean lastOnGround;

    @Unique
    private double legacyStoredElytraBoost;
    @Unique
    private double legacyActiveElytraReleaseBoost;
    @Unique
    private double legacyElytraBoostYBobMovement;
    @Unique
    private boolean legacyElytraBoostBobbing;
    @Unique
    private boolean legacyWasJumpHeld;
    @Unique
    private boolean legacyWasLookingDownForElytraBoost;
    @Unique
    private boolean legacyReleasedElytraBoostByLook;
    @Unique
    private boolean legacyAutoShielding;

    //? if <1.21.5 {
    @Shadow protected abstract boolean hasEnoughFoodToStartSprinting();
    //?} else {
    /*@Shadow protected abstract boolean hasEnoughFoodToSprint();
    *///?}

    @Shadow public abstract void move(MoverType arg, Vec3 arg2);

    @Shadow public abstract boolean isMovingSlowly();

    public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    public boolean canSprintController() {
        return !this.isSprinting() && !this.isFallFlying() && /*? if <1.21.5 {*/this.hasEnoughFoodToStartSprinting()/*?} else {*//*this.hasEnoughFoodToSprint()*//*?}*/ && !this.isUsingItem() && !this.isMovingSlowly() && this.minecraft.screen == null;
    }

    @Override
    public boolean isLegacyElytraBoosting() {
        return legacy$canUseElytraBoostYBobbing() && legacy$isJumpHeld();
    }

    @Override
    public boolean isLegacyElytraBoostBobbing() {
        return legacyElytraBoostBobbing;
    }

    @Override
    public double getLegacyElytraBoostYBobMovement() {
        return legacyElytraBoostYBobMovement;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = /*? if <1.20.5 {*//*2*//*?} else if <1.21.5 {*/3/*?} else {*//*1*//*?}*/))
    public boolean onGroundFlying(boolean original) {
        return !legacy$hasLegacyFlight() && original;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = 0))
    public boolean onGroundCanSprint(boolean original) {
        return true;
    }

    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;crouching:Z", opcode = Opcodes.PUTFIELD, ordinal = 0))
    public void aiStepCrouching(LocalPlayer instance, boolean value) {
        crouching = value && (!legacy$hasLegacyFlight() || ((onGround() || !isInWater()) && !getAbilities().flying && !isFallFlying()));
    }

    @Inject(method = "aiStep", at = @At(value = "FIELD",target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z",opcode = Opcodes.PUTFIELD, ordinal = 1, shift = At.Shift.AFTER))
    public void aiStepStopCrouching(CallbackInfo ci) {
        minecraft.options.keyShift.setDown(false);
    }

    @ModifyExpressionValue(method = /*? if <1.21.5 {*/"aiStep"/*?} else {*//*"shouldStopRunSprinting"*//*?}*/, at = @At(value = "FIELD",target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z"))
    public boolean aiStepSprinting(boolean original) {
        return false;
    }

    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V", ordinal = 0))
    public boolean aiStepSprintController(LocalPlayer instance, boolean b) {
        return !controllerManager.isControllerTheLastInput();
    }

    //? if <1.21.5 {
    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V", ordinal = /*? if <1.21.4 {*/3/*?} else {*//*4*//*?}*/))
    public boolean aiStepSprintingWater(LocalPlayer instance, boolean b) {
        return !Legacy4JClient.hasModOnServer() || !gameRules.getRule(LegacyGameRules.LEGACY_SWIMMING).get() || !this.input.hasForwardImpulse() || !this.hasEnoughFoodToStartSprinting();
    }
    //?} else {
    /*@ModifyExpressionValue(method = {"shouldStopRunSprinting", "canStartSprinting"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUnderWater()Z"))
    public boolean shouldStopRunSprinting(boolean original) {
        return (Legacy4JClient.hasModOnServer() && gameRules.getRule(LegacyGameRules.LEGACY_SWIMMING).get() && isInWater()) || original;
    }
    *///?}

    @Redirect(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    public void aiStepFlyUpDown(LocalPlayer instance, Vec3 vec3) {
        if (legacy$hasLegacyFlight()) move(MoverType.SELF,vec3.with(Direction.Axis.Y,(vec3.y - getDeltaMovement().y) * (legacy$isJumpHeld() ? (/*? if <1.20.5 {*//*0.42f*//*?} else {*/this.getAttributeValue(Attributes.JUMP_STRENGTH)/*?}*/ + getJumpBoostPower()) * 6 : 3)));
        else setDeltaMovement(vec3);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(/*? if <1.21.2 {*/value = "FIELD",target = "Lnet/minecraft/client/player/Input;shiftKeyDown:Z"/*?} else {*//*value = "INVOKE",target = "Lnet/minecraft/world/entity/player/Input;shift()Z"*//*?}*/, ordinal = /*? if <1.21.5 {*/2/*?} else {*//*2*//*?}*/))
    public boolean aiStepShift(boolean original) {
        if (!lastOnGround && !isSpectator()){
            checkSupportingBlock(true,null);
            lastOnGround = mainSupportingBlockPos.isPresent();
        }
        return original && (!legacy$hasLegacyFlight() || (!legacy$isJumpHeld() && !lastOnGround));
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(/*? if <1.21.2 {*/value = "FIELD",target = "Lnet/minecraft/client/player/Input;jumping:Z"/*?} else {*//*value = "INVOKE",target = "Lnet/minecraft/world/entity/player/Input;jump()Z"*//*?}*/, ordinal = 4))
    public boolean aiStepJump(boolean original) {
        return original && (!legacy$hasLegacyFlight() || !isSprinting() || getXRot() <= 0 || !lastOnGround);
    }

    @Override
    public float maxUpStep() {
        return (legacy$hasLegacyFlight() && legacy$isJumpHeld() && isSprinting() && getAbilities().flying ? 0.5f : 0) + super.maxUpStep();
    }

    @Unique
    private boolean legacy$hasLegacyFlight() {
        return LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT);
    }

    @Unique
    private boolean legacy$isJumpHeld() {
        return input./*? if >=1.21.2 {*//*keyPresses.jump()*//*?} else {*/jumping/*?}*/;
    }

    @Unique
    private boolean legacy$canUseStoredElytraBoost() {
        return isFallFlying() && getAbilities().mayfly && getAbilities().invulnerable && this.isControlledCamera() && legacy$hasLegacyFlight();
    }

    @Unique
    private boolean legacy$isLookingDownForElytraBoost() {
        return getXRot() > LEGACY_ELYTRA_STORAGE_PITCH;
    }

    @Unique
    private boolean legacy$canUseElytraBoostYBobbing() {
        return legacy$canUseStoredElytraBoost() && legacy$isLookingDownForElytraBoost();
    }

    @Unique
    private void legacy$chargeStoredElytraBoost(Vec3 deltaMovement) {
        if (!legacy$isLookingDownForElytraBoost()) return;
        double pitchScale = Math.min(1.0, (getXRot() - LEGACY_ELYTRA_STORAGE_PITCH) / (90.0 - LEGACY_ELYTRA_STORAGE_PITCH));
        double storedThisTick = (LEGACY_ELYTRA_STORAGE_BASE + deltaMovement.horizontalDistance() * LEGACY_ELYTRA_STORAGE_SPEED_FACTOR) * pitchScale;
        legacyStoredElytraBoost = Math.min(LEGACY_ELYTRA_MAX_STORAGE, legacyStoredElytraBoost + storedThisTick);
    }

    @Unique
    private void legacy$updateElytraBoostYBobMovement(Vec3 deltaMovement, boolean startedBoosting) {
        if (startedBoosting) legacyElytraBoostYBobMovement = Math.min(0.0, deltaMovement.y);
        legacyElytraBoostYBobMovement = (legacyElytraBoostYBobMovement - LEGACY_ELYTRA_BOOST_BOB_GRAVITY) * LEGACY_ELYTRA_BOOST_BOB_DRAG;
        legacyElytraBoostBobbing = true;
    }

    @Unique
    private void legacy$clearElytraBoostYBobMovement() {
        legacyElytraBoostYBobMovement = 0.0;
        legacyElytraBoostBobbing = false;
    }

    @Unique
    private void legacy$handoffElytraBoostYBobMovement(Vec3 deltaMovement) {
        if (!legacyElytraBoostBobbing) return;
        if (deltaMovement.y <= legacyElytraBoostYBobMovement || Math.abs(deltaMovement.y - legacyElytraBoostYBobMovement) <= LEGACY_ELYTRA_BOOST_BOB_HANDOFF_END) {
            legacy$clearElytraBoostYBobMovement();
            return;
        }
        legacy$updateElytraBoostYBobMovement(deltaMovement, false);
    }

    @Unique
    private void legacy$exitElytraBoostYBobMovement(Vec3 deltaMovement) {
        if (!legacyElytraBoostBobbing) return;
        legacyElytraBoostYBobMovement += (deltaMovement.y - legacyElytraBoostYBobMovement) * LEGACY_ELYTRA_BOOST_BOB_EXIT_HANDOFF;
        if (Math.abs(deltaMovement.y - legacyElytraBoostYBobMovement) <= LEGACY_ELYTRA_BOOST_BOB_HANDOFF_END) legacy$clearElytraBoostYBobMovement();
    }

    @Unique
    private void legacy$releaseStoredElytraBoost() {
        if (legacyStoredElytraBoost <= 0.0) return;
        legacy$cancelActiveElytraReleaseBoost();
        legacyActiveElytraReleaseBoost = legacyStoredElytraBoost;
        legacyStoredElytraBoost = 0.0;
    }

    @Unique
    private void legacy$decayStoredElytraBoost(boolean hardReset) {
        if (hardReset) {
            legacyStoredElytraBoost = 0.0;
            legacy$clearElytraBoostYBobMovement();
            legacy$cancelActiveElytraReleaseBoost();
            legacyWasJumpHeld = false;
            legacyWasLookingDownForElytraBoost = false;
            legacyReleasedElytraBoostByLook = false;
            return;
        }
        legacyStoredElytraBoost *= LEGACY_ELYTRA_IDLE_DECAY;
        if (legacyStoredElytraBoost < 0.01) legacyStoredElytraBoost = 0.0;
    }

    @Unique
    private Vec3 legacy$clearActiveElytraReleaseBoost(Vec3 deltaMovement) {
        legacy$cancelActiveElytraReleaseBoost();
        return deltaMovement;
    }

    @Unique
    private Vec3 legacy$clearElytraClimbVelocity(Vec3 deltaMovement) {
        return deltaMovement.y > 0.0 ? deltaMovement.with(Direction.Axis.Y, 0.0) : deltaMovement;
    }

    @Unique
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

    @Unique
    private void legacy$cancelActiveElytraReleaseBoost() {
        legacyActiveElytraReleaseBoost = 0.0;
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"))
    public void setYElytraFlightElevation(CallbackInfo ci) {
        Vec3 deltaMovement = getDeltaMovement();
        if (!legacy$hasLegacyFlight()) {
            setDeltaMovement(legacy$clearActiveElytraReleaseBoost(deltaMovement));
            legacy$decayStoredElytraBoost(true);
            return;
        }
        if (!legacy$canUseStoredElytraBoost()) {
            setDeltaMovement(legacy$clearActiveElytraReleaseBoost(deltaMovement));
            legacyWasLookingDownForElytraBoost = false;
            legacyReleasedElytraBoostByLook = false;
            legacy$decayStoredElytraBoost(!isFallFlying());
            return;
        }
        boolean jumpHeld = legacy$isJumpHeld();
        boolean lookingDownForBoost = legacy$isLookingDownForElytraBoost();
        boolean canBobBoost = lookingDownForBoost;
        boolean startedBoosting = !legacyWasJumpHeld && jumpHeld;
        boolean releasedJumpThisTick = legacyWasJumpHeld && !jumpHeld;
        boolean releasedLookThisTick = legacyWasJumpHeld && jumpHeld && legacyWasLookingDownForElytraBoost && !lookingDownForBoost;
        if (!jumpHeld || lookingDownForBoost) legacyReleasedElytraBoostByLook = false;
        if (releasedLookThisTick) legacyReleasedElytraBoostByLook = true;
        legacyWasJumpHeld = jumpHeld;
        legacyWasLookingDownForElytraBoost = jumpHeld && lookingDownForBoost;
        if (releasedLookThisTick) {
            deltaMovement = legacy$clearElytraClimbVelocity(deltaMovement);
            legacy$exitElytraBoostYBobMovement(deltaMovement);
        } else if (jumpHeld && !legacyReleasedElytraBoostByLook) {
            deltaMovement = legacy$clearActiveElytraReleaseBoost(deltaMovement);
            if (canBobBoost) {
                legacy$updateElytraBoostYBobMovement(deltaMovement, startedBoosting);
                deltaMovement = deltaMovement.with(Direction.Axis.Y, this.getAbilities().getFlyingSpeed() * 12);
                legacy$chargeStoredElytraBoost(deltaMovement);
            } else {
                legacy$exitElytraBoostYBobMovement(deltaMovement);
                move(MoverType.SELF, new Vec3(0, this.getAbilities().getFlyingSpeed() * 12, 0));
            }
        } else if (jumpHeld) {
            deltaMovement = legacy$clearElytraClimbVelocity(deltaMovement);
            legacy$exitElytraBoostYBobMovement(deltaMovement);
        } else if (!releasedJumpThisTick) {
            if (canBobBoost) legacy$handoffElytraBoostYBobMovement(deltaMovement);
            else legacy$exitElytraBoostYBobMovement(deltaMovement);
            legacy$decayStoredElytraBoost(false);
        }
        if (releasedJumpThisTick || releasedLookThisTick) {
            deltaMovement = legacy$clearElytraClimbVelocity(deltaMovement);
            legacy$releaseStoredElytraBoost();
        }
        setDeltaMovement(legacy$updateActiveElytraReleaseBoost(deltaMovement));
    }

    @Inject(method = "aiStep", at = @At(value = "RETURN"))
    public void setYFlightElevation(CallbackInfo ci) {
        if (!legacy$hasLegacyFlight()) return;
        if (this.getAbilities().flying && this.isControlledCamera()) {
            if (keyFlyDown.isDown() && !keyFlyUp.isDown() || !keyFlyDown.isDown() && keyFlyUp.isDown() || keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown())
                setDeltaMovement(getDeltaMovement().add(0,(keyFlyUp.isDown() ? 1.5 : keyFlyDown.isDown() ? -1.5 : 0) * this.getAbilities().getFlyingSpeed(),0));
            if (getXRot() != 0 && (!lastOnGround || getXRot() < 0) && input.hasForwardImpulse() && isSprinting()) move(MoverType.SELF,new Vec3(0,-(getXRot() / 90) * input./*? if <1.21.5 {*/forwardImpulse/*?} else {*//*getMoveVector().y*//*?}*/ * getFlyingSpeed() * 2, 0));
        }
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void aiStepShieldControls(CallbackInfo ci) {
        legacy$updateShieldControls();
    }

    @ModifyExpressionValue(method = /*? if <1.20.5 {*//*"handleNetherPortalClient"*//*?} else if <1.21.5 {*/"handleConfusionTransitionEffect"/*?} else {*//*"handlePortalTransitionEffect"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;isPauseScreen()Z"))
    public boolean handleConfusionTransitionEffect(boolean original) {
        return original || Legacy4JClient.hasModOnServer();
    }

    @Inject(method = /*? if <1.21.5 {*/"serverAiStep"/*?} else {*//*"applyInput"*//*?}*/, at = @At("RETURN"))
    public void applyInput(CallbackInfo ci) {
        if (this.isControlledCamera() && this.getAbilities().flying && legacy$hasLegacyFlight()) {
            if (keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown()) xxa+= (keyFlyLeft.isDown() ? 12 : -12) * this.getAbilities().getFlyingSpeed();
            if (getXRot() != 0 && input.hasForwardImpulse() && isSprinting()) zza*=Math.max(0.1f,1 - Math.abs(getXRot() / 90));
        }
        if (wantsToStopRiding() && this.isPassenger()) {
            minecraft.options.keyShift.setDown(false);
        }
    }

    @Inject(method = "rideTick", at = @At("RETURN"))
    private void rideTickShieldControls(CallbackInfo ci) {
        legacy$updateShieldControls();
    }

    @WrapWithCondition(method = "onSyncedDataUpdated", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;startUsingItem(Lnet/minecraft/world/InteractionHand;)V"))
    private boolean onSyncedDataUpdatedStartUsingItem(LocalPlayer instance, InteractionHand hand) {
        return !((LegacyShieldPlayer)this).isShieldPaused() || !LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SHIELD_CONTROLS) || !(getItemInHand(hand).getItem() instanceof ShieldItem);
    }

    @Unique
    private void legacy$updateShieldControls() {
        InteractionHand hand = legacy$getShieldHand();
        if (LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_SHIELD_CONTROLS) && hand != null && (isPassenger() || input./*? if >=1.21.2 {*//*keyPresses.shift()*//*?} else {*/shiftKeyDown/*?}*/)) {
            if (LegacyShieldPlayer.hasConflictingUse((LocalPlayer)(Object)this, hand)) {
                legacyAutoShielding = false;
                return;
            }
            if (((LegacyShieldPlayer)this).isShieldPaused()) {
                if (legacyAutoShielding && isUsingItem() && getUseItem().getItem() instanceof ShieldItem) stopUsingItem();
                legacyAutoShielding = false;
                return;
            }
            if (!isUsingItem() || !getUseItem().is(getItemInHand(hand).getItem()) || getUsedItemHand() != hand) {
                if (isUsingItem()) stopUsingItem();
                startUsingItem(hand);
            }
            legacyAutoShielding = true;
        } else {
            if (legacyAutoShielding && isUsingItem() && getUseItem().getItem() instanceof ShieldItem) stopUsingItem();
            legacyAutoShielding = false;
        }
    }

    @Unique
    private InteractionHand legacy$getShieldHand() {
        if (getOffhandItem().getItem() instanceof ShieldItem) return InteractionHand.OFF_HAND;
        return getMainHandItem().getItem() instanceof ShieldItem ? InteractionHand.MAIN_HAND : null;
    }
}
