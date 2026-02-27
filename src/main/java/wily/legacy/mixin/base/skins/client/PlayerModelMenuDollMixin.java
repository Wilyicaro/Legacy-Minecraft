package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.gui.DollRenderIds;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.StiffArmsConfig;
import wily.legacy.Skins.client.render.StiffArmsPose;
import wily.legacy.Skins.client.render.StiffLegsConfig;
import wily.legacy.Skins.client.render.SkiingPose;
import wily.legacy.Skins.client.render.ZombieArmsPose;
import wily.legacy.Skins.client.render.IdleSitPose;
import wily.legacy.Skins.client.render.WeepingStatuePose;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMenuDollMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void consoleskins$menuDollFixHeadSpin(AvatarRenderState state, CallbackInfo ci) {
        if (state == null) return;

        PlayerModel self = (PlayerModel) (Object) this;

        String skinId = null;
        boolean moving = false;
        if (state instanceof RenderStateSkinIdAccess a) {
            try {
                skinId = a.consoleskins$getSkinId();
            } catch (Throwable ignored) {
            }
            try {
                moving = a.consoleskins$isMoving();
            } catch (Throwable ignored) {
            }
        }

        if (state.id == DollRenderIds.MENU_DOLL_ID) {
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
                hat.yRot = head.yRot;
            }
            float t = (System.currentTimeMillis() % 1_000_000L) / 1000.0F;
            float speed = 3.0F;
            float swing = (float) Math.sin(t * speed) * 0.084F;
            self.rightArm.xRot += swing;
            self.leftArm.xRot -= swing;
            self.rightLeg.xRot -= swing;
            self.leftLeg.xRot += swing;
            if (ConsoleSkinsClientSettings.isSkinAnimations() && skinId != null && StiffArmsConfig.isStiffArmsSkin(skinId)) {
                self.rightArm.xRot -= swing;
                self.leftArm.xRot += swing;
                self.rightSleeve.xRot = self.rightArm.xRot;
                self.rightSleeve.yRot = self.rightArm.yRot;
                self.rightSleeve.zRot = self.rightArm.zRot;
                self.leftSleeve.xRot = self.leftArm.xRot;
                self.leftSleeve.yRot = self.leftArm.yRot;
                self.leftSleeve.zRot = self.leftArm.zRot;
            }
            if (ConsoleSkinsClientSettings.isSkinAnimations() && skinId != null && StiffLegsConfig.isStiffLegsSkin(skinId)) {
                self.rightLeg.xRot = 0.0F;
                self.leftLeg.xRot = 0.0F;
                self.rightLeg.yRot = 0.0F;
                self.leftLeg.yRot = 0.0F;
                self.rightLeg.zRot = 0.0F;
                self.leftLeg.zRot = 0.0F;
                self.rightPants.xRot = self.rightLeg.xRot;
                self.rightPants.yRot = self.rightLeg.yRot;
                self.rightPants.zRot = self.rightLeg.zRot;
                self.leftPants.xRot = self.leftLeg.xRot;
                self.leftPants.yRot = self.leftLeg.yRot;
                self.leftPants.zRot = self.leftLeg.zRot;
            }
        }

        if (!ConsoleSkinsClientSettings.isSkinAnimations()) return;

        if (ZombieArmsPose.shouldApply(state)) {
            ZombieArmsPose.apply(self);
        }

        if (IdleSitPose.shouldApply(state)) {

            if (state.pose == Pose.STANDING || state.pose == Pose.CROUCHING) {
                IdleSitPose.apply(self);
            }
        }

        if (SkiingPose.shouldApply(state)) {
            float t = state.id == DollRenderIds.MENU_DOLL_ID ? (System.currentTimeMillis() % 1_000_000L) / 1000.0F : StiffArmsPose.getAgeInTicks(state);
            SkiingPose.apply(self, state, t);
        }

        if (skinId != null && StiffLegsConfig.isStiffLegsSkin(skinId)) {
            if (state.pose == Pose.STANDING || state.pose == Pose.CROUCHING) {
                self.rightLeg.xRot = 0.0F;
                self.leftLeg.xRot = 0.0F;
                self.rightLeg.yRot = 0.0F;
                self.leftLeg.yRot = 0.0F;
                self.rightLeg.zRot = 0.0F;
                self.leftLeg.zRot = 0.0F;

                self.rightPants.xRot = self.rightLeg.xRot;
                self.rightPants.yRot = self.rightLeg.yRot;
                self.rightPants.zRot = self.rightLeg.zRot;
                self.leftPants.xRot = self.leftLeg.xRot;
                self.leftPants.yRot = self.leftLeg.yRot;
                self.leftPants.zRot = self.leftLeg.zRot;
            }
        }

        if (skinId != null && StiffArmsConfig.isStiffArmsSkin(skinId)) {
            if (state.pose == Pose.STANDING) {
                StiffArmsPose.removeIdleSway(self, StiffArmsPose.getAgeInTicks(state), moving);
            }
        }
        if (WeepingStatuePose.shouldApply(state)) {
            WeepingStatuePose.apply(self);
        }

    }
}
