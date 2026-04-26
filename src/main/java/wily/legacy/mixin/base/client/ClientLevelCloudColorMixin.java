package wily.legacy.mixin.base.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyCloudAtmosphere;

@Mixin(ClientLevel.class)
public abstract class ClientLevelCloudColorMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void legacy$useConsoleSkyColor(Vec3 position, float partialTick, CallbackInfoReturnable<Integer> cir) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (!LegacyCloudAtmosphere.shouldUseConsoleAtmosphere(level)) {
            return;
        }

        cir.setReturnValue(LegacyCloudAtmosphere.getSkyColor(level, position, partialTick));
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void legacy$applySunriseCloudTint(float partialTick, CallbackInfoReturnable<Integer> cir) {
        ClientLevel level = (ClientLevel) (Object) this;
        cir.setReturnValue(LegacyCloudAtmosphere.getSunriseCloudColor(level, partialTick, cir.getReturnValueI()));
    }

}
