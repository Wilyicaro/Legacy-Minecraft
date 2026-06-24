package wily.legacy.skins.pose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Pose;

public final class SyncLegsPose {
    private SyncLegsPose() {
    }

    public static boolean shouldApply(Object state) {
        if (!ArmPoseSupport.hasPose(state, SkinPoseRegistry.PoseTag.SYNC_LEGS)) return false;
        if (ArmPoseSupport.isMenuDoll(state)) return true;
        Pose pose = ArmPoseSupport.getPose(state);
        if (pose != Pose.STANDING && pose != Pose.CROUCHING) return false;
        return ArmPoseSupport.isMoving(state) || ArmPoseSupport.getMoveSpeedSq(state) > 1.0E-4F;
    }

    public static void apply(PlayerModel model) {
        if (model == null) return;
        model.leftLeg.xRot = model.rightLeg.xRot;
        model.leftLeg.yRot = model.rightLeg.yRot;
        model.leftLeg.zRot = model.rightLeg.zRot;
        model.leftPants.xRot = model.leftLeg.xRot;
        model.leftPants.yRot = model.leftLeg.yRot;
        model.leftPants.zRot = model.leftLeg.zRot;
    }
}
