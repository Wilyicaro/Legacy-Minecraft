package wily.legacy.mixin.base;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyCloudAtmosphere;

@Mixin(DimensionSpecialEffects.class)
public abstract class DimensionSpecialEffectsMixin {
    @Shadow
    public abstract DimensionSpecialEffects.SkyType skyType();

    @Inject(method = "getCloudHeight", at = @At("RETURN"), cancellable = true)
    private void getCloudHeight(CallbackInfoReturnable<Float> cir) {
        if (LegacyCloudAtmosphere.shouldUseConsoleAtmosphere((DimensionSpecialEffects) (Object) this)) {
            cir.setReturnValue(LegacyCloudAtmosphere.getCloudHeight(cir.getReturnValue()));
        }
    }

    @Inject(method = /*? if <1.21.2 {*/"getSunriseColor"/*?} else {*//*"getSunriseOrSunsetColor"*//*?}*/, at = @At("HEAD"), cancellable = true)
    private void getSunriseColor(float timeOfDay, /*? if <1.21.2 {*/float partialTick, CallbackInfoReturnable<float[]> cir/*?} else {*//*CallbackInfoReturnable<Integer> cir*//*?}*/) {
        if (!LegacyCloudAtmosphere.shouldUseConsoleAtmosphere((DimensionSpecialEffects) (Object) this)) return;
        //? if <1.21.2 {
        float[] color = LegacyCloudAtmosphere.getSunriseColorFloats(timeOfDay);
        if (color != null) cir.setReturnValue(color);
        //?} else {
        /*int color = LegacyCloudAtmosphere.getSunriseColor(timeOfDay);
        if (color != 0) cir.setReturnValue(color);
        *///?}
    }

    //? if >=1.21.2 {
    /*@Inject(method = "isSunriseOrSunset", at = @At("HEAD"), cancellable = true)
    private void isSunriseOrSunset(float timeOfDay, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyCloudAtmosphere.shouldUseConsoleAtmosphere((DimensionSpecialEffects) (Object) this)) {
            cir.setReturnValue(LegacyCloudAtmosphere.isSunriseOrSunset(timeOfDay));
        }
    }
    *///?}
}
