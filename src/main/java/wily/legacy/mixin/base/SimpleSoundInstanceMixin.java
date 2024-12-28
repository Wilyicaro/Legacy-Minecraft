package wily.legacy.mixin.base;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SimpleSoundInstance.class)
public class SimpleSoundInstanceMixin {
    @ModifyArg(method = "forUI(Lnet/minecraft/sounds/SoundEvent;F)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;forUI(Lnet/minecraft/sounds/SoundEvent;FF)Lnet/minecraft/client/resources/sounds/SimpleSoundInstance;"),index = 2)
    private static float forUI(float f) {
        return 1.0f;
    }
}
