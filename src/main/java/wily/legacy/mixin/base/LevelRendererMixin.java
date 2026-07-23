package wily.legacy.mixin.base;

//? if <1.20.2 {
/*import it.unimi.dsi.fastutil.objects.ObjectArrayList;
*///?}
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
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
    @Shadow private int ticks;

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

    //? if <1.21.2 {
    @ModifyConstant(method = "renderSnowAndRain", constant = @Constant(intValue = 10))
    private int legacy$weatherRadius(int radius) {
        return 9;
    }

    @ModifyExpressionValue(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float legacy$rainLevel(float rainLevel) {
        return rainLevel * rainLevel;
    }

    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"))
    private float legacy$rainRandom(net.minecraft.util.RandomSource randomSource) {
        return (float) randomSource.nextDouble();
    }

    //? if <1.20.2 {
    /*@ModifyVariable(method = "renderSnowAndRain", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;RAIN_LOCATION:Lnet/minecraft/resources/ResourceLocation;"), to = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;SNOW_LOCATION:Lnet/minecraft/resources/ResourceLocation;")), at = @At("STORE"), index = 35)
    private float legacy$rainAnimation(float animation) {
        return -animation;
    }
    *///?} else {
    @ModifyVariable(method = "renderSnowAndRain", slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;RAIN_LOCATION:Lnet/minecraft/resources/ResourceLocation;"), to = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;SNOW_LOCATION:Lnet/minecraft/resources/ResourceLocation;")), at = @At("STORE"), index = 37)
    private float legacy$rainAnimation(float animation, @Local(ordinal = 5) int z, @Local(ordinal = 6) int x, @Local(ordinal = 3) float speed, @Local(argsOnly = true, ordinal = 0) float partialTick) {
        int phase = ticks + x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761;
        return ((phase & 31) + partialTick) / 32.0F * speed;
    }
    //?}

    //? if <1.21 {
    /*@ModifyArg(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0), index = 3)
    private float legacy$fadeFirstRainTopVertex(float alpha) {
        return 0.0F;
    }

    @ModifyArg(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 1), index = 3)
    private float legacy$fadeSecondRainTopVertex(float alpha) {
        return 0.0F;
    }
    *///?} else {
    @ModifyArg(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0), index = 3)
    private float legacy$fadeFirstRainTopVertex(float alpha) {
        return 0.0F;
    }

    @ModifyArg(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 1), index = 3)
    private float legacy$fadeSecondRainTopVertex(float alpha) {
        return 0.0F;
    }
    //?}
    //?}
}
