package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.sugar.Local;
//? if >1.21.4 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import wily.legacy.client.LegacyRenderPipelines;
import com.mojang.blaze3d.systems.RenderPass;
*///?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.CompiledShaderProgram;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyUnderwaterFog;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @Shadow
    private boolean updateLightTexture;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private float legacy$underwaterVisionFactor;

    @Inject(method = "updateLightTexture", at = @At("HEAD"))
    private void legacy$refreshUnderwaterVisionFactor(float partialTick, CallbackInfo ci) {
        float factor = legacy$calculateUnderwaterVisionFactor(partialTick);
        if (factor != legacy$underwaterVisionFactor) {
            legacy$underwaterVisionFactor = factor;
            updateLightTexture = true;
        }
    }

    @Unique
    private float legacy$calculateUnderwaterVisionFactor(float partialTick) {
        ClientLevel level = minecraft.level;
        if (level == null || !LegacyUnderwaterFog.isEnabled()) return 0.0F;
        Entity cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) return 0.0F;
        Vec3 eyePosition = cameraEntity.getEyePosition(partialTick);
        BlockPos eyePos = BlockPos.containing(eyePosition);
        FluidState fluidState = level.getFluidState(eyePos);
        if (!fluidState.is(FluidTags.WATER)
                || eyePosition.y >= eyePos.getY() + fluidState.getHeight(level, eyePos)) return 0.0F;
        int skyLight = level.getBrightness(LightLayer.SKY, eyePos);
        return skyLight * Math.max(level.getSkyDarken(1.0F), 0.2F) / 15.0F;
    }

    //? if <1.21.2 {
    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;set(FFF)Lorg/joml/Vector3f;", shift = At.Shift.AFTER, remap = false))
    public void updateLightTexture(float f, CallbackInfo ci, @Local(ordinal = 1) Vector3f light, @Local(ordinal = 1) int x, @Local(ordinal = 0) int y) {
        if (x < 15) light.mul(ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }

    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;clampColor(Lorg/joml/Vector3f;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void legacy$applyUnderwaterVision(float partialTick, CallbackInfo ci, @Local(ordinal = 1) Vector3f light) {
        if (legacy$underwaterVisionFactor <= 0.0F) return;
        float max = Math.max(light.x, Math.max(light.y, light.z));
        if (max <= 0.0F || max >= 1.0F) return;
        light.lerp(new Vector3f(light).mul(1.0F / max), legacy$underwaterVisionFactor);
    }
    //?} else <1.21.5 {
    /*@Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CompiledShaderProgram;safeGetUniform(Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/AbstractUniform;", ordinal = 0))
    public void updateLightTexture(float f, CallbackInfo ci, @Local CompiledShaderProgram shader) {
       shader.safeGetUniform("UnderwaterVisionFactor").set(legacy$underwaterVisionFactor);
       shader.safeGetUniform("BlockLightColor").set(ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }
    *///?} else {
    /*@Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setUniform(Ljava/lang/String;[F)V", ordinal = 0, remap = false))
    public void updateLightTexture(float f, CallbackInfo ci, @Local RenderPass pass) {
        pass.setUniform("UnderwaterVisionFactor", legacy$underwaterVisionFactor);
        pass.setUniform("BlockLightColor", ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()),ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }
    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V", remap = false))
    public RenderPipeline changeLightmapPipeline(RenderPipeline renderPipeline) {
        return LegacyRenderPipelines.LIGHTMAP;
    }
    *///?}
}
