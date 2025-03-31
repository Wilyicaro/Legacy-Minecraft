package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.sugar.Local;
//? if >1.21.4 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import wily.legacy.client.LegacyRenderPipelines;
import com.mojang.blaze3d.systems.RenderPass;
*///?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.CompiledShaderProgram;
*///?}
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.CommonColor;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    //? if <1.21.2 {
    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;set(FFF)Lorg/joml/Vector3f;", shift = At.Shift.AFTER, remap = false))
    public void updateLightTexture(float f, CallbackInfo ci, @Local(ordinal = 1) Vector3f light, @Local(ordinal = 1) int x, @Local(ordinal = 0) int y) {
        if (x < 15) light.mul(ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }
    //?} else <1.21.5 {
    /*@Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CompiledShaderProgram;safeGetUniform(Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/AbstractUniform;", ordinal = 0))
    public void updateLightTexture(float f, CallbackInfo ci, @Local CompiledShaderProgram shader) {
       shader.safeGetUniform("BlockLightColor").set(ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }
    *///?} else {
    /*@Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setUniform(Ljava/lang/String;[F)V", ordinal = 0, remap = false))
    public void updateLightTexture(float f, CallbackInfo ci, @Local RenderPass pass) {
        pass.setUniform("BlockLightColor", ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }
    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V", remap = false))
    public RenderPipeline changeLightmapPipeline(RenderPipeline renderPipeline) {
        return LegacyRenderPipelines.LIGHTMAP;
    }
    *///?}
}
