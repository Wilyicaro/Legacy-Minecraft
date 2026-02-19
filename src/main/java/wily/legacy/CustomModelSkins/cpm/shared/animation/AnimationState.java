package wily.legacy.CustomModelSkins.cpm.shared.animation;

import wily.legacy.CustomModelSkins.cpl.util.Hand;
import wily.legacy.CustomModelSkins.cpl.util.HandAnimation;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.PlayerModelSetup.ArmPose;

public class AnimationState {
    public boolean sneaking, crawling, swimming, retroSwimming, sprinting, riding, dying, tridentSpin, wearingHelm, wearingElytra, wearingBody, wearingLegs, wearingBoots, parrotLeft, parrotRight, isOnLadder, isBurning, isFreezing;
    public float moveAmountX, moveAmountY, moveAmountZ, yaw, pitch, bodyYaw, hurtTime, attackTime, swimAmount, crossbowPullback, bowPullback;
    public int encodedState;
    public HandAnimation usingAnimation;
    public Hand mainHand, activeHand, swingingHand;
    public ArmPose leftArm, rightArm;
    public Object vrState;
    public boolean hasSkullOnHead, sleeping, invisible, inGui, firstPersonMod;
    public AnimationEngine.AnimationMode animationMode;

    public void resetPlayer() {
        hasSkullOnHead = sleeping = invisible = inGui = firstPersonMod = sneaking = crawling = swimming = retroSwimming = sprinting = riding = dying = tridentSpin = wearingHelm = wearingElytra = wearingBody = wearingLegs = wearingBoots = parrotLeft = parrotRight = isOnLadder = isBurning = isFreezing = false;
        moveAmountX = moveAmountY = moveAmountZ = yaw = pitch = bodyYaw = hurtTime = attackTime = swimAmount = crossbowPullback = bowPullback = 0;
        encodedState = 0;
        usingAnimation = null;
        mainHand = activeHand = swingingHand = null;
        leftArm = rightArm = null;
        vrState = null;
    }

    public void resetModel() {
    }
}
