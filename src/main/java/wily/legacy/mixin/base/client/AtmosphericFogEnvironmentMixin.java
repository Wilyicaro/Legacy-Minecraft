package wily.legacy.mixin.base.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyCloudAtmosphere;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {
    @Inject(method = "setupFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;environmentalStart:F", ordinal = 1, opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void setupFogStart(FogData fogData, Camera camera, ClientLevel clientLevel, float f, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LegacyOptions.overrideTerrainFogStart.get())
            fogData.environmentalStart = LegacyOptions.getTerrainFogStart() * 16;
    }

    @Inject(method = "setupFog", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;environmentalEnd:F", ordinal = 1, opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void setupFogEnd(FogData fogData, Camera camera, ClientLevel clientLevel, float f, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LegacyOptions.overrideTerrainFogEnd.get())
            fogData.environmentalEnd = LegacyOptions.terrainFogEnd.get().floatValue() * 16;
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private void setupCloudFogOptions(FogData fogData, Camera camera, ClientLevel clientLevel, float f, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LegacyCloudAtmosphere.areLceCloudsEnabled()) {
            fogData.cloudEnd = LegacyCloudAtmosphere.getCloudFogEndBlocks(fogData.environmentalEnd);
        }
    }

}
