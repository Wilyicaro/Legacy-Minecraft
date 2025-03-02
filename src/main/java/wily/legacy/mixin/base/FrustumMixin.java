package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Thanks for ABU008, from Legacy4J Group, for suggesting and creating the original code in this mixin
 */
@Mixin(Frustum.class)
public class FrustumMixin {
    @Shadow @Final private Matrix4f matrix;

    @ModifyExpressionValue(method = "calculateFrustum",at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;mul(Lorg/joml/Matrix4fc;Lorg/joml/Matrix4f;)Lorg/joml/Matrix4f;", remap = false))
    private Matrix4f calculateFrustum(Matrix4f original, Matrix4f right, Matrix4f matrix4f2) {
        return adjustProjectionFOV(matrix4f2).mul(right, matrix);
    }

    @Unique
    private Matrix4f adjustProjectionFOV(Matrix4f projectionMatrix) {
        Matrix4f adjustedMatrix = new Matrix4f(projectionMatrix);
        adjustedMatrix.m00(adjustedMatrix.m00() * 0.65f);
        adjustedMatrix.m11(adjustedMatrix.m11() * 0.65f);
        return adjustedMatrix;
    }
}