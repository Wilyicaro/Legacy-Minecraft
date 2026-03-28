package wily.legacy.Skins.client.render;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.model.PlayerModel;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import net.minecraft.world.entity.Pose;

public final class IdleSitPose {
    public static final float BODY_DOWN = 9.25F;

    private IdleSitPose() {
    }

    private static final Map<PlayerModel, BasePose> BASE = new WeakHashMap<>();

    private static BasePose base(PlayerModel model) {
        BasePose b = BASE.get(model);
        if (b != null) return b;
        b = new BasePose(model);
        BASE.put(model, b);
        return b;
    }

    private static final class BasePose {
        final float bodyY, jacketY, headY, hatY;
        final float rArmY, lArmY, rSleeveY, lSleeveY;
        final float rLegY, lLegY, rLegZ, lLegZ;

        BasePose(PlayerModel m) {
            this.bodyY = m.body.y;
            this.jacketY = m.jacket.y;
            this.headY = m.head.y;
            this.hatY = m.hat.y;
            this.rArmY = m.rightArm.y;
            this.lArmY = m.leftArm.y;
            this.rSleeveY = m.rightSleeve.y;
            this.lSleeveY = m.leftSleeve.y;
            this.rLegY = m.rightLeg.y;
            this.lLegY = m.leftLeg.y;
            this.rLegZ = m.rightLeg.z;
            this.lLegZ = m.leftLeg.z;
        }
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (state == null) return false;
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;

        String id;
        try {
            id = a.consoleskins$getSkinId();
        } catch (Throwable t) {
            return false;
        }
        if (id == null || id.isBlank()) return false;

        UUID uuid = null;
        try {
            uuid = a.consoleskins$getEntityUuid();
        } catch (Throwable ignored) {
        }

        if (!SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.IDLE_SIT, id)) {
            if (uuid != null) {
                try { IdleSitTracker.reset(uuid); } catch (Throwable ignored) {}
            }
            return false;
        }

        if (uuid == null) return false;

        boolean moving = false;
        try {
            moving = a.consoleskins$isMoving();
        } catch (Throwable ignored) {
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                Player p = mc.level.getPlayerByUUID(uuid);
                if (p == null || p.isFallFlying() || p.getAbilities().flying) {
                    IdleSitTracker.reset(uuid);
                    return false;
                }
                moving = moving || !p.onGround();
            }
        } catch (Throwable ignored) {
        }

        try {
            if (state.pose != Pose.STANDING && state.pose != Pose.CROUCHING) {
                IdleSitTracker.updateAndShouldSit(uuid, true, state.yRot, state.xRot);
                return false;
            }
        } catch (Throwable ignored) {
        }

        try {
            return IdleSitTracker.updateAndShouldSit(uuid, moving, state.yRot, state.xRot);
        } catch (Throwable t) {
            return false;
        }
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

        try {
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
        } catch (Throwable ignored) {
        }
    }
}
