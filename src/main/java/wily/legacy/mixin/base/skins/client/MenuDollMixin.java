package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.gui.GuiDollRender;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.pose.IdleSitPose;
import wily.legacy.Skins.pose.SkiingPose;
import wily.legacy.Skins.pose.SkinPoseRegistry;
import wily.legacy.Skins.pose.StiffArmsPose;
import wily.legacy.Skins.pose.SyncLegsPose;
import wily.legacy.Skins.pose.WeepingStatuePose;
import wily.legacy.Skins.pose.ZombieArmsPose;
import wily.legacy.client.LegacyOptions;

@Mixin(PlayerModel.class)
public abstract class MenuDollMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void consoleskins$menuDollFixHeadSpin(AvatarRenderState state, CallbackInfo ci) {
        if (state == null) return;
        PlayerModel self = (PlayerModel) (Object) this;
        String skinId = state instanceof RenderStateSkinIdAccess access ? access.consoleskins$getSkinId() : null;
        boolean stiffLegs = SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.STIFF_LEGS, skinId);
        boolean customAnimation = LegacyOptions.customSkinAnimation.get();
        if (state.id == GuiDollRender.MENU_DOLL_ID) {
            ModelPart head = self.head;
            head.xRot = 0.0F;
            head.yRot = 0.0F;
            head.zRot = 0.0F;
            ModelPart hat = self.hat;
            hat.xRot = 0.0F;
            hat.yRot = 0.0F;
            hat.zRot = 0.0F;
            if (state.isCrouching) { head.yRot = 0.15F; }
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
        } else if (customAnimation && stiffLegs) { consoleskins$applyStiffLegs(self, state); }
        if (!customAnimation) return;
        if (ZombieArmsPose.shouldApply(state)) { ZombieArmsPose.apply(self, state); }
        if (IdleSitPose.shouldApply(state)) {
            if (state.pose == Pose.STANDING || state.pose == Pose.CROUCHING || state.pose == Pose.SWIMMING || state.pose == Pose.FALL_FLYING) { IdleSitPose.apply(self); }
        }
        if (SkiingPose.shouldApply(state)) {
            float t = state.id == GuiDollRender.MENU_DOLL_ID ? (System.currentTimeMillis() % 1_000_000L) / 1000.0F : StiffArmsPose.getAgeInTicks(state);
            SkiingPose.apply(self, state, t);
        }
        if (stiffLegs) consoleskins$applyStiffLegs(self, state);
        if (!ZombieArmsPose.shouldApply(state) && StiffArmsPose.shouldApply(state)) { StiffArmsPose.apply(self, state); }
        if (WeepingStatuePose.shouldApply(state)) { WeepingStatuePose.apply(self, state); }
        if (consoleskins$shouldSyncArms(state, skinId)) { consoleskins$applySyncArms(self); }
        if (SyncLegsPose.shouldApply(state)) { SyncLegsPose.apply(self); }
    }

    private static boolean consoleskins$shouldSyncArms(AvatarRenderState state, String skinId) {
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.SYNC_ARMS, skinId)) return false;
        return state != null && state.id == GuiDollRender.MENU_DOLL_ID;
    }

    private static void consoleskins$applySyncArms(PlayerModel model) {
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
}
