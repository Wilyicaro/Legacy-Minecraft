package wily.legacy.mixin.base;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Listener;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyMusicFader;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.SoundEngineAccessor;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin implements SoundEngineAccessor {
    @Shadow private boolean loaded;

    @Shadow @Final private Options options;

    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Shadow @Final private Map<SoundInstance, Integer> queuedSounds;

    @Shadow @Final private List<TickableSoundInstance> tickingSounds;

    @Shadow @Final private Multimap<SoundSource, SoundInstance> instanceBySource;

    @Shadow @Final private Map<SoundInstance, Integer> soundDeleteTime;

    @Shadow @Final private List<TickableSoundInstance> queuedTickableSounds;

    @Shadow protected abstract float calculateVolume(SoundInstance arg);

    @ModifyArg(method = "calculatePitch", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"), index = 2)
    private float calculatePitch(float max, @Local(argsOnly = true) SoundInstance sound) {
        return sound.getLocation().equals(SoundEvents.ITEM_PICKUP./*? if <1.21.2 {*/getLocation/*?} else {*//*location*//*?}*/()) ? 4.0f : max;
    }

    @Override
    public void stopAllSound() {
        if (this.loaded) {
            Predicate<SoundInstance> isNotMusic = (soundInstance) -> !ignoresSoundVolume(soundInstance);
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

    @ModifyArg(method = "updateCategoryVolume", at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private BiConsumer<SoundInstance, ChannelAccess.ChannelHandle> noStopMusic(BiConsumer<SoundInstance, ChannelAccess.ChannelHandle> action) {
        return this::updateChannelVolume;
    }

    @Redirect(method = {"loadLibrary", "updateCategoryVolume"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Listener;setGain(F)V"))
    private void setSoundListenerGain(Listener listener, float volume) {
        listener.setGain(LegacyOptions.unlinkMusicFromMasterVolume.get() ? 1.0f : volume);
    }

    @Inject(method = "updateCategoryVolume", at = @At("RETURN"))
    private void updateSoundVolume(SoundSource source, float volume, CallbackInfo ci) {
        if (!this.loaded || source != SoundSource.MASTER) return;
        if (!LegacyOptions.unlinkMusicFromMasterVolume.get()) {
            this.instanceToChannel.forEach(this::updateChannelVolume);
            return;
        }
        this.instanceToChannel.forEach((instance, handle) -> {
            if (!ignoresSoundVolume(instance)) updateChannelVolume(instance, handle);
        });
    }

    @WrapOperation(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngine;getVolume(Lnet/minecraft/sounds/SoundSource;)F"))
    private float getVolumeWithSoundVolume(SoundEngine instance, SoundSource soundSource, Operation<Float> original) {
        float volume = original.call(instance, soundSource);
        if (!LegacyOptions.unlinkMusicFromMasterVolume.get()) return volume;
        if (soundSource == SoundSource.RECORDS) return volume * this.options.getSoundSourceVolume(SoundSource.MUSIC);
        return ignoresSoundVolume(soundSource) ? volume : volume * this.options.getSoundSourceVolume(SoundSource.MASTER);
    }

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("HEAD"), cancellable = true)
    private void calculateMusicVolume(SoundInstance instance, CallbackInfoReturnable<Float> cir) {
        if (!LegacyOptions.unlinkMusicFromMasterVolume.get() || !ignoresSoundVolume(instance)) return;
        cir.setReturnValue(calculateMusicVolume(instance));
    }

    @WrapOperation(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float calculatePlayVolume(SoundEngine soundEngine, float volume, SoundSource source, Operation<Float> original, @Local(argsOnly = true) SoundInstance instance) {
        if (!LegacyOptions.unlinkMusicFromMasterVolume.get() || !ignoresSoundVolume(instance)) return original.call(soundEngine, volume, source);
        return calculateMusicVolume(instance);
    }

    @WrapOperation(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getSoundSourceVolume(Lnet/minecraft/sounds/SoundSource;)F"))
    private float getSoundSourceVolumeNotMusic(Options instance, SoundSource soundSource, Operation<Float> original) {
        if (!LegacyOptions.unlinkMusicFromMasterVolume.get()) return original.call(instance, soundSource);
        if (soundSource == SoundSource.MASTER) return 1;
        if (soundSource == SoundSource.RECORDS) return original.call(instance, soundSource) * instance.getSoundSourceVolume(SoundSource.MUSIC);
        return original.call(instance, soundSource);
    }

    @Override
    public void setVolume(SoundInstance soundInstance, float volume) {
        ChannelAccess.ChannelHandle channelHandle = this.instanceToChannel.get(soundInstance);
        if (channelHandle != null) channelHandle.execute((channel) -> channel.setVolume(volume * this.calculateVolume(soundInstance)));
    }

    @Override
    public void fadeAllMusic() {
        this.instanceToChannel.keySet().forEach(soundInstance -> {
            if (ignoresSoundVolume(soundInstance))
                LegacyMusicFader.fadeOutMusic(soundInstance, true, true);
        });
    }

    private void updateChannelVolume(SoundInstance instance, ChannelAccess.ChannelHandle handle) {
        float volume = this.calculateVolume(instance);
        handle.execute(channel -> {
            if (volume <= 0 && shouldStopAtZero(instance)) channel.stop();
            else channel.setVolume(volume);
        });
    }

    private float calculateMusicVolume(SoundInstance instance) {
        float volume = instance.getVolume() * this.options.getSoundSourceVolume(SoundSource.MUSIC);
        if (instance.getSource() == SoundSource.RECORDS) volume *= this.options.getSoundSourceVolume(SoundSource.RECORDS);
        return Mth.clamp(volume, 0.0f, 1.0f);
    }

    private static boolean shouldStopAtZero(SoundInstance instance) {
        return !LegacyOptions.unlinkMusicFromMasterVolume.get() || instance.getSource() != SoundSource.MASTER && !ignoresSoundVolume(instance);
    }

    private static boolean ignoresSoundVolume(SoundInstance instance) {
        String path = instance.getLocation().getPath();
        return ignoresSoundVolume(instance.getSource()) || path.startsWith("music.") || path.startsWith("music/");
    }

    private static boolean ignoresSoundVolume(SoundSource source) {
        return source == SoundSource.MUSIC || source == SoundSource.RECORDS;
    }
}
