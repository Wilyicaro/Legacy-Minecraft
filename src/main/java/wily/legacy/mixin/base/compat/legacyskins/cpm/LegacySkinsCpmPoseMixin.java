package wily.legacy.mixin.base.compat.legacyskins.cpm;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.compat.legacyskins.LegacySkinsCompat;

@Pseudo
@Mixin(targets = "io.github.redrain0o0.legacyskins.client.screen.PlayerSkinWidget$Model", remap = false)
public abstract class LegacySkinsCpmPoseMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"), require = 0, remap = false)
    private void legacy$applyEmbeddedPreviewPose(@Coerce Object safeWidget, PlayerModel model, CallbackInfo ci) {
        if (!LegacySkinsCompat.isRenderingEmbeddedCpmPreview() || model == null) return;
        if (LegacySkinsCompat.isEmbeddedCpmPreviewSneaking()) {
            legacy$applyCrouchPose(model);
        }
        if (LegacySkinsCompat.isEmbeddedCpmPreviewPunching()) {
            legacy$applyPunchLoop(model);
        }
        legacy$syncWearLayers(model);
    }

    private static void legacy$applyCrouchPose(PlayerModel model) {
        model.body.xRot = 0.5F;
        model.rightArm.xRot += 0.4F;
        model.leftArm.xRot += 0.4F;
        model.rightLeg.z = 4.0F;
        model.leftLeg.z = 4.0F;
        model.rightLeg.y = 12.2F;
        model.leftLeg.y = 12.2F;
        model.head.y = 4.2F;
        model.body.y = 3.2F;
        model.leftArm.y = 5.2F;
        model.rightArm.y = 5.2F;
    }

    private static void legacy$applyPunchLoop(PlayerModel model) {
        long swingMs = 300L;
        long phase = System.currentTimeMillis() % (swingMs + 5L);
        float attackTime = phase < swingMs ? phase / (float) swingMs : 0.0F;
        if (attackTime <= 0.0F) return;

        float bodyYaw = Mth.sin(Mth.sqrt(attackTime) * Mth.PI * 2.0F) * 0.2F;
        model.body.yRot = bodyYaw;

        model.rightArm.z = Mth.sin(bodyYaw) * 5.0F;
        model.rightArm.x = -Mth.cos(bodyYaw) * 5.0F;
        model.leftArm.z = -Mth.sin(bodyYaw) * 5.0F;
        model.leftArm.x = Mth.cos(bodyYaw) * 5.0F;

        model.rightArm.yRot += bodyYaw;
        model.leftArm.yRot += bodyYaw;
        model.leftArm.xRot += bodyYaw;

        float ease = 1.0F - attackTime;
        ease *= ease;
        ease *= ease;
        ease = 1.0F - ease;

        float swing = Mth.sin(ease * Mth.PI);
        float lift = Mth.sin(attackTime * Mth.PI) * -(model.head.xRot - 0.7F) * 0.75F;

        model.rightArm.xRot -= swing * 1.2F + lift;
        model.rightArm.yRot += bodyYaw * 2.0F;
        model.rightArm.zRot += Mth.sin(attackTime * Mth.PI) * -0.4F;
    }

    private static void legacy$syncWearLayers(PlayerModel model) {
        legacy$copyPart(model.hat, model.head);
        legacy$copyPart(model.jacket, model.body);
        legacy$copyPart(model.rightSleeve, model.rightArm);
        legacy$copyPart(model.leftSleeve, model.leftArm);
        legacy$copyPart(model.rightPants, model.rightLeg);
        legacy$copyPart(model.leftPants, model.leftLeg);
    }

    private static void legacy$copyPart(ModelPart target, ModelPart source) {
        if (target == null || source == null) return;
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
        target.xScale = source.xScale;
        target.yScale = source.yScale;
        target.zScale = source.zScale;
        target.visible = source.visible;
    }
}
