package wily.legacy.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import wily.legacy.mixin.base.MusicManagerAccessor;

public class LegacyMusicFader {
    public static final int FADE_TICKS = 70;
    public static int musicStartTick = -1;
    public static int musicStopTick = -1;
    public static SoundInstance fadingMusic;
    public static int ticks = 0;
    private static final Minecraft mc = Minecraft.getInstance();
    private static final SoundManager soundManager = mc.getSoundManager();

    public static void fadeInMusic(SoundInstance newSong) {
        if (fadingMusic != null || ((MusicManagerAccessor) mc.getMusicManager()).getCurrentMusic() != null) {
            musicStartTick = fadingMusic != null ? musicStopTick : ticks + FADE_TICKS;
            soundManager.playDelayed(newSong, fadingMusic != null ? musicStopTick - ticks : FADE_TICKS);
        } else {
            soundManager.play(newSong);
        }
    }

    public static void fadeOutMusic(SoundInstance fadeMusic, boolean delayMusicManager) {
        musicStopTick = ticks + FADE_TICKS;
        if (fadingMusic != null) soundManager.stop(fadingMusic);
        fadingMusic = fadeMusic;
        if (delayMusicManager) ((MusicManagerAccessor) mc.getMusicManager()).setNextSongDelay(1200);
    }

    public static void tick() {
        ++ticks;

        SoundInstance music;
        if (ticks <= musicStartTick && (music = ((MusicManagerAccessor) mc.getMusicManager()).getCurrentMusic()) != null) {
            ScreenUtil.setSoundInstanceVolume(music, (musicStartTick - ticks) / (float) FADE_TICKS);
            if (ticks == musicStartTick) {
                mc.getMusicManager().stopPlaying();
                musicStartTick = -1;
            }
        }

        if (ticks <= musicStopTick && fadingMusic != null) {
            ScreenUtil.setSoundInstanceVolume(fadingMusic, (musicStopTick - ticks) / (float) FADE_TICKS);
            if (ticks == musicStopTick) {
                soundManager.stop(fadingMusic);
                musicStopTick = -1;
                fadingMusic = null;
            }
        }
    }
}
