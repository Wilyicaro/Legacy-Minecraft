//? if >=1.21.3 {
package wily.legacy.mixin.base;

import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.LegacyMusicFader;

@Mixin(LevelEventHandler.class)
public abstract class LevelEventHandlerMixin {
    @Redirect(method = "playJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;"))
    public SoundEngine.PlayResult waitToPlaySong(SoundManager instance, SoundInstance soundInstance) {
        return LegacyMusicFader.fadeInMusic(soundInstance, true);
    }

    @Redirect(method = "stopJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void fadeJukeboxSong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeOutMusic(soundInstance, true, true);
    }
}
//?}