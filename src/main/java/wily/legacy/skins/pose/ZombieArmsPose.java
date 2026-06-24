package wily.legacy.skins.pose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public final class ZombieArmsPose {
    private ZombieArmsPose() {
    }

    public static boolean shouldApply(Object state) {
        if (!ArmPoseSupport.hasPose(state, SkinPoseRegistry.PoseTag.ZOMBIE_ARMS)) return false;
        if (ArmPoseSupport.getPose(state) == Pose.SWIMMING) return false;
        Player player = ArmPoseSupport.getPlayer(state);
        return player == null || player.getPose() != Pose.SWIMMING;
    }

    public static void apply(PlayerModel model, Object state) {
        Player player = ArmPoseSupport.getPlayer(state);
        if (player != null && player.getPose() == Pose.SWIMMING) return;

        ArmPoseSupport.ArmState rightState = ArmPoseSupport.ArmState.capture(model.rightArm, model.rightSleeve);
        ArmPoseSupport.ArmState leftState = ArmPoseSupport.ArmState.capture(model.leftArm, model.leftSleeve);
        ArmPoseSupport.ArmFlags blocking = ArmPoseSupport.getShieldBlockingArms(player, false);

        float baseX = ArmPoseSupport.isCrouching(state) ? -1.2F : -1.55F;
        if (!blocking.right()) {
            model.rightArm.xRot = baseX;
            model.rightArm.yRot = -0.15F;
            model.rightArm.zRot = 0.0F;
        }
        if (!blocking.left()) {
            model.leftArm.xRot = baseX;
            model.leftArm.yRot = 0.15F;
            model.leftArm.zRot = 0.0F;
        }

        float attackTime = ArmPoseSupport.getAttackTime(state);
        ArmPoseSupport.ArmFlags holding = ArmPoseSupport.getHoldingArms(player);
        float holdScale = 1.0F - attackTime;
        if (holdScale > 0.0F && (holding.right() || holding.left())) {
            float targetX = Math.max(baseX, -1.3F);
            float adjust = (targetX - baseX) * holdScale;
            if (holding.right() && !blocking.right()) model.rightArm.xRot += adjust;
            if (holding.left() && !blocking.left()) model.leftArm.xRot += adjust;
        }

        ArmPoseSupport.applyIdleSway(model, ArmPoseSupport.getIdleSwayTime(state), attackTime, 0.95F, 0.02F, 0.85F, 0.015F, 0.75F, 0.012F, blocking.right(), blocking.left());
        ArmPoseSupport.applyAttackSwing(model, state, attackTime);

        if (blocking.right()) rightState.restore(model.rightArm, model.rightSleeve);
        else rightState.syncSleeve(model.rightArm, model.rightSleeve);

        if (blocking.left()) leftState.restore(model.leftArm, model.leftSleeve);
        else leftState.syncSleeve(model.leftArm, model.leftSleeve);
    }
}
