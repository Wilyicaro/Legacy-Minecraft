package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.platform.Lighting;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Lighting.class)
public abstract class LightingMixin {

    @ModifyVariable(method = "<init>", at = @At(value = "STORE"), ordinal = 1, remap = false)
    private Matrix4f init(Matrix4f original) {
        return new Matrix4f().scaling(1.0f, -1.0f, 1.0f).rotateYXZ(4.6821041f, 3.2375858f, 0.0f).rotateYXZ(-0.3926991f, 2.3561945f, 0.0f);
    }
}
