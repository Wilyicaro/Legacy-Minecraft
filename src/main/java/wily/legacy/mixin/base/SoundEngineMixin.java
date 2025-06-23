package wily.legacy.mixin.base;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.LegacyMusicFader;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Shadow private boolean loaded;

    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Shadow @Final private Map<SoundInstance, Integer> queuedSounds;

    @Shadow @Final private List<TickableSoundInstance> tickingSounds;

    @Shadow @Final private Multimap<SoundSource, SoundInstance> instanceBySource;

    @Shadow @Final private Map<SoundInstance, Integer> soundDeleteTime;

    @Shadow @Final private List<TickableSoundInstance> queuedTickableSounds;

    @Inject(method = "stopAll", at = @At("HEAD"), cancellable = true)
    public void stopAll(CallbackInfo ci) {
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
            this.instanceToChannel.keySet().forEach(soundInstance -> LegacyMusicFader.fadeOutMusic(soundInstance, true));
            ci.cancel();
        }
    }
}
