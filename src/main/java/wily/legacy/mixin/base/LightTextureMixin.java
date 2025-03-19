package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.sugar.Local;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.CompiledShaderProgram;
*///?}
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.CommonColor;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    //? if <1.21.2 {
    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;set(FFF)Lorg/joml/Vector3f;", shift = At.Shift.AFTER))
    public void updateLightTexture(float f, CallbackInfo ci, @Local(ordinal = 1) Vector3f light, @Local(ordinal = 1) int x, @Local(ordinal = 0) int y) {
        if (x < 15) light.mul(ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }
    //?} else {
    /*@Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CompiledShaderProgram;safeGetUniform(Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/AbstractUniform;", ordinal = 0))
    public void updateLightTexture(float f, CallbackInfo ci, @Local CompiledShaderProgram shader) {
       shader.safeGetUniform("BlockLightColor").set(ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }
    *///?}
}
