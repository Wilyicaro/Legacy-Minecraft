package wily.legacy.mixin.base.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyUnderwaterFog;

@Mixin(WaterFogEnvironment.class)
public abstract class WaterFogEnvironmentMixin {
    @Inject(method = "getBaseColor", at = @At("HEAD"), cancellable = true)
    private void getLegacyWaterFogColor(ClientLevel level, Camera camera, int renderDistance, float darkenWorldAmount, CallbackInfoReturnable<Integer> cir) {
        if (LegacyUnderwaterFog.isEnabled()) cir.setReturnValue(LegacyUnderwaterFog.getFogColor(level, camera));
    }

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void setupLegacyWaterFog(FogData fogData, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LegacyUnderwaterFog.setupFog(fogData, camera.entity(), level)) ci.cancel();
    }

    @Inject(method = "isApplicable", at = @At("RETURN"))
    private void resetLegacyWaterFog(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) LegacyUnderwaterFog.reset();
    }
}
