package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private Entity entity;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private float yRot;

    @Shadow
    protected abstract void move(float forwards, float up, float right);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @ModifyArg(method = /*? if >=1.21 && (neoforge || forge) {*//*"setRotation(FFF)V", remap = false*//*?} else {*/"setRotation"/*?}*/, at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", ordinal = 0, remap = false), index = 0)
    protected float setFlyingViewYRotation(float f) {
        return LegacyRenderUtil.getFlyingViewYRotation(f);
    }

    @ModifyArg(method = /*? if >=1.21 && (neoforge || forge) {*//*"setRotation(FFF)V", remap = false*//*?} else {*/"setRotation"/*?}*/, at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", ordinal = 0, remap = false), index = 2)
    protected float setFlyingViewRollingRotation(float f) {
        return LegacyRenderUtil.getFlyingViewRollingRotation(f);
    }

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void alignWithEntity(float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof LocalPlayer player) || !player.isSleeping() || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        float progress = Mth.clamp((player.getSleepTimer() + partialTicks) / 90.0F, 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        move(0.0F, Mth.lerp(eased, 0.32F, 0.14F), 0.0F);
        setRotation(yRot, Mth.lerp(eased, 0.0F, 14.0F));
    }

    @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;xRot:F"))
    protected float setup(float f) {
        return LegacyOptions.invertedFrontCameraPitch.get() ? f : -f;
    }
}
