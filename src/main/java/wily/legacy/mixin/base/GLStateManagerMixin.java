package wily.legacy.mixin.base;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class GLStateManagerMixin {
    @Inject(method = "setupGui3DDiffuseLighting",at = @At("HEAD"),cancellable = true,remap = false)
    private static void setupFor3DItems(Vector3f vector3f, Vector3f vector3f2, CallbackInfo ci) {
        RenderSystem.assertOnRenderThread();
        //? if <1.20.5 {
        /*Matrix4f matrix4f = new Matrix4f().rotationYXZ(-0.6821041f, 3.2375858f, 0.0f).rotateYXZ(0.3926991f, 2.3561945f, 0.0f);
        *///?} else {
         Matrix4f matrix4f =  new Matrix4f().scaling(1.0f, -1.0f, 1.0f).rotateYXZ(4.6821041f, 3.2375858f, 0.0f).rotateYXZ(-0.3926991f, 2.3561945f, 0.0f);
        //?}
        GlStateManager.setupLevelDiffuseLighting(vector3f, vector3f2, matrix4f);
        ci.cancel();
    }
}
