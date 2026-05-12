package wily.legacy.mixin.base.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyCloudAtmosphere;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private void setupFogOptions(FogData fogData, Camera camera, ClientLevel clientLevel, float f, DeltaTracker deltaTracker, CallbackInfo ci) {
        float cloudFogEnd = fogData.environmentalEnd;
        if (LegacyOptions.legacySkyShape.get() && LegacyOptions.overrideTerrainFogEnd.get()) {
            cloudFogEnd = LegacyCloudAtmosphere.getTerrainFogEndBlocks();
            float terrainFadeEnd = LegacyCloudAtmosphere.getTerrainFogFadeEndBlocks();
            fogData.environmentalEnd = terrainFadeEnd;
            fogData.renderDistanceEnd = terrainFadeEnd;
        }
        if (LegacyOptions.legacySkyShape.get() && LegacyOptions.overrideTerrainFogStart.get()) {
            fogData.environmentalStart = Math.min(LegacyCloudAtmosphere.getTerrainFogFadeStartBlocks(), Math.max(0.0f, fogData.environmentalEnd - 1.0f));
            fogData.renderDistanceStart = fogData.environmentalStart;
        }
        if (LegacyCloudAtmosphere.areLceCloudsEnabled()) {
            fogData.skyEnd = Math.min(fogData.skyEnd, LegacyCloudAtmosphere.getSkyFogEndBlocks());
            fogData.cloudEnd = LegacyCloudAtmosphere.getCloudFogEndBlocks(cloudFogEnd);
        }
    }

}
