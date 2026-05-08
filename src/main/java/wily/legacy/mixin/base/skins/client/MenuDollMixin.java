package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.skins.client.gui.GuiDollRender;
import wily.legacy.skins.client.render.PlayerModelParts;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.skins.client.render.boxloader.AttachSlot;
import wily.legacy.skins.client.render.boxloader.BoxModelManager;
import wily.legacy.skins.client.render.boxloader.BoxModelManager.PivotAnimation;
import wily.legacy.skins.pose.*;
import wily.legacy.skins.skin.SkinIdUtil;

import java.util.EnumMap;

@Mixin(PlayerModel.class)
public abstract class MenuDollMixin {
    private static boolean consoleskins$shouldSyncArms(AvatarRenderState state, String skinId) {
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.SYNC_ARMS, skinId)) return false;
        return state != null && state.id == GuiDollRender.MENU_DOLL_ID;
    }

    private static void consoleskins$applyMenuSyncArms(PlayerModel model) {
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

    private static void consoleskins$applySyncArms(PlayerModel model, AvatarRenderState state, String skinId) {
        if (model == null || state == null || SkinIdUtil.isBlankOrAutoSelect(skinId)) return;
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.SYNC_ARMS, skinId)) return;
        if (state.pose != Pose.STANDING && state.pose != Pose.CROUCHING || state.attackTime > 0.0F) return;
        if (!(state instanceof RenderStateSkinIdAccess access) || access.consoleskins$isUsingItem() || access.consoleskins$isBlocking())
            return;
        float speedSq = access.consoleskins$getMoveSpeedSq();
        if (!access.consoleskins$isMoving() && speedSq <= 1.0E-4F) return;
        float factor = access.consoleskins$isMoving() ? 1.0F : Mth.clamp(speedSq * 120.0F, 0.0F, 1.0F);
        if (factor <= 0.01F) return;
        model.rightArm.xRot += (model.leftArm.xRot - model.rightArm.xRot) * factor;
        model.rightSleeve.xRot = model.rightArm.xRot;
    }

    private static void consoleskins$applyStiffLegs(PlayerModel model, AvatarRenderState state) {
        if (model == null) return;
        boolean sitting = state != null && (state.pose == Pose.SITTING || state.hasPose(Pose.SITTING)
                || state instanceof RenderStateSkinIdAccess access && access.consoleskins$isSitting());
        boolean crouching = state != null && (state.pose == Pose.CROUCHING || state.hasPose(Pose.CROUCHING) || state.isCrouching);
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

    private static void consoleskins$applyBoxPivotAnimations(PlayerModel model, AvatarRenderState state, RenderStateSkinIdAccess access) {
        if (model == null || access == null) return;
        Identifier modelId = access.consoleskins$getCachedModelId();
        if (modelId == null) return;
        EnumMap<AttachSlot, float[]> scales = BoxModelManager.getAnimationScales(modelId);
        EnumMap<AttachSlot, float[]> offsets = BoxModelManager.getAnimationOffsets(modelId);
        EnumMap<AttachSlot, PivotAnimation> animations = BoxModelManager.getPivotAnimations(modelId);
        if ((scales == null || scales.isEmpty()) && (offsets == null || offsets.isEmpty()) && (animations == null || animations.isEmpty())) return;
        float time = consoleskins$animationTime(state);
        boolean moving = state != null && state.id == GuiDollRender.MENU_DOLL_ID || access.consoleskins$isMoving() || access.consoleskins$getMoveSpeedSq() > 1.0E-4F;
        for (AttachSlot slot : PlayerModelParts.ALL) {
            ModelPart part = PlayerModelParts.get(model, slot);
            if (part == null) continue;
            consoleskins$applyBoxAnimation(part, scales == null ? null : scales.get(slot), offsets == null ? null : offsets.get(slot), animations == null ? null : animations.get(slot), time, moving);
        }
    }

    private static void consoleskins$applyBoxAnimation(ModelPart part, float[] scale, float[] offset, PivotAnimation animation, float time, boolean moving) {
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

    private static float consoleskins$animationTime(AvatarRenderState state) {
        if (state != null && state.id == GuiDollRender.MENU_DOLL_ID) {
            return state.ageInTicks == 0.0F ? (System.currentTimeMillis() % 1_000_000L) / 300.0F : state.ageInTicks * 0.16F;
        }
        return StiffArmsPose.getAgeInTicks(state);
    }

    private static float axis(float[] value, int index, float fallback) {
        return value != null && value.length > index ? value[index] : fallback;
    }

    private static float degrees(float value) {
        return value * ((float) Math.PI / 180.0F);
    }

    private static void consoleskins$removeIdleSway(PlayerModel model, AvatarRenderState state) {
        if (model == null) return;
        float ageInTicks = state == null ? 0.0F : state.ageInTicks;
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

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void consoleskins$menuDollFixHeadSpin(AvatarRenderState state, CallbackInfo ci) {
        if (state == null) return;
        PlayerModel self = (PlayerModel) (Object) this;
        RenderStateSkinIdAccess access = state instanceof RenderStateSkinIdAccess skinAccess ? skinAccess : null;
        String skinId = access == null ? null : access.consoleskins$getSkinId();
        boolean stiffLegs = SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.STIFF_LEGS, skinId);
        boolean customAnimation = LegacyOptions.customSkinAnimation.get() && (access == null || !access.consoleskins$skipCustomAnimation());
        boolean noIdleSway = SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.NO_IDLE_SWAY, skinId);
        if (state.id == GuiDollRender.MENU_DOLL_ID) {
            ModelPart head = self.head;
            head.xRot = 0.0F;
            head.yRot = 0.0F;
            head.zRot = 0.0F;
            ModelPart hat = self.hat;
            hat.xRot = 0.0F;
            hat.yRot = 0.0F;
            hat.zRot = 0.0F;
            if (state.isCrouching) {
                head.yRot = 0.15F;
            }
            float t = (System.currentTimeMillis() % 1_000_000L) / 1000.0F;
            float speed = 3.0F;
            float swing = (float) Math.sin(t * speed) * 0.084F;
            self.rightArm.xRot += swing;
            self.leftArm.xRot -= swing;
            self.rightLeg.xRot -= swing;
            self.leftLeg.xRot += swing;
            if (customAnimation && SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.STIFF_ARMS, skinId)) {
                self.rightArm.xRot -= swing;
                self.leftArm.xRot += swing;
                self.rightSleeve.xRot = self.rightArm.xRot;
                self.rightSleeve.yRot = self.rightArm.yRot;
                self.rightSleeve.zRot = self.rightArm.zRot;
                self.leftSleeve.xRot = self.leftArm.xRot;
                self.leftSleeve.yRot = self.leftArm.yRot;
                self.leftSleeve.zRot = self.leftArm.zRot;
            }
        } else if (customAnimation && stiffLegs) {
            consoleskins$applyStiffLegs(self, state);
        }
        if (!customAnimation) {
            if (noIdleSway && state.id != GuiDollRender.MENU_DOLL_ID) consoleskins$removeIdleSway(self, state);
            return;
        }
        boolean zombieArms = ZombieArmsPose.shouldApply(state);
        if (zombieArms) ZombieArmsPose.apply(self, state);
        if (IdleSitPose.shouldApply(state)) {
            if (state.pose == Pose.STANDING || state.pose == Pose.CROUCHING || state.pose == Pose.SWIMMING || state.pose == Pose.FALL_FLYING) {
                IdleSitPose.apply(self);
            }
        }
        if (stiffLegs) consoleskins$applyStiffLegs(self, state);
        if (!ZombieArmsPose.shouldApply(state) && StiffArmsPose.shouldApply(state)) {
            StiffArmsPose.apply(self, state);
        }
        if (StatueOfLibertyPose.shouldApply(state)) {
            StatueOfLibertyPose.apply(self, state);
        }
        if (!zombieArms) {
            if (consoleskins$shouldSyncArms(state, skinId)) consoleskins$applyMenuSyncArms(self);
            else consoleskins$applySyncArms(self, state, skinId);
        }
        if (SyncLegsPose.shouldApply(state)) {
            SyncLegsPose.apply(self);
        }
        if (noIdleSway && !zombieArms && state.id != GuiDollRender.MENU_DOLL_ID) {
            consoleskins$removeIdleSway(self, state);
        }
        consoleskins$applyBoxPivotAnimations(self, state, access);
    }
}
