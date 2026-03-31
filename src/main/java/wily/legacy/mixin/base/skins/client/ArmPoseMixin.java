package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import wily.legacy.Skins.pose.ZombieArmsPose;
import wily.legacy.Skins.skin.SkinIdUtil;

@Mixin(PlayerModel.class)
public abstract class ArmPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"), require = 0)
    private void consoleskins$syncArmsPose(AvatarRenderState state, CallbackInfo ci) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return;
        String skinId = a.consoleskins$getSkinId();
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        PlayerModel self = (PlayerModel) (Object) this;
        if (ZombieArmsPose.shouldApply(state)) {
            ZombieArmsPose.apply(self, state);
            return;
        }
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.SYNC_ARMS, skinId)) return;
        if (state.pose != Pose.STANDING && state.pose != Pose.CROUCHING) return;
        if (state.attackTime > 0.0F) return;
        if (a.consoleskins$isUsingItem() || a.consoleskins$isBlocking()) return;
        float speedSq = a.consoleskins$getMoveSpeedSq();
        if (!a.consoleskins$isMoving() && speedSq <= 1.0E-4F) return;
        float factor = a.consoleskins$isMoving() ? 1.0F : Mth.clamp(speedSq * 120.0F, 0.0F, 1.0F);
        if (factor <= 0.01F) return;
        float diff = self.leftArm.xRot - self.rightArm.xRot;
        self.rightArm.xRot += diff * factor;
        self.rightSleeve.xRot = self.rightArm.xRot;
    }
}
