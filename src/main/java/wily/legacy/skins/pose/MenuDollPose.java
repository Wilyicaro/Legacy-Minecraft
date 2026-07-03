package wily.legacy.skins.pose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import wily.legacy.client.LegacyOptions;
import wily.legacy.skins.client.render.PlayerModelParts;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.client.render.boxloader.AttachSlot;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.render.boxloader.BoxModelManager.PivotAnimation;
import wily.legacy.skins.skin.ClientSkinAssets;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.EnumMap;

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

    private static void applyBoxPivotAnimations(PlayerModel model, Object state, String skinId) {
        if (model == null || state == null || SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        ResourceLocation modelId = modelId(state, skinId);
        if (modelId == null) return;
        EnumMap<AttachSlot, float[]> scales = BoxModelManager.getAnimationScales(modelId);
        EnumMap<AttachSlot, float[]> offsets = BoxModelManager.getAnimationOffsets(modelId);
        EnumMap<AttachSlot, PivotAnimation> animations = BoxModelManager.getPivotAnimations(modelId);
        if ((scales == null || scales.isEmpty()) && (offsets == null || offsets.isEmpty()) && (animations == null || animations.isEmpty())) return;
        float time = animationTime(state);
        boolean moving = ArmPoseSupport.isMenuDoll(state) || ArmPoseSupport.isMoving(state) || ArmPoseSupport.getMoveSpeedSq(state) > 1.0E-4F;
        for (AttachSlot slot : PlayerModelParts.ALL) {
            ModelPart part = PlayerModelParts.get(model, slot);
            if (part == null) continue;
            applyBoxAnimation(part, scales == null ? null : scales.get(slot), offsets == null ? null : offsets.get(slot), animations == null ? null : animations.get(slot), time, moving);
        }
    }

    private static ResourceLocation modelId(Object state, String skinId) {
        if (state instanceof RenderStateSkinIdAccess access) return access.consoleskins$getCachedModelId();
        ClientSkinAssets.ResolvedSkin resolved = ClientSkinAssets.resolveSkin(skinId, ArmPoseSupport.getEntityUuid(state));
        return resolved == null ? null : resolved.modelId();
    }

    private static void applyBoxAnimation(ModelPart part, float[] scale, float[] offset, PivotAnimation animation, float time, boolean moving) {
        if (scale != null) {
            part.xRot *= axis(scale, 0, 1.0F);
            part.yRot *= axis(scale, 1, 1.0F);
            part.zRot *= axis(scale, 2, 1.0F);
        }
        if (offset != null) {
            part.xRot += degrees(axis(offset, 0, 0.0F));
            part.yRot += degrees(axis(offset, 1, 0.0F));
            part.zRot += degrees(axis(offset, 2, 0.0F));
        }
        if (animation == null || animation.movingOnly() && !moving) return;
        float wave = Mth.sin(time * animation.speed() + animation.phase());
        float[] base = animation.offset();
        float[] amplitude = animation.amplitude();
        part.xRot += degrees(axis(base, 0, 0.0F) + axis(amplitude, 0, 0.0F) * wave);
        part.yRot += degrees(axis(base, 1, 0.0F) + axis(amplitude, 1, 0.0F) * wave);
        part.zRot += degrees(axis(base, 2, 0.0F) + axis(amplitude, 2, 0.0F) * wave);
    }

    private static float animationTime(Object state) {
        if (ArmPoseSupport.isMenuDoll(state)) return (System.currentTimeMillis() % 1_000_000L) / 300.0F;
        return ArmPoseSupport.getAgeInTicks(state);
    }

    private static float axis(float[] value, int index, float fallback) {
        return value != null && value.length > index ? value[index] : fallback;
    }

    private static float degrees(float value) {
        return value * ((float) Math.PI / 180.0F);
    }

    private static void removeIdleSway(PlayerModel model, Object state) {
        if (model == null) return;
        float ageInTicks = state == null ? 0.0F : ArmPoseSupport.getAgeInTicks(state);
        float zRot = Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        float xRot = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        model.rightArm.zRot -= zRot;
        model.leftArm.zRot += zRot;
        model.rightArm.xRot -= xRot;
        model.leftArm.xRot += xRot;
        model.rightSleeve.zRot = model.rightArm.zRot;
        model.leftSleeve.zRot = model.leftArm.zRot;
        model.rightSleeve.xRot = model.rightArm.xRot;
        model.leftSleeve.xRot = model.leftArm.xRot;
    }

    private static boolean skipCustomAnimation(Object state) {
        return state instanceof RenderStateSkinIdAccess access && access.consoleskins$skipCustomAnimation();
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
        boolean customAnimation = LegacyOptions.customSkinAnimation.get() && !skipCustomAnimation(state);
        boolean noIdleSway = SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.NO_IDLE_SWAY, skinId);
        applyMenuDollAnimation(model, state, skinId);
        if (!ArmPoseSupport.isMenuDoll(state) && customAnimation && stiffLegs) {
            applyStiffLegs(model, state);
        }
        if (!customAnimation) {
            if (noIdleSway && !ArmPoseSupport.isMenuDoll(state)) removeIdleSway(model, state);
            return;
        }
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
        if (noIdleSway && !zombieArms && !ArmPoseSupport.isMenuDoll(state)) {
            removeIdleSway(model, state);
        }
        applyBoxPivotAnimations(model, state, skinId);
    }

    private record State(String skinId, boolean crouching, float attackTime) {
    }
}
