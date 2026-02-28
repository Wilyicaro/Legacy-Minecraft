package wily.legacy.Skins.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class ZombieArmsPose {
    private ZombieArmsPose() {
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        String id = a.consoleskins$getSkinId();
        return ZombieArmsConfig.isZombieArmsSkin(id);
    }

    public static void apply(PlayerModel model, AvatarRenderState state) {
        float baseX = -1.55F;
        if (state != null && state.isCrouching) baseX = -1.2F;

        model.rightArm.xRot = baseX;
        model.leftArm.xRot = baseX;
        model.rightArm.yRot = -0.15F;
        model.leftArm.yRot = 0.15F;
        model.rightArm.zRot = 0.0F;
        model.leftArm.zRot = 0.0F;

        float attackTime = state == null ? 0.0F : state.attackTime;
        if (attackTime > 0.0F) {
            HumanoidArm arm = state.attackArm;
            if (arm == null) arm = HumanoidArm.RIGHT;

            float bodyY = Mth.sin(Mth.sqrt(attackTime) * Mth.PI * 2.0F) * 0.2F;
            if (arm == HumanoidArm.LEFT) bodyY *= -1.0F;

            model.body.yRot = bodyY;
            model.jacket.yRot = bodyY;

            model.rightArm.z = Mth.sin(bodyY) * 5.0F;
            model.rightArm.x = -Mth.cos(bodyY) * 5.0F;
            model.leftArm.z = -Mth.sin(bodyY) * 5.0F;
            model.leftArm.x = Mth.cos(bodyY) * 5.0F;

            model.rightArm.yRot += bodyY;
            model.leftArm.yRot += bodyY;
            model.leftArm.xRot += bodyY;

            float g = 1.0F - attackTime;
            g *= g;
            g *= g;
            g = 1.0F - g;

            float swing = Mth.sin(g * Mth.PI);
            float lift = Mth.sin(attackTime * Mth.PI) * -(model.head.xRot - 0.7F) * 0.75F;

            if (arm == HumanoidArm.LEFT) {
                model.leftArm.xRot -= swing * 1.2F + lift;
                model.leftArm.yRot += bodyY * 2.0F;
                model.leftArm.zRot += Mth.sin(attackTime * Mth.PI) * -0.4F;
            } else {
                model.rightArm.xRot -= swing * 1.2F + lift;
                model.rightArm.yRot += bodyY * 2.0F;
                model.rightArm.zRot += Mth.sin(attackTime * Mth.PI) * -0.4F;
            }
        }

        model.rightSleeve.x = model.rightArm.x;
        model.rightSleeve.y = model.rightArm.y;
        model.rightSleeve.z = model.rightArm.z;
        model.leftSleeve.x = model.leftArm.x;
        model.leftSleeve.y = model.leftArm.y;
        model.leftSleeve.z = model.leftArm.z;

        model.rightSleeve.xRot = model.rightArm.xRot;
        model.rightSleeve.yRot = model.rightArm.yRot;
        model.rightSleeve.zRot = model.rightArm.zRot;
        model.leftSleeve.xRot = model.leftArm.xRot;
        model.leftSleeve.yRot = model.leftArm.yRot;
        model.leftSleeve.zRot = model.leftArm.zRot;
    }
}
