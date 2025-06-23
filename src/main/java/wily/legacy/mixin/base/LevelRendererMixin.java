package wily.legacy.mixin.base;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.SkyRenderer;
*///?}
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LevelRendererAccessor;
import wily.legacy.util.ScreenUtil;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererAccessor {
    //? if <1.21.2 {
    @Shadow protected abstract void createLightSky();

    @Shadow protected abstract void createDarkSky();

    @Shadow private int ticks;

    @Shadow @Final private Minecraft minecraft;

    @Unique public int jukeboxStartTick = -1;
    @Unique public int jukeboxStopTick = -1;
    @Unique public SoundInstance jukeboxSong;

    @Override
    public void updateSkyBuffers() {
        createLightSky();
        createDarkSky();
    }

    //? if <1.20.5 {
    /*@Inject(method = "buildSkyDisc", at = @At("HEAD"), cancellable = true)
    private static void buildSkyDisc(BufferBuilder bufferBuilder, float f, CallbackInfoReturnable<BufferBuilder.RenderedBuffer> cir) {
        if (LegacyOptions.legacySkyShape.get()) {
            RenderSystem.setShader(GameRenderer::getPositionShader);
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            Legacy4JClient.buildLegacySkyDisc(bufferBuilder, f);
            cir.setReturnValue(bufferBuilder.end());
        }
    }
    *///?} else {
    @Inject(method = "buildSkyDisc", at = @At("HEAD"), cancellable = true)
    private static void buildSkyDisc(Tesselator tesselator, float f, CallbackInfoReturnable<MeshData> cir) {
        if (LegacyOptions.legacySkyShape.get()) {
            RenderSystem.setShader(GameRenderer::getPositionShader);
            BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            Legacy4JClient.buildLegacySkyDisc(bufferBuilder, f);
            cir.setReturnValue(bufferBuilder.buildOrThrow());
        }
    }
    //?}

    //?} else {

    /*@Shadow @Final @Mutable
    private SkyRenderer skyRenderer;
    @Override
    public void updateSkyBuffers() {
        skyRenderer.close();
        this.skyRenderer = new SkyRenderer();
    }
    *///?}

    @Redirect(method = "playJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void waitToPlaySong(SoundManager instance, SoundInstance soundInstance) {
        if (((MusicManagerAccessor) this.minecraft.getMusicManager()).getCurrentMusic() != null || jukeboxSong != null) {
            jukeboxStartTick = jukeboxSong != null ? jukeboxStopTick : this.ticks + 70;
            instance.playDelayed(soundInstance, jukeboxSong != null ? jukeboxStopTick - this.ticks : 70);
        } else {
            instance.play(soundInstance);
        }
    }

    @Redirect(method = "stopJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void fadeJukeboxSong(SoundManager instance, SoundInstance soundInstance) {
        jukeboxStopTick = this.ticks + 70;
        if (jukeboxSong != null) this.minecraft.getSoundManager().stop(jukeboxSong);
        jukeboxSong = soundInstance;
        ((MusicManagerAccessor) this.minecraft.getMusicManager()).setNextSongDelay(1200);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void fadeSongs(CallbackInfo ci) {
        SoundInstance music;
        if (this.ticks <= jukeboxStartTick && (music = ((MusicManagerAccessor) this.minecraft.getMusicManager()).getCurrentMusic()) != null) {
            ScreenUtil.setSoundInstanceVolume(music, (jukeboxStartTick - this.ticks) / 70f);
            if (this.ticks == jukeboxStartTick) {
                this.minecraft.getMusicManager().stopPlaying();
                jukeboxStartTick = -1;
            }
        }

        if (this.ticks <= jukeboxStopTick && jukeboxSong != null) {
            ScreenUtil.setSoundInstanceVolume(jukeboxSong, (jukeboxStopTick - this.ticks) / 70f);
            if (this.ticks == jukeboxStopTick) {
                this.minecraft.getSoundManager().stop(jukeboxSong);
                jukeboxStopTick = -1;
                jukeboxSong = null;
            }
        }
    }
}
