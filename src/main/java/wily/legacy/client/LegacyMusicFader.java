package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import wily.legacy.mixin.base.MusicManagerAccessor;
import wily.legacy.util.ScreenUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LegacyMusicFader {
    private static final long FADE_TICKS = 70;
    private static long ticks = 0;
    private static final Minecraft mc = Minecraft.getInstance();
    private static final SoundManager soundManager = mc.getSoundManager();
    private static final MusicManagerAccessor musicManagerAccessor = (MusicManagerAccessor) mc.getMusicManager();

    public static SoundInstance queuedSong = null;
    public static Map<SoundInstance, Long> fadingSongs = new HashMap<>();
    public static boolean musicManagerShouldTick = true;

    public static void fadeInMusic(SoundInstance newSong, boolean stopMusicManager) {
        SoundInstance music;
        if ((music = musicManagerAccessor.getCurrentMusic()) != null) fadingSongs.putIfAbsent(music, ticks + FADE_TICKS);
        if (fadingSongs.isEmpty()) soundManager.play(newSong);
        else queuedSong = newSong;
        if (stopMusicManager) musicManagerShouldTick = false;
    }

    public static void fadeOutMusic(SoundInstance fadeMusic, boolean startMusicManager, boolean delayMusicManager) {
        if (queuedSong == fadeMusic) queuedSong = null;
        else fadingSongs.putIfAbsent(fadeMusic, ticks + FADE_TICKS);
        if (startMusicManager) musicManagerShouldTick = true;
        if (delayMusicManager) musicManagerAccessor.setNextSongDelay(1200);
    }

    public static void fadeOutBgMusic(boolean startMusicManager) {
        SoundInstance music = ((MusicManagerAccessor) mc.getMusicManager()).getCurrentMusic();
        if (music != null) fadeOutMusic(music, startMusicManager, false);
    }

    public static void tick() {
        ++ticks;

        for (Iterator<Map.Entry<SoundInstance, Long>> it = fadingSongs.entrySet().iterator(); it.hasNext();) {
            Map.Entry<SoundInstance, Long> entry = it.next();
            SoundInstance song = entry.getKey();
            long songTick = entry.getValue();
            if (ticks >= songTick) {
                soundManager.stop(song);
                it.remove();
            } else {
                ScreenUtil.setSoundInstanceVolume(song, (songTick - ticks) / (float) FADE_TICKS);
            }
        }
        if (fadingSongs.isEmpty() && queuedSong != null) {
            soundManager.play(queuedSong);
            queuedSong = null;
        }
    }
}
