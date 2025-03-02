package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

@Mixin(Camera.class)
public class CameraMixin {
    @ModifyArg(method = /*? if >=1.21 && (neoforge || forge) {*//*"setRotation(FFF)V", remap = false*//*?} else {*/"setRotation"/*?}*/, at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", ordinal = 0, remap = false), index = 2)
    protected float setRotation(float f) {
        return ScreenUtil.getFlyingViewRollingRotation(f);
    }
    @ModifyExpressionValue(method = "setup", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Camera;xRot:F"))
    protected float setup(float f) {
        return LegacyOptions.invertedFrontCameraPitch.get() ? f : -f;
    }
}
