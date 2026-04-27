package wily.legacy.mixin.base.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyCloudAtmosphere;

@Mixin(FogEnvironment.class)
public abstract class AirBasedFogEnvironmentMixin {
    @Inject(method = "getBaseColor", at = @At("HEAD"), cancellable = true)
    private void legacy$useConsoleFogColor(
        ClientLevel level,
        Camera camera,
        int renderDistanceChunks,
        float partialTick,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (!((Object) this instanceof AtmosphericFogEnvironment) || !LegacyCloudAtmosphere.shouldUseConsoleAtmosphere(level)) {
            return;
        }

        cir.setReturnValue(LegacyCloudAtmosphere.getAtmosphericFogColor(level, camera, renderDistanceChunks, partialTick));
    }
}
