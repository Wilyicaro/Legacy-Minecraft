package wily.legacy.skins.pose;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;

public final class StatueOfLibertyPose {
    private StatueOfLibertyPose() {
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        String id = a.consoleskins$getSkinId();
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.STATUE_OF_LIBERTY, id)) return false;
        if (state != null && state.attackTime > 0.0F) return false;
        Player player = ArmPoseSupport.getPlayer(state);
        return player == null || !ArmPoseSupport.getHoldingArms(player).right();
    }

    public static void apply(PlayerModel model, AvatarRenderState state) {
        model.rightArm.xRot = -3.1415927F;
        model.rightArm.yRot = 0.0F;
        model.rightArm.zRot = 0.0F;

        float attackTime = state == null ? 0.0F : state.attackTime;
        float idleScale = 1.0F - attackTime;
        if (idleScale < 0.0F) idleScale = 0.0F;
        if (idleScale > 0.0F) {
            float t = ArmPoseSupport.getAgeInTicks(state);
            float cos = Mth.cos(t * 0.95F) * 0.02F * idleScale;
            float sin = Mth.sin(t * 0.85F) * 0.015F * idleScale;
            float yaw = Mth.sin(t * 0.75F) * 0.012F * idleScale;
            model.rightArm.zRot += cos;
            model.rightArm.xRot += sin;
            model.rightArm.yRot += yaw;
        }

        model.rightSleeve.xRot = model.rightArm.xRot;
        model.rightSleeve.yRot = model.rightArm.yRot;
        model.rightSleeve.zRot = model.rightArm.zRot;
    }

    public static void apply(PlayerModel model) {
        apply(model, null);
    }
}
