package wily.legacy.Skins.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

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
float rArmX0 = model.rightArm.x;
float rArmY0 = model.rightArm.y;
float rArmZ0 = model.rightArm.z;
float rArmXRot0 = model.rightArm.xRot;
float rArmYRot0 = model.rightArm.yRot;
float rArmZRot0 = model.rightArm.zRot;

float lArmX0 = model.leftArm.x;
float lArmY0 = model.leftArm.y;
float lArmZ0 = model.leftArm.z;
float lArmXRot0 = model.leftArm.xRot;
float lArmYRot0 = model.leftArm.yRot;
float lArmZRot0 = model.leftArm.zRot;

float rSleeveX0 = model.rightSleeve.x;
float rSleeveY0 = model.rightSleeve.y;
float rSleeveZ0 = model.rightSleeve.z;
float rSleeveXRot0 = model.rightSleeve.xRot;
float rSleeveYRot0 = model.rightSleeve.yRot;
float rSleeveZRot0 = model.rightSleeve.zRot;

float lSleeveX0 = model.leftSleeve.x;
float lSleeveY0 = model.leftSleeve.y;
float lSleeveZ0 = model.leftSleeve.z;
float lSleeveXRot0 = model.leftSleeve.xRot;
float lSleeveYRot0 = model.leftSleeve.yRot;
float lSleeveZRot0 = model.leftSleeve.zRot;

boolean rightBlocking = false;
boolean leftBlocking = false;
if (player != null) {
    try {
        if (player.isUsingItem() && player.getUseItem().is(Items.SHIELD)) {
            InteractionHand used = player.getUsedItemHand();
            HumanoidArm main = player.getMainArm();
            HumanoidArm arm = used == InteractionHand.MAIN_HAND ? main : (main == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
            rightBlocking = arm == HumanoidArm.RIGHT;
            leftBlocking = arm == HumanoidArm.LEFT;
        }
    } catch (Throwable ignored) {
    }
}


        float baseX = -1.55F;
        if (state != null && state.isCrouching) baseX = -1.2F;

if (!rightBlocking) {
    model.rightArm.xRot = baseX;
    model.rightArm.yRot = -0.15F;
    model.rightArm.zRot = 0.0F;
}
if (!leftBlocking) {
    model.leftArm.xRot = baseX;
    model.leftArm.yRot = 0.15F;
    model.leftArm.zRot = 0.0F;
}


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
            if (rightHolding && !rightBlocking) model.rightArm.xRot += d;
            if (leftHolding && !leftBlocking) model.leftArm.xRot += d;
        }

        float t = StiffArmsPose.getAgeInTicks(state);
        float idleScale = 1.0F - attackTime;
        if (idleScale < 0.0F) idleScale = 0.0F;
        if (idleScale > 0.0F) {
            float cos = Mth.cos(t * 0.95F) * 0.02F * idleScale;
            float sin = Mth.sin(t * 0.85F) * 0.015F * idleScale;
            float yaw = Mth.sin(t * 0.75F) * 0.012F * idleScale;
if (!rightBlocking) {
    model.rightArm.zRot += cos;
    model.rightArm.xRot += sin;
    model.rightArm.yRot += yaw;
}
if (!leftBlocking) {
    model.leftArm.zRot -= cos;
    model.leftArm.xRot -= sin;
    model.leftArm.yRot -= yaw;
}
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

if (rightBlocking) {
    model.rightArm.x = rArmX0;
    model.rightArm.y = rArmY0;
    model.rightArm.z = rArmZ0;
    model.rightArm.xRot = rArmXRot0;
    model.rightArm.yRot = rArmYRot0;
    model.rightArm.zRot = rArmZRot0;

    model.rightSleeve.x = rSleeveX0;
    model.rightSleeve.y = rSleeveY0;
    model.rightSleeve.z = rSleeveZ0;
    model.rightSleeve.xRot = rSleeveXRot0;
    model.rightSleeve.yRot = rSleeveYRot0;
    model.rightSleeve.zRot = rSleeveZRot0;
} else {
    model.rightSleeve.x = model.rightArm.x;
    model.rightSleeve.y = model.rightArm.y;
    model.rightSleeve.z = model.rightArm.z;
    model.rightSleeve.xRot = model.rightArm.xRot;
    model.rightSleeve.yRot = model.rightArm.yRot;
    model.rightSleeve.zRot = model.rightArm.zRot;
}

if (leftBlocking) {
    model.leftArm.x = lArmX0;
    model.leftArm.y = lArmY0;
    model.leftArm.z = lArmZ0;
    model.leftArm.xRot = lArmXRot0;
    model.leftArm.yRot = lArmYRot0;
    model.leftArm.zRot = lArmZRot0;

    model.leftSleeve.x = lSleeveX0;
    model.leftSleeve.y = lSleeveY0;
    model.leftSleeve.z = lSleeveZ0;
    model.leftSleeve.xRot = lSleeveXRot0;
    model.leftSleeve.yRot = lSleeveYRot0;
    model.leftSleeve.zRot = lSleeveZRot0;
} else {
    model.leftSleeve.x = model.leftArm.x;
    model.leftSleeve.y = model.leftArm.y;
    model.leftSleeve.z = model.leftArm.z;
    model.leftSleeve.xRot = model.leftArm.xRot;
    model.leftSleeve.yRot = model.leftArm.yRot;
    model.leftSleeve.zRot = model.leftArm.zRot;
}
    }
}
