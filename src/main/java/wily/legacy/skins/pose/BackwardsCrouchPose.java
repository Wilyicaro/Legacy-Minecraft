package wily.legacy.skins.pose;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;

public final class BackwardsCrouchPose {
    private BackwardsCrouchPose() {
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess access)) return false;
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.BACKWARDS_CROUCH, access.consoleskins$getSkinId())) return false;
        return state.isCrouching || state.pose == Pose.CROUCHING || state.hasPose(Pose.CROUCHING);
    }

    public static void apply(PlayerModel model) {
        if (model == null) return;
        model.body.xRot = -0.5F;
        model.body.y = 0.0F;
        model.body.z = 2.0F;
        model.head.y = 1.0F;
        model.head.z = 0.0F;
        model.rightArm.y = 2.0F;
        model.rightArm.z = 2.0F;
        model.leftArm.y = 2.0F;
        model.leftArm.z = 2.0F;
        model.rightLeg.y = 9.0F;
        model.rightLeg.z = -4.0F;
        model.leftLeg.y = 9.0F;
        model.leftLeg.z = -4.0F;
        copyPose(model.hat, model.head);
        copyPose(model.jacket, model.body);
        copyPose(model.rightSleeve, model.rightArm);
        copyPose(model.leftSleeve, model.leftArm);
        copyPose(model.rightPants, model.rightLeg);
        copyPose(model.leftPants, model.leftLeg);
    }

    private static void copyPose(ModelPart target, ModelPart source) {
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
    }
}
