package wily.legacy.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;

public interface SoundManagerAccessor {
    static SoundManagerAccessor of(SoundManager soundManager) {
        return (SoundManagerAccessor) soundManager;
    }

    void setVolume(SoundInstance soundInstance, float volume);
    void fadeAllMusic();
    void stopAllSound();
}
