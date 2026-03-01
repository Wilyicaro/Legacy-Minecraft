package wily.legacy.Skins.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class ZombieArmsPose {
    private ZombieArmsPose() {
    }

    private static Player getPlayer(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return null;
        try {
            UUID uuid = a.consoleskins$getEntityUuid();
            if (uuid == null) return null;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return null;
            return mc.level.getPlayerByUUID(uuid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        String id = a.consoleskins$getSkinId();
        if (!ZombieArmsConfig.isZombieArmsSkin(id)) return false;
        Player p = getPlayer(state);
        return p == null || p.getPose() != Pose.SWIMMING;
    }

    public static void apply(PlayerModel model, AvatarRenderState state) {
        Player player = getPlayer(state);
        if (player != null && player.getPose() == Pose.SWIMMING) return;

        float baseX = -1.55F;
        if (state != null && state.isCrouching) baseX = -1.2F;

        model.rightArm.xRot = baseX;
        model.leftArm.xRot = baseX;
        model.rightArm.yRot = -0.15F;
        model.leftArm.yRot = 0.15F;
        model.rightArm.zRot = 0.0F;
        model.leftArm.zRot = 0.0F;

        float attackTime = state == null ? 0.0F : state.attackTime;

        boolean rightHolding = false;
        boolean leftHolding = false;
        if (player != null) {
            HumanoidArm main = player.getMainArm();
            boolean mainItem = !player.getMainHandItem().isEmpty();
            boolean offItem = !player.getOffhandItem().isEmpty();
            if (mainItem) {
                if (main == HumanoidArm.RIGHT) rightHolding = true;
                else leftHolding = true;
            }
            if (offItem) {
                if (main == HumanoidArm.RIGHT) leftHolding = true;
                else rightHolding = true;
            }
        }

        float holdScale = 1.0F - attackTime;
        if (holdScale < 0.0F) holdScale = 0.0F;
        if (holdScale > 0.0F && (rightHolding || leftHolding)) {
            float target = Math.max(baseX, -1.3F);
            float d = (target - baseX) * holdScale;
            if (rightHolding) model.rightArm.xRot += d;
            if (leftHolding) model.leftArm.xRot += d;
        }

        float t = StiffArmsPose.getAgeInTicks(state);
        float idleScale = 1.0F - attackTime;
        if (idleScale < 0.0F) idleScale = 0.0F;
        if (idleScale > 0.0F) {
            float cos = Mth.cos(t * 0.95F) * 0.02F * idleScale;
            float sin = Mth.sin(t * 0.85F) * 0.015F * idleScale;
            float yaw = Mth.sin(t * 0.75F) * 0.012F * idleScale;
            model.rightArm.zRot += cos;
            model.leftArm.zRot -= cos;
            model.rightArm.xRot += sin;
            model.leftArm.xRot -= sin;
            model.rightArm.yRot += yaw;
            model.leftArm.yRot -= yaw;
        }
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
