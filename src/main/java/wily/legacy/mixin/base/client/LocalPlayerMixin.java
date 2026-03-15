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
import wily.legacy.Legacy4JClient;
import wily.legacy.entity.LegacyLocalPlayer;
import wily.legacy.init.LegacyGameRules;

import static wily.legacy.Legacy4JClient.*;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer implements LegacyLocalPlayer {

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

    public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Shadow
    protected abstract boolean isControlledCamera();

    @Shadow
    protected abstract boolean hasEnoughFoodToSprint();

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
        crouching = value && (!gameRules.getBoolean(LegacyGameRules.LEGACY_FLIGHT) || ((onGround() || !isInWater()) && !getAbilities().flying && !isFallFlying()));
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

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"))
    public void setYElytraFlightElevation(CallbackInfo ci) {
        if (!LegacyGameRules.getSidedBooleanGamerule(this, LegacyGameRules.LEGACY_FLIGHT)) return;
        if (isFallFlying() && getAbilities().mayfly && getAbilities().invulnerable && this.isControlledCamera() && jumping && gameRules.getRule(LegacyGameRules.LEGACY_FLIGHT).get())
            setDeltaMovement(getDeltaMovement().with(Direction.Axis.Y, input./*? if >=1.21.2 {*/keyPresses.jump()/*?} else {*//*jumping*//*?}*/ ? this.getAbilities().getFlyingSpeed() * 12 : 0));

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
