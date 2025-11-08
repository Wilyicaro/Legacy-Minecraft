package wily.legacy.mixin.base.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.client.SoundEngineAccessor;
import wily.legacy.client.SoundManagerAccessor;

@Mixin(SoundManager.class)
public class SoundManagerMixin implements SoundManagerAccessor {
    @Shadow
    @Final
    private SoundEngine soundEngine;

    @Override
    public void setVolume(SoundInstance soundInstance, float volume) {
        SoundEngineAccessor.of(this.soundEngine).setVolume(soundInstance, volume);
    }

    @Override
    public void fadeAllMusic() {
        SoundEngineAccessor.of(this.soundEngine).fadeAllMusic();
    }

    @Override
    public void stopAllSound() {
        SoundEngineAccessor.of(this.soundEngine).stopAllSound();
    }
}
