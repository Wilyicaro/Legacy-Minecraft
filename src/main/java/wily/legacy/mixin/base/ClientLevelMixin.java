package wily.legacy.mixin.base;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyCloudAtmosphere;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Shadow
    public abstract DimensionSpecialEffects effects();

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void getCloudColor(float partialTick, CallbackInfoReturnable</*? if <1.21.2 {*/Vec3/*?} else {*//*Integer*//*?}*/> cir) {
        if (!LegacyCloudAtmosphere.shouldUseConsoleAtmosphere(effects())) return;
        //? if <1.21.2 {
        cir.setReturnValue(LegacyCloudAtmosphere.getWarmCloudColor(cir.getReturnValue(), LegacyCloudAtmosphere.getTimeOfDay((ClientLevel) (Object) this, partialTick)));
        //?} else {
        /*cir.setReturnValue(LegacyCloudAtmosphere.getWarmCloudColor(cir.getReturnValue(), LegacyCloudAtmosphere.getTimeOfDay((ClientLevel) (Object) this, partialTick)));
        *///?}
    }
}
