package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    //? if >1.21.4 {
    @Shadow(remap = false)
    public static void setShaderLights(Vector3f vector3f, Vector3f vector3f2) {
    }
    @Inject(method = "setupGui3DDiffuseLighting",at = @At("HEAD"),cancellable = true,remap = false)
    private static void setupFor3DItems(Vector3f vector3f, Vector3f vector3f2, CallbackInfo ci) {
        RenderSystem.assertOnRenderThread();
        Matrix4f matrix4f = new Matrix4f().scaling(1.0f, -1.0f, 1.0f).rotateYXZ(4.6821041f, 3.2375858f, 0.0f).rotateYXZ(-0.3926991f, 2.3561945f, 0.0f);
        setShaderLights(matrix4f.transformDirection(vector3f, new Vector3f()), matrix4f.transformDirection(vector3f2, new Vector3f()));
        ci.cancel();
    }
    //?}
}
