package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.SoundEngineAccessor;
import wily.legacy.client.SoundManagerAccessor;

@Mixin(SoundManager.class)
public class SoundManagerMixin implements SoundManagerAccessor {
    @Shadow @Final private SoundEngine soundEngine;

    @WrapWithCondition(method = "updateSourceVolume", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop()V"))
    private boolean updateSourceVolume(SoundManager instance, SoundSource source, float volume) {
        return !LegacyOptions.unlinkMusicFromMasterVolume.get();
    }

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
