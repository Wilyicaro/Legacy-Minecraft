package wily.legacy.skins.pose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public final class StiffArmsPose {
    private StiffArmsPose() {
    }

    private static float getBaseXRot(Object state) {
        return ArmPoseSupport.isCrouching(state) ? 0.4F : 0.0F;
    }

    public static boolean shouldApply(Object state) {
        if (!ArmPoseSupport.hasPose(state, SkinPoseRegistry.PoseTag.STIFF_ARMS)) return false;
        Pose pose = ArmPoseSupport.getPose(state);
        if (pose == Pose.SWIMMING) return false;
        Player player = ArmPoseSupport.getPlayer(state);
        if (player != null && player.getPose() == Pose.SWIMMING) return false;
        return pose == Pose.STANDING || pose == Pose.CROUCHING || pose == Pose.FALL_FLYING;
    }

    public static void apply(PlayerModel model, Object state) {
        if (ArmPoseSupport.getPose(state) == Pose.SWIMMING) return;
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
        float attackTime = ArmPoseSupport.getAttackTime(state);
        ArmPoseSupport.applyIdleSway(model, ArmPoseSupport.getIdleSwayTime(state), attackTime, 0.45F, 0.06F, 0.37F, 0.04F, 0.28F, 0.02F, blocking.right(), blocking.left());
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
