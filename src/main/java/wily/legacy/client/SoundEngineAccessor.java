package wily.legacy.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;


public interface SoundEngineAccessor {
    static SoundEngineAccessor of(SoundEngine soundEngine) {
        return (SoundEngineAccessor) soundEngine;
    }

    void setVolume(SoundInstance soundInstance, float volume);
    void fadeAllMusic();
}
