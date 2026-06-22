package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
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

    @Inject(method = "setup", at = @At("TAIL"))
    private void setup(Level level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof LocalPlayer player) || !player.isSleeping() || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        float progress = Mth.clamp((player.getSleepTimer() + partialTicks) / 90.0F, 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        move(0.0F, Mth.lerp(eased, 0.32F, 0.14F), 0.0F);
        setRotation(yRot, Mth.lerp(eased, 0.0F, 14.0F));
    }

    @ModifyExpressionValue(method = "setup", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;xRot:F", opcode = Opcodes.GETFIELD))
    protected float invertFrontCameraPitch(float f) {
        return LegacyOptions.invertedFrontCameraPitch.get() ? f : -f;
    }
}
