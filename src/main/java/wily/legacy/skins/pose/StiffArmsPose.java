package wily.legacy.skins.pose;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;

public final class StiffArmsPose {
    private StiffArmsPose() {
    }

    private static float getBaseXRot(AvatarRenderState state) {
        return state != null && state.isCrouching ? 0.4F : 0.0F;
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess access)) return false;
        String skinId = access.consoleskins$getSkinId();
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.STIFF_ARMS, skinId)) return false;
        if (state.pose == Pose.SWIMMING) return false;
        Player player = ArmPoseSupport.getPlayer(state);
        if (player != null && player.getPose() == Pose.SWIMMING) return false;
        return state.pose == Pose.STANDING || state.pose == Pose.CROUCHING || state.pose == Pose.FALL_FLYING;
    }

    public static void apply(PlayerModel model, AvatarRenderState state) {
        if (state != null && state.pose == Pose.SWIMMING) return;
        Player player = ArmPoseSupport.getPlayer(state);
        if (player != null && player.getPose() == Pose.SWIMMING) return;
        ArmPoseSupport.ArmState rightState = ArmPoseSupport.ArmState.capture(model.rightArm, model.rightSleeve);
        ArmPoseSupport.ArmState leftState = ArmPoseSupport.ArmState.capture(model.leftArm, model.leftSleeve);
        ArmPoseSupport.ArmFlags blocking = ArmPoseSupport.includeModelBlocking(state, ArmPoseSupport.getShieldBlockingArms(player, true));
        float baseXRot = getBaseXRot(state);
        if (!blocking.right()) {
            model.rightArm.xRot = baseXRot;
            model.rightArm.yRot = 0.0F;
            model.rightArm.zRot = 0.0F;
        }
        if (!blocking.left()) {
            model.leftArm.xRot = baseXRot;
            model.leftArm.yRot = 0.0F;
            model.leftArm.zRot = 0.0F;
        }
        float attackTime = state == null ? 0.0F : state.attackTime;
        ArmPoseSupport.applyIdleSway(
                model,
                ArmPoseSupport.getAgeInTicks(state),
                attackTime,
                0.45F,
                0.06F,
                0.37F,
                0.04F,
                0.28F,
                0.02F,
                blocking.right(),
                blocking.left()
        );
        ArmPoseSupport.applyAttackSwing(model, state, attackTime);
        if (blocking.right()) rightState.restore(model.rightArm, model.rightSleeve);
        else rightState.syncSleeve(model.rightArm, model.rightSleeve);
        if (blocking.left()) leftState.restore(model.leftArm, model.leftSleeve);
        else leftState.syncSleeve(model.leftArm, model.leftSleeve);
    }

    public static float getAgeInTicks(Object state) {
        return ArmPoseSupport.getAgeInTicks(state);
    }
}
