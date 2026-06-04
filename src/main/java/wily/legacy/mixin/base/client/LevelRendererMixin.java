package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.2 {
import net.minecraft.client.renderer.SkyRenderer;
//?}
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyChunkLoading;
import wily.legacy.client.LevelRendererAccessor;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererAccessor {
    @Inject(method = "getLightCoords(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private static void getLightCoords(LevelRenderer.BrightnessGetter brightnessGetter, BlockAndLightGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (LegacyChunkLoading.hasPendingFeatures(pos)) {
            cir.setReturnValue(LightCoordsUtil.max(cir.getReturnValue(), LightCoordsUtil.FULL_SKY));
        }
    }

    //? if <1.21.2 {
    /*@Shadow protected abstract void createLightSky();

    @Shadow protected abstract void createDarkSky();

    @Override
    public void updateSkyBuffers() {
        createLightSky();
        createDarkSky();
    }

    //? if <1.20.5 {
    /^@Inject(method = "buildSkyDisc", at = @At("HEAD"), cancellable = true)
    private static void buildSkyDisc(BufferBuilder bufferBuilder, float f, CallbackInfoReturnable<BufferBuilder.RenderedBuffer> cir) {
        if (LegacyOptions.legacySkyShape.get()) {
            RenderSystem.setShader(GameRenderer::getPositionShader);
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            Legacy4JClient.buildLegacySkyDisc(bufferBuilder, f);
            cir.setReturnValue(bufferBuilder.end());
        }
    }
    ^///?} else {
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

    *///?} else {

    @Shadow
    @Final
    @Mutable
    private SkyRenderer skyRenderer;
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract void onResourceManagerReload(ResourceManager resourceManager);

    @Override
    public void updateSkyBuffers() {
        if (skyRenderer == null) return;
        onResourceManagerReload(minecraft.getResourceManager());
    }
    //?}

    //? if <1.20.5 {
    /*@Redirect(method = "playStreamingMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void waitToPlaySong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeInMusic(soundInstance, true);
    }

    @Redirect(method = "playStreamingMusic", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void fadeJukeboxSong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeOutMusic(soundInstance, true, true);
    }
    *///?} else if <1.21.3 {
    /*@Redirect(method = "playJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void waitToPlaySong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeInMusic(soundInstance, true);
    }

    @Redirect(method = "stopJukeboxSong", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
    public void fadeJukeboxSong(SoundManager instance, SoundInstance soundInstance) {
        LegacyMusicFader.fadeOutMusic(soundInstance, true, true);
    }
    *///?}
}
