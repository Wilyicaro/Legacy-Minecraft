package wily.legacy.mixin.base.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyCloudAtmosphere;

@Mixin(DimensionSpecialEffects.OverworldEffects.class)
public abstract class OverworldEffectsMixin {
    @Inject(method = "getSunriseOrSunsetColor", at = @At("HEAD"), cancellable = true)
    private void legacy$useConsoleSunriseColor(float timeOfDay, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(LegacyCloudAtmosphere.getSunriseColor(timeOfDay));
    }
}
