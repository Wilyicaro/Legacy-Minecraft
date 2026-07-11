package wily.legacy.skins.pose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class IdleSitPose {
    public static final float BODY_DOWN = 9.25F;
    private static final Map<PlayerModel, BasePose> BASE = new WeakHashMap<>();

    private IdleSitPose() {
    }

    private static BasePose base(PlayerModel model) {
        BasePose b = BASE.get(model);
        if (b != null) return b;
        b = new BasePose(model);
        BASE.put(model, b);
        return b;
    }

    public static boolean shouldApply(Object state) {
        if (state == null) return false;
        String id = ArmPoseSupport.getSkinId(state);
        if (id == null || id.isBlank()) return false;

        UUID uuid = ArmPoseSupport.getEntityUuid(state);

        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.IDLE_SIT, id)) {
            if (uuid != null) IdleSitTracker.reset(uuid);
            return false;
        }

        if (uuid == null) return false;

        boolean moving = ArmPoseSupport.isMoving(state);
        Player player = ArmPoseSupport.getPlayer(state);
        if (player == null || ArmPoseSupport.isFallFlying(state)) {
            IdleSitTracker.reset(uuid);
            return false;
        }
        moving = moving || !player.onGround() && !player.getAbilities().flying;

        Pose pose = ArmPoseSupport.getPose(state);
        if (pose != Pose.STANDING && pose != Pose.CROUCHING) {
            IdleSitTracker.updateAndShouldSit(uuid, true, ArmPoseSupport.getYRot(state), ArmPoseSupport.getXRot(state));
            return false;
        }

        return IdleSitTracker.updateAndShouldSit(uuid, moving, ArmPoseSupport.getYRot(state), ArmPoseSupport.getXRot(state));
    }

    public static void apply(PlayerModel model) {
        if (model == null) return;

        BasePose b = base(model);

        float rideLegX = -1.4137167F;
        float rideLegY = 0.31415927F;
        float rideLegZ = 0.07853982F;
        float bodyDown = BODY_DOWN;
        float legsYOff = bodyDown - 0.3F;
        float legsZOff = -2.8F;

        model.rightLeg.xRot = rideLegX;
        model.leftLeg.xRot = rideLegX;
        model.rightLeg.yRot = rideLegY;
        model.leftLeg.yRot = -rideLegY;
        model.rightLeg.zRot = rideLegZ;
        model.leftLeg.zRot = -rideLegZ;

        model.rightLeg.y = b.rLegY + legsYOff;
        model.leftLeg.y = b.lLegY + legsYOff;
        model.rightLeg.z = b.rLegZ + legsZOff;
        model.leftLeg.z = b.lLegZ + legsZOff;

        model.body.y = b.bodyY + bodyDown;
        model.jacket.y = b.jacketY + bodyDown;
        model.head.y = b.headY + bodyDown;
        model.hat.y = b.hatY;

        model.rightArm.y = b.rArmY + bodyDown;
        model.leftArm.y = b.lArmY + bodyDown;
        model.rightSleeve.y = b.rSleeveY + bodyDown;
        model.leftSleeve.y = b.lSleeveY + bodyDown;

        model.rightPants.xRot = model.rightLeg.xRot;
        model.rightPants.yRot = model.rightLeg.yRot;
        model.rightPants.zRot = model.rightLeg.zRot;
        model.rightPants.y = model.rightLeg.y;
        model.rightPants.z = model.rightLeg.z;
        model.leftPants.xRot = model.leftLeg.xRot;
        model.leftPants.yRot = model.leftLeg.yRot;
        model.leftPants.zRot = model.leftLeg.zRot;
        model.leftPants.y = model.leftLeg.y;
        model.leftPants.z = model.leftLeg.z;
    }

    private static final class BasePose {
        final float bodyY, jacketY, headY, hatY;
        final float rArmY, lArmY, rSleeveY, lSleeveY;
        final float rLegY, lLegY, rLegZ, lLegZ;

        BasePose(PlayerModel model) {
            this.bodyY = model.body.y;
            this.jacketY = model.jacket.y;
            this.headY = model.head.y;
            this.hatY = model.hat.y;
            this.rArmY = model.rightArm.y;
            this.lArmY = model.leftArm.y;
            this.rSleeveY = model.rightSleeve.y;
            this.lSleeveY = model.leftSleeve.y;
            this.rLegY = model.rightLeg.y;
            this.lLegY = model.leftLeg.y;
            this.rLegZ = model.rightLeg.z;
            this.lLegZ = model.leftLeg.z;
        }
    }
}
