package wily.legacy.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
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

import static wily.legacy.Legacy4JClient.*;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    @Shadow private boolean crouching;

    @Shadow protected abstract boolean isControlledCamera();

    @Shadow public Input input;

    @Shadow @Final protected Minecraft minecraft;

    @Shadow @Final public static Logger LOGGER;

    @Shadow private boolean lastOnGround;

    public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = 2))
    public boolean onGroundFlying(LocalPlayer instance) {
        return false;
    }
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z", ordinal = 0))
    public boolean onGroundCanSprint(LocalPlayer instance) {
        return true;
    }
    @Redirect(method = "aiStep", at = @At(value = "FIELD",target = "Lnet/minecraft/client/player/LocalPlayer;crouching:Z", opcode = Opcodes.PUTFIELD, ordinal = 0))
    public void aiStepCrouching(LocalPlayer instance, boolean value) {
        crouching = value && !isFallFlying();
    }
    @Redirect(method = "aiStep", at = @At(value = "FIELD",target = "Lnet/minecraft/client/player/LocalPlayer;horizontalCollision:Z"))
    public boolean aiStepSprinting(LocalPlayer instance) {
        return false;
    }
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/player/LocalPlayer;isInWater()Z", ordinal = 1))
    public boolean aiStepSprintingWater(LocalPlayer instance) {
        return instance.isInWater() && isAffectedByFluids();
    }
    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"))
    public void setYElytraFlightElevation(CallbackInfo ci) {
        if (isFallFlying() && getAbilities().mayfly && getAbilities().invulnerable && this.isControlledCamera() && this.input.jumping)
            setDeltaMovement(new Vec3(getDeltaMovement().x,this.getAbilities().getFlyingSpeed() * 12,getDeltaMovement().z));
    }
    @Inject(method = "aiStep", at = @At(value = "RETURN"))
    public void setYFlightElevation(CallbackInfo ci) {
        if (this.getAbilities().flying && this.isControlledCamera()) {
            if (keyFlyDown.isDown() && !keyFlyUp.isDown() || !keyFlyDown.isDown() && keyFlyUp.isDown())
                setDeltaMovement(getDeltaMovement().add(0,(keyFlyUp.isDown() ? 1.5 : keyFlyDown.isDown() ? -1.5 : 0) * this.getAbilities().getFlyingSpeed(), 0));
            if (!lastOnGround){
                checkSupportingBlock(true,null);
                lastOnGround = mainSupportingBlockPos.isPresent();
            }
            if (getXRot() != 0 && (!lastOnGround || getXRot() < 0 || getXRot() > 45) && input.hasForwardImpulse() && isSprinting()) {
                setDeltaMovement(getDeltaMovement().add(0, -Math.sin(Math.toRadians(getXRot())) * input.forwardImpulse * getFlyingSpeed(), 0));
            }
        }
    }
    @Redirect(method = "handleNetherPortalClient", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;isPauseScreen()Z"))
    public boolean handleNetherPortalClient(Screen instance) {
        return true;
    }
    @Inject(method = "serverAiStep", at = @At("RETURN"))
    public void serverAiStep(CallbackInfo ci) {
        if (this.isControlledCamera() && this.getAbilities().flying) {
            if (keyFlyLeft.isDown() && !keyFlyRight.isDown() || !keyFlyLeft.isDown() && keyFlyRight.isDown()) xxa+= (keyFlyLeft.isDown() ? 12 : -12) * this.getAbilities().getFlyingSpeed();
        }
    }
}
