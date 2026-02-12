//? >=1.21.11 {
package wily.legacy.mixin.base.client;

import net.minecraft.client.resources.sounds.RidingEntitySoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;

@Mixin(RidingMinecartSoundInstance.class)
public abstract class RidingMinecartSoundInstanceMixin extends RidingEntitySoundInstance {

    public RidingMinecartSoundInstanceMixin(Player player, Entity entity, boolean underwaterSound, SoundEvent soundEvent, SoundSource soundSource, float volumeMin, float volumeMax, float volumeAmplifier) {
        super(player, entity, underwaterSound, soundEvent, soundSource, volumeMin, volumeMax, volumeAmplifier);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void disableMinecartSound(CallbackInfo ci) {
        if (!LegacyOptions.minecartSounds.get()) {
            this.volume = 0;
            ci.cancel();
        }
    }
}
//?}
