package wily.legacy.mixin.base.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MusicManager.class)
public interface MusicManagerAccessor {
    @Accessor
    SoundInstance getCurrentMusic();

    @Accessor("nextSongDelay")
    void setNextSongDelay(int delay);
}
