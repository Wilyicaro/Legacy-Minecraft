package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyLightmapRenderState;
import wily.legacy.client.LegacyUnderwaterFog;

@Mixin(LightmapRenderStateExtractor.class)
public class LightTextureMixin {
    @Shadow
    private boolean needsUpdate;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private float legacy$underwaterVisionFactor;

    @Inject(method = "extract", at = @At("HEAD"))
    private void legacy$refreshUnderwaterVisionFactor(LightmapRenderState renderState, float partialTick, CallbackInfo ci) {
        float factor = legacy$calculateUnderwaterVisionFactor(partialTick);
        if (Float.compare(factor, legacy$underwaterVisionFactor) != 0) {
            legacy$underwaterVisionFactor = factor;
            needsUpdate = true;
        }
        LegacyLightmapRenderState.of(renderState).setUnderwaterVisionFactor(factor);
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
        float skyLightFactor = minecraft.gameRenderer.getMainCamera().attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTick);
        return skyLight * Math.max(skyLightFactor, 0.2F) / 15.0F;
    }

    @ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;", ordinal = 0))
    public Object updateLightTexture(Object original) {
        return CommonColor.BLOCK_LIGHT.get() & 0xFFFFFF;
    }
}
