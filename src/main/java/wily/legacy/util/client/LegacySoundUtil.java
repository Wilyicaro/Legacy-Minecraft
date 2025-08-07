package wily.legacy.util.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.SoundManagerAccessor;
import wily.legacy.init.LegacyRegistries;

public class LegacySoundUtil {
    public static void playSimpleUISound(SoundEvent sound, float volume, float pitch, boolean randomPitch){
        RandomSource source = SoundInstance.createUnseededRandom();
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound.location(), SoundSource.UI, volume,pitch + (randomPitch ? (source.nextFloat() - 0.5f) / 10 : 0), source, false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }

    public static void playSimpleUISound(SoundEvent sound, float pitch, boolean randomPitch){
        playSimpleUISound(sound,1.0f, pitch,randomPitch);
    }

    public static void playSimpleUISound(SoundEvent sound, float pitch){
        playSimpleUISound(sound, pitch,false);
    }

    public static void playSimpleUISound(SoundEvent sound, boolean randomPitch){
        playSimpleUISound(sound,1.0f, randomPitch);
    }

    public static void playBackSound(){
        if (LegacyOptions.backSound.get()) playSimpleUISound(LegacyRegistries.BACK.get(), 1.0f);
    }

    public static void setSoundInstanceVolume(SoundInstance soundInstance, float volume) {
        SoundManagerAccessor.of(Minecraft.getInstance().getSoundManager()).setVolume(soundInstance, volume);
    }
}
