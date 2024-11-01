package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOption;

@Mixin(MinecartSoundInstance.class)
public class MinecartSoundIntanceMixin {
    @Inject(method = "canPlaySound", at = @At("RETURN"), cancellable = true)
    public void canPlaySound(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(LegacyOption.minecartSounds.get() && cir.getReturnValue());
    }
}
