package wily.legacy.mixin.base;

//? if <1.20.2 {
/*import it.unimi.dsi.fastutil.objects.ObjectArrayList;
*///?}
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
//? if <1.20.2 {
/*import net.minecraft.client.renderer.culling.Frustum;
*///?}
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.SkyRenderer;
*///?}
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
//? if <1.20.2 {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyChunkLoading;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LevelRendererAccessor;
import wily.legacy.client.LegacyMusicFader;

//? if <1.20.2 {
/*import java.util.concurrent.atomic.AtomicBoolean;
*///?}

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererAccessor {
    //? if <1.20.2 {
    /*@Shadow @Final private ObjectArrayList<?> renderChunksInFrustum;
    @Shadow @Final private AtomicBoolean needsFrustumUpdate;

    @Inject(method = "applyFrustum", at = @At("TAIL"))
    private void legacy$filterSlowChunkLoading(Frustum frustum, CallbackInfo ci) {
        if (LegacyChunkLoading.filterLegacy(renderChunksInFrustum)) {
            needsFrustumUpdate.set(true);
        }
    }
    *///?}

    //? if <1.21.5 {
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void legacy$hidePendingFeatureShadows(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (LegacyChunkLoading.hasPendingFeatures(pos)) {
            cir.setReturnValue(LightTexture.pack(LightTexture.block(cir.getReturnValue()), 15));
        }
    }
    //?} else {
    /*@Inject(method = "getLightColor(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void legacy$hidePendingFeatureShadows(LevelRenderer.BrightnessGetter brightnessGetter, BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (LegacyChunkLoading.hasPendingFeatures(pos)) {
            cir.setReturnValue(LightTexture.pack(LightTexture.block(cir.getReturnValue()), 15));
        }
    }
    *///?}

    //? if <1.21.2 {
    @Shadow protected abstract void createLightSky();

    @Shadow protected abstract void createDarkSky();

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

    //? if <1.20.5 && (forge || neoforge) {
    /*@Redirect(method = "playStreamingMusic(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/RecordItem;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void waitToPlaySong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeInMusic(soundInstance, true);
    }

    @Redirect(method = "playStreamingMusic(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/RecordItem;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void fadeJukeboxSong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeOutMusic(soundInstance, true, true);
    }
    *///?} else if <1.20.5 {
    /*@Redirect(method = "playStreamingMusic*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void waitToPlaySong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeInMusic(soundInstance, true);
    }

    @Redirect(method = "playStreamingMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void fadeJukeboxSong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeOutMusic(soundInstance, true, true);
    }
    *///?} else if <1.21.3 {
    @Redirect(method = "playJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void waitToPlaySong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeInMusic(soundInstance, true);
    }

    @Redirect(method = "stopJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void fadeJukeboxSong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeOutMusic(soundInstance, true, true);
    }
    //?}
}
