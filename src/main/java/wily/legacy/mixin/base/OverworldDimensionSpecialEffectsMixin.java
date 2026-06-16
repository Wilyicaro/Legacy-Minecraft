package wily.legacy.mixin.base;

//? if >=1.21.2 {
/*import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyCloudAtmosphere;

@Mixin(DimensionSpecialEffects.OverworldEffects.class)
public class OverworldDimensionSpecialEffectsMixin {
    @Inject(method = "getSunriseOrSunsetColor", at = @At("HEAD"), cancellable = true)
    private void getSunriseOrSunsetColor(float timeOfDay, CallbackInfoReturnable<Integer> cir) {
        if (!LegacyCloudAtmosphere.areLceCloudsEnabled()) return;
        int color = LegacyCloudAtmosphere.getSunriseColor(timeOfDay);
        if (color != 0) cir.setReturnValue(color);
    }

    @Inject(method = "isSunriseOrSunset", at = @At("HEAD"), cancellable = true)
    private void isSunriseOrSunset(float timeOfDay, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCloudAtmosphere.areLceCloudsEnabled()) {
            cir.setReturnValue(LegacyCloudAtmosphere.isSunriseOrSunset(timeOfDay));
        }
    }
}
*///?}
