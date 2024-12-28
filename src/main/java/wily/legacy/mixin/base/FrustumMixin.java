package wily.legacy.mixin.base;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Thanks for ABU008, from Legacy4J Group, for suggesting and creating the original code in this mixin
 */
@Mixin(Frustum.class)
public class FrustumMixin {

    @Final
    @Shadow
    private Matrix4f matrix;

    @Inject(method = "calculateFrustum",at = @At("HEAD"))
    private void calculateFrustum(Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        adjustProjectionFOV(matrix4f2).mul(matrix4f, this.matrix);
    }

    @Unique
    private Matrix4f adjustProjectionFOV(Matrix4f projectionMatrix) {
        Matrix4f adjustedMatrix = new Matrix4f(projectionMatrix);
        adjustedMatrix.m00(adjustedMatrix.m00() * 0.65f);
        adjustedMatrix.m11(adjustedMatrix.m11() * 0.65f);
        return adjustedMatrix;
    }
}