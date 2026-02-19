package wily.legacy.mixin.base.skins.client;

import wily.legacy.Skins.client.gui.DollRenderIds;
import wily.legacy.Skins.client.render.BadSantaSitConfig;
import wily.legacy.Skins.client.render.IdleSitPose;
import wily.legacy.Skins.client.render.IdleSitTracker;
import wily.legacy.Skins.client.render.RenderStateSkinIdAccess;
import wily.legacy.Skins.client.render.StiffLegsConfig;
import wily.legacy.Skins.client.render.StiffArmsConfig;
import wily.legacy.Skins.client.render.StiffArmsPose;
import wily.legacy.Skins.client.render.ZombieArmsPose;
import wily.legacy.Skins.client.render.SkiingPose;
import wily.legacy.Skins.client.util.ConsoleSkinsClientSettings;

import java.util.UUID;

import wily.legacy.CustomModelSkins.cpm.client.PlayerRenderStateAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMenuDollMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("RETURN"))
    private void consoleskins$cpmLoadState(AvatarRenderState state, CallbackInfo ci) {
        if (state instanceof PlayerRenderStateAccess sa) {
            sa.cpm$loadState((PlayerModel) (Object) this);
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void consoleskins$menuDollPostAnim(AvatarRenderState state, CallbackInfo ci) {
        if (state == null) return;

        PlayerModel self = (PlayerModel) (Object) this;

        String skinId = null;
        if (state instanceof RenderStateSkinIdAccess rsa) {
            try {
                skinId = rsa.consoleskins$getSkinId();
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
            float speed = 4.8F;
            float swing = (float) Math.sin(t * speed) * 0.28F;
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

            if (ConsoleSkinsClientSettings.isSkinAnimations() && StiffLegsConfig.isStiffLegsSkin(skinId)) {
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

        if (ConsoleSkinsClientSettings.isSkinAnimations() && ZombieArmsPose.shouldApply(state)) {
            ZombieArmsPose.apply(self);
        }

        if (ConsoleSkinsClientSettings.isSkinAnimations() && SkiingPose.shouldApply(state)) {
            float t = state.id == DollRenderIds.MENU_DOLL_ID ? (System.currentTimeMillis() % 1_000_000L) / 1000.0F : StiffArmsPose.getAgeInTicks(state);
            SkiingPose.apply(self, state, t);
        }

        if (ConsoleSkinsClientSettings.isSkinAnimations() && state instanceof RenderStateSkinIdAccess a2) {
            String sid = a2.consoleskins$getSkinId();
            if (StiffArmsConfig.isStiffArmsSkin(sid)) {
                StiffArmsPose.removeIdleSway(self, StiffArmsPose.getAgeInTicks(state), a2.consoleskins$isMoving());
            }
        }

        try {
            if (state.id != DollRenderIds.MENU_DOLL_ID && state instanceof RenderStateSkinIdAccess a) {
                Minecraft mc = Minecraft.getInstance();
                if (mc == null || mc.level == null || mc.screen != null) return;

                if (!ConsoleSkinsClientSettings.isSkinAnimations()) {
                    try {
                        IdleSitTracker.reset(a.consoleskins$getEntityUuid());
                    } catch (Throwable ignored) {
                    }
                    return;
                }

                String id = a.consoleskins$getSkinId();

                if (ConsoleSkinsClientSettings.isSkinAnimations() && StiffLegsConfig.isStiffLegsSkin(id)) {

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

                if (ConsoleSkinsClientSettings.isSkinAnimations() && StiffArmsConfig.isStiffArmsSkin(id)) {

                    if (state.pose == Pose.STANDING) {
                        StiffArmsPose.removeIdleSway(self, StiffArmsPose.getAgeInTicks(state), a.consoleskins$isMoving());
                    }
                }
                if (BadSantaSitConfig.isIdleSitSkin(id)) {
                    UUID uuid = a.consoleskins$getEntityUuid();
                    Player p = uuid == null ? null : mc.level.getPlayerByUUID(uuid);

                    if (p == null || p.isFallFlying() || p.getAbilities().flying) {
                        IdleSitTracker.reset(uuid);
                        return;
                    }

                    if (state.pose == Pose.STANDING) {

                        boolean sit = IdleSitTracker.updateAndShouldSit(
                                uuid,
                                a.consoleskins$isMoving() || !p.onGround(),
                                p.getYRot(),
                                p.getXRot()
                        );
                        if (sit) IdleSitPose.apply(self);
                    } else {
                        IdleSitTracker.updateAndShouldSit(uuid, true, p.getYRot(), p.getXRot());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

}
