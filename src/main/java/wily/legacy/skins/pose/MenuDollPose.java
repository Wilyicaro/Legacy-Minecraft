package wily.legacy.skins.pose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import wily.legacy.client.LegacyOptions;
import wily.legacy.skins.skin.SkinIdUtil;

public final class MenuDollPose {
    private MenuDollPose() {
    }

    public static Object state(String skinId, boolean crouching, float attackTime) {
        return new State(skinId, crouching, Math.min(1.0F, Math.max(0.0F, attackTime)));
    }

    static boolean isState(Object state) {
        return state instanceof State;
    }

    static String getSkinId(Object state) {
        return state instanceof State dollState ? dollState.skinId() : null;
    }

    static boolean isCrouching(Object state) {
        return state instanceof State dollState && dollState.crouching();
    }

    static float getAttackTime(Object state) {
        return state instanceof State dollState ? dollState.attackTime() : 0.0F;
    }

    private static boolean shouldSyncArms(Object state, String skinId) {
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.SYNC_ARMS, skinId)) return false;
        return ArmPoseSupport.isMenuDoll(state);
    }

    private static void applyMenuSyncArms(PlayerModel model) {
        float xRot = model.leftArm.xRot;
        float yRot = -model.leftArm.yRot;
        float zRot = -model.leftArm.zRot;
        model.rightArm.xRot = xRot;
        model.rightArm.yRot = yRot;
        model.rightArm.zRot = zRot;
        model.rightSleeve.xRot = xRot;
        model.rightSleeve.yRot = yRot;
        model.rightSleeve.zRot = zRot;
        model.leftSleeve.xRot = model.leftArm.xRot;
        model.leftSleeve.yRot = model.leftArm.yRot;
        model.leftSleeve.zRot = model.leftArm.zRot;
    }

    private static void applySyncArms(PlayerModel model, Object state, String skinId) {
        if (model == null || state == null || SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.SYNC_ARMS, skinId)) return;
        Pose pose = ArmPoseSupport.getPose(state);
        if (pose != Pose.STANDING && pose != Pose.CROUCHING || ArmPoseSupport.getAttackTime(state) > 0.0F) return;
        if (ArmPoseSupport.isUsingItem(state) || ArmPoseSupport.isBlocking(state)) return;
        float speedSq = ArmPoseSupport.getMoveSpeedSq(state);
        if (!ArmPoseSupport.isMoving(state) && speedSq <= 1.0E-4F) return;
        float factor = ArmPoseSupport.isMoving(state) ? 1.0F : Mth.clamp(speedSq * 120.0F, 0.0F, 1.0F);
        if (factor <= 0.01F) return;
        model.rightArm.xRot += (model.leftArm.xRot - model.rightArm.xRot) * factor;
        model.rightSleeve.xRot = model.rightArm.xRot;
    }

    private static void applyStiffLegs(PlayerModel model, Object state) {
        if (model == null) return;
        Pose pose = ArmPoseSupport.getPose(state);
        boolean sitting = pose == Pose.SITTING || ArmPoseSupport.isSitting(state);
        boolean crouching = pose == Pose.CROUCHING || ArmPoseSupport.isCrouching(state);
        model.rightLeg.yRot = 0.0F;
        model.leftLeg.yRot = 0.0F;
        model.rightLeg.zRot = 0.0F;
        model.leftLeg.zRot = 0.0F;
        if (sitting) {
            float rot = -((float) Math.PI) / 2.0F + 0.2F;
            model.rightLeg.xRot = rot;
            model.leftLeg.xRot = rot;
            model.rightLeg.x = -2.0F;
            model.leftLeg.x = 2.0F;
            model.rightLeg.z = 0.0F;
            model.leftLeg.z = 0.0F;
        } else if (crouching) {
            model.rightLeg.xRot = 0.0F;
            model.leftLeg.xRot = 0.0F;
        } else {
            model.rightLeg.xRot = 0.0F;
            model.leftLeg.xRot = 0.0F;
            model.rightLeg.x = -1.9F;
            model.leftLeg.x = 1.9F;
            model.rightLeg.z = 0.0F;
            model.leftLeg.z = 0.0F;
        }
        model.rightPants.xRot = model.rightLeg.xRot;
        model.rightPants.yRot = model.rightLeg.yRot;
        model.rightPants.zRot = model.rightLeg.zRot;
        model.rightPants.x = model.rightLeg.x;
        model.rightPants.z = model.rightLeg.z;
        model.leftPants.xRot = model.leftLeg.xRot;
        model.leftPants.yRot = model.leftLeg.yRot;
        model.leftPants.zRot = model.leftLeg.zRot;
        model.leftPants.x = model.leftLeg.x;
        model.leftPants.z = model.leftLeg.z;
    }

    private static void applyMenuDollAnimation(PlayerModel model, Object state, String skinId) {
        if (!ArmPoseSupport.isMenuDoll(state)) return;
        ModelPart head = model.head;
        head.xRot = 0.0F;
        head.yRot = 0.0F;
        head.zRot = 0.0F;
        ModelPart hat = model.hat;
        hat.xRot = 0.0F;
        hat.yRot = 0.0F;
        hat.zRot = 0.0F;
        if (ArmPoseSupport.isCrouching(state)) {
            head.yRot = 0.15F;
        }
        float t = (System.currentTimeMillis() % 1_000_000L) / 1000.0F;
        float speed = 3.0F;
        float swing = (float) Math.sin(t * speed) * 0.084F;
        model.rightArm.xRot += swing;
        model.leftArm.xRot -= swing;
        model.rightLeg.xRot -= swing;
        model.leftLeg.xRot += swing;
        if (LegacyOptions.customSkinAnimation.get() && SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.STIFF_ARMS, skinId)) {
            model.rightArm.xRot -= swing;
            model.leftArm.xRot += swing;
            model.rightSleeve.xRot = model.rightArm.xRot;
            model.rightSleeve.yRot = model.rightArm.yRot;
            model.rightSleeve.zRot = model.rightArm.zRot;
            model.leftSleeve.xRot = model.leftArm.xRot;
            model.leftSleeve.yRot = model.leftArm.yRot;
            model.leftSleeve.zRot = model.leftArm.zRot;
        }
    }

    public static void applySkinPoses(PlayerModel model, Object state) {
        if (model == null || state == null) return;
        String skinId = ArmPoseSupport.getSkinId(state);
        boolean stiffLegs = SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.STIFF_LEGS, skinId);
        boolean customAnimation = LegacyOptions.customSkinAnimation.get();
        applyMenuDollAnimation(model, state, skinId);
        if (!ArmPoseSupport.isMenuDoll(state) && customAnimation && stiffLegs) {
            applyStiffLegs(model, state);
        }
        if (!customAnimation) return;
        boolean zombieArms = ZombieArmsPose.shouldApply(state);
        if (zombieArms) ZombieArmsPose.apply(model, state);
        if (IdleSitPose.shouldApply(state)) {
            Pose pose = ArmPoseSupport.getPose(state);
            if (pose == Pose.STANDING || pose == Pose.CROUCHING || pose == Pose.SWIMMING || pose == Pose.FALL_FLYING) {
                IdleSitPose.apply(model);
            }
        }
        if (stiffLegs) applyStiffLegs(model, state);
        if (!ZombieArmsPose.shouldApply(state) && StiffArmsPose.shouldApply(state)) {
            StiffArmsPose.apply(model, state);
        }
        if (StatueOfLibertyPose.shouldApply(state)) {
            StatueOfLibertyPose.apply(model, state);
        }
        if (!zombieArms) {
            if (shouldSyncArms(state, skinId)) applyMenuSyncArms(model);
            else applySyncArms(model, state, skinId);
        }
        if (SyncLegsPose.shouldApply(state)) {
            SyncLegsPose.apply(model);
        }
    }

    private record State(String skinId, boolean crouching, float attackTime) {
    }
}
