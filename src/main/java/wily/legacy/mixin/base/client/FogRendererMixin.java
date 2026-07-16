package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyUnderwaterFog;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Unique
    private Vector4f legacy$exactUnderwaterFogColor;

    @ModifyExpressionValue(method = "computeFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;redFloat(I)F"))
    private float legacy$useExactUnderwaterFogRed(float original, Camera camera, float partialTick, ClientLevel level) {
        if (LegacyUnderwaterFog.isEnabled() && camera.getFluidInCamera() == FogType.WATER) {
            legacy$exactUnderwaterFogColor = LegacyUnderwaterFog.getExactFogColor(level, camera);
            return legacy$exactUnderwaterFogColor.x;
        }

        legacy$exactUnderwaterFogColor = null;
        return original;
    }

    @ModifyExpressionValue(method = "computeFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;greenFloat(I)F"))
    private float legacy$useExactUnderwaterFogGreen(float original) {
        return legacy$exactUnderwaterFogColor == null ? original : legacy$exactUnderwaterFogColor.y;
    }

    @ModifyExpressionValue(method = "computeFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;blueFloat(I)F"))
    private float legacy$useExactUnderwaterFogBlue(float original) {
        if (legacy$exactUnderwaterFogColor == null) {
            return original;
        }

        float blue = legacy$exactUnderwaterFogColor.z;
        legacy$exactUnderwaterFogColor = null;
        return blue;
    }

    @ModifyExpressionValue(method = "computeFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getWaterVision()F"))
    private float legacy$preserveUnderwaterFogColor(float original) {
        return LegacyUnderwaterFog.isEnabled() ? 0.0F : original;
    }

    @Inject(
            method = "setupFog",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
            )
    )
    private void legacy$removeUnderwaterRenderDistanceFog(
            Camera camera, int renderDistanceChunks, DeltaTracker deltaTracker,
            float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<Vector4f> cir,
            @Local FogData fogData) {
        if (LegacyUnderwaterFog.isEnabled() && camera.getFluidInCamera() == FogType.WATER) {
            fogData.renderDistanceStart = fogData.environmentalStart;
            fogData.renderDistanceEnd = fogData.environmentalEnd;
        }
    }
}
