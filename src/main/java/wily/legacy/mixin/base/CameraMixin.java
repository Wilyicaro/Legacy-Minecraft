package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float yRot;

    @Shadow
    protected abstract void move(/*? if >=1.21 {*/float/*?} else {*//*double*//*?}*/ forwards, /*? if >=1.21 {*/float/*?} else {*//*double*//*?}*/ up, /*? if >=1.21 {*/float/*?} else {*//*double*//*?}*/ right);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @ModifyArg(method = /*? if >=1.21 && (neoforge || forge) {*//*"setRotation(FFF)V", remap = false*//*?} else {*/"setRotation"/*?}*/, at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", ordinal = 0, remap = false), index = 2)
    protected float setRotation(float f) {
        return ScreenUtil.getFlyingViewRollingRotation(f);
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void setup(BlockGetter blockGetter, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(entity instanceof LocalPlayer player) || !player.isSleeping() || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        float progress = Mth.clamp((player.getSleepTimer() + partialTicks) / 90.0F, 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        move(0.0F, Mth.lerp(eased, 0.32F, 0.14F), 0.0F);
        setRotation(yRot, Mth.lerp(eased, 0.0F, 14.0F));
    }

    @ModifyExpressionValue(method = "setup", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;xRot:F"))
    protected float setup(float f) {
        return LegacyOptions.invertedFrontCameraPitch.get() ? f : -f;
    }
}
