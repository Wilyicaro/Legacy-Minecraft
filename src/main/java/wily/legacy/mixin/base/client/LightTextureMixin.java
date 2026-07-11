package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
//? if >1.21.4 {
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderPass;
//?} else if >=1.21.2 {
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

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MappableRingBuffer;<init>(Ljava/util/function/Supplier;II)V"), index = 2)
    public int changeBufferSize(int i) {
        return (new Std140SizeCalculator()).putFloat().putFloat().putFloat().putInt().putFloat().putFloat().putFloat().putFloat().putVec3().putVec3().putVec3().get();
    }

    @Inject(method = "updateLightTexture", at = @At("HEAD"))
    private void legacy$refreshUnderwaterVisionFactor(float partialTick, CallbackInfo ci) {
        float factor = legacy$calculateUnderwaterVisionFactor(partialTick);
        if (factor != legacy$underwaterVisionFactor) {
            legacy$underwaterVisionFactor = factor;
            updateLightTexture = true;
        }
    }

    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", remap = false, ordinal = 6))
    private Std140Builder legacy$writeUnderwaterVisionFactor(Std140Builder original) {
        return original.putFloat(legacy$underwaterVisionFactor);
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

    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec3(Lorg/joml/Vector3fc;)Lcom/mojang/blaze3d/buffers/Std140Builder;", remap = false, ordinal = 1))
    public Std140Builder updateLightTexture(Std140Builder original) {
        return original.putVec3(ColorUtil.getRed(CommonColor.BLOCK_LIGHT.get()), ColorUtil.getGreen(CommonColor.BLOCK_LIGHT.get()), ColorUtil.getBlue(CommonColor.BLOCK_LIGHT.get()));
    }
}
