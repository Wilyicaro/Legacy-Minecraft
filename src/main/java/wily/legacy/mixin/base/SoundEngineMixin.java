package wily.legacy.mixin.base;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyMusicFader;
import wily.legacy.client.SoundEngineAccessor;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin implements SoundEngineAccessor {
    @Shadow private boolean loaded;

    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Shadow @Final private Map<SoundInstance, Integer> queuedSounds;

    @Shadow @Final private List<TickableSoundInstance> tickingSounds;

    @Shadow @Final private Multimap<SoundSource, SoundInstance> instanceBySource;

    @Shadow @Final private Map<SoundInstance, Integer> soundDeleteTime;

    @Shadow @Final private List<TickableSoundInstance> queuedTickableSounds;

    @Shadow protected abstract float calculateVolume(SoundInstance arg);

    @Override
    public void stopAllSound() {
        if (this.loaded) {
            Predicate<SoundInstance> isNotMusic = (soundInstance) -> soundInstance.getSource() != SoundSource.MUSIC && soundInstance.getSource() != SoundSource.RECORDS;
            this.instanceToChannel.forEach((soundInstance, channelHandle) -> {
                if (isNotMusic.test(soundInstance)) channelHandle.execute(Channel::stop);
            });
            this.instanceToChannel.keySet().removeIf(isNotMusic);
            this.instanceBySource.values().removeIf(isNotMusic);
            this.soundDeleteTime.keySet().removeIf(isNotMusic);
            this.queuedSounds.clear();
            this.tickingSounds.clear();
            this.queuedTickableSounds.clear();
        }
    }

    @Redirect(method = "method_19754", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V"))
    private void noStopMusic(ChannelAccess.ChannelHandle instance, Consumer<Channel> consumer, @Local(argsOnly = true) SoundInstance soundInstance, @Local float volume) {
        instance.execute((channel) -> {
            if (volume <= 0 && soundInstance.getSource() != SoundSource.MUSIC && soundInstance.getSource() != SoundSource.RECORDS) channel.stop();
            else channel.setVolume(volume);
        });
    }

    @WrapOperation(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getSoundSourceVolume(Lnet/minecraft/sounds/SoundSource;)F"))
    private float getSoundSourceVolumeNotMusic(Options instance, SoundSource soundSource, Operation<Float> original) {
        if (soundSource == SoundSource.MUSIC || soundSource == SoundSource.RECORDS) return 1;
        else return original.call(instance, soundSource);
    }

    @Override
    public void setVolume(SoundInstance soundInstance, float volume) {
        ChannelAccess.ChannelHandle channelHandle = this.instanceToChannel.get(soundInstance);
        if (channelHandle != null) channelHandle.execute((channel) -> channel.setVolume(volume * this.calculateVolume(soundInstance)));
    }

    @Override
    public void fadeAllMusic() {
        this.instanceToChannel.keySet().forEach(soundInstance -> {
            if (soundInstance.getSource() == SoundSource.MUSIC || soundInstance.getSource() == SoundSource.RECORDS)
                LegacyMusicFader.fadeOutMusic(soundInstance, true, true);
        });
    }
}
