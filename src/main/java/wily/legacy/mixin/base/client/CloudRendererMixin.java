package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyCloudAtmosphere;
import wily.legacy.client.LegacyRenderPipelines;

import java.nio.ByteBuffer;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Unique
    private static final float LEGACY_CLOUD_HEIGHT = 128.33f;
    @Unique
    private boolean legacy$enabled;
    @Unique
    private boolean legacy$insideClouds;
    @Unique
    private boolean legacy$warmClouds;
    @Unique
    private boolean legacy$packCloudShader;
    @Unique
    private CloudRenderer.TextureData legacy$lastTintTexture;
    @Unique
    private int legacy$cloudTextureTint = 0xFFFFFFFF;

    @Shadow
    private boolean needsRebuild;

    @Shadow
    private CloudRenderer.TextureData texture;

    @Inject(method = "render", at = @At("HEAD"))
    private void legacy$prepareClouds(int color, CloudStatus cloudStatus, float cloudHeight, int cloudDistanceChunks, Vec3 cameraPosition, long ticks, float partialTick, CallbackInfo ci) {
        boolean enabled = cloudStatus != CloudStatus.OFF && LegacyCloudAtmosphere.areLceCloudsEnabled();
        double height = (LegacyCloudAtmosphere.areLegacyCloudHeightAndTextureEnabled() ? LEGACY_CLOUD_HEIGHT : cloudHeight) - cameraPosition.y;
        boolean insideClouds = height > -5.0 && height <= 5.0;
        if (legacy$enabled != enabled || legacy$insideClouds != insideClouds) needsRebuild = true;
        legacy$enabled = enabled;
        legacy$insideClouds = insideClouds;
        if (!enabled) return;

        Minecraft minecraft = Minecraft.getInstance();
        legacy$warmClouds = minecraft.level != null && LegacyCloudAtmosphere.shouldUseWarmCloudTransparency(minecraft.level, partialTick);
        legacy$packCloudShader = LegacyCloudAtmosphere.shouldUsePackCloudShader();
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private CloudStatus legacy$forceFancyClouds(CloudStatus cloudStatus) {
        return LegacyCloudAtmosphere.areLceCloudsEnabled() && cloudStatus != CloudStatus.OFF ? CloudStatus.FANCY : cloudStatus;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int legacy$useCloudTextureTint(int color) {
        if (!LegacyCloudAtmosphere.areLceCloudsEnabled()) return color;
        legacy$updateCloudTextureTint();
        return ARGB.colorFromFloat(
            ARGB.alphaFloat(color),
            ARGB.redFloat(color) * ARGB.redFloat(legacy$cloudTextureTint),
            ARGB.greenFloat(color) * ARGB.greenFloat(legacy$cloudTextureTint),
            ARGB.blueFloat(color) * ARGB.blueFloat(legacy$cloudTextureTint)
        );
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderPipelines;CLOUDS:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"))
    private RenderPipeline legacy$useLegacyCloudPipeline() {
        return legacy$enabled ? legacy$getCloudPipeline().color() : RenderPipelines.CLOUDS;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V", remap = false))
    private void legacy$renderDepthFirst(RenderPass renderPass, int baseVertex, int firstIndex, int indexCount, int instanceCount, Operation<Void> original) {
        if (legacy$enabled) {
            LegacyRenderPipelines.CloudPipeline pipeline = legacy$getCloudPipeline();
            renderPass.setPipeline(pipeline.depth());
            original.call(renderPass, baseVertex, firstIndex, indexCount, instanceCount);
            renderPass.setPipeline(pipeline.color());
        }
        original.call(renderPass, baseVertex, firstIndex, indexCount, instanceCount);
    }

    @Unique
    private LegacyRenderPipelines.CloudPipeline legacy$getCloudPipeline() {
        if (legacy$packCloudShader) {
            return legacy$insideClouds ? LegacyRenderPipelines.LEGACY_PACK_CLOUDS_INSIDE : LegacyRenderPipelines.LEGACY_PACK_CLOUDS;
        }
        if (legacy$warmClouds) {
            return legacy$insideClouds ? LegacyRenderPipelines.LEGACY_WARM_CLOUDS_INSIDE : LegacyRenderPipelines.LEGACY_WARM_CLOUDS;
        }
        return legacy$insideClouds ? LegacyRenderPipelines.LEGACY_CLOUDS_INSIDE : LegacyRenderPipelines.LEGACY_CLOUDS;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int legacy$useExtendedCloudDistance(int cloudDistanceChunks) {
        return LegacyCloudAtmosphere.areLceCloudsEnabled() ? LegacyCloudAtmosphere.getCloudDrawDistanceChunks() : cloudDistanceChunks;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float legacy$useLegacyCloudHeight(float cloudHeight) {
        return LegacyCloudAtmosphere.areLegacyCloudHeightAndTextureEnabled() ? LEGACY_CLOUD_HEIGHT : cloudHeight;
    }

    @Inject(method = "getSizeForCloudDistance", at = @At("HEAD"), cancellable = true)
    private static void legacy$sizeSquareCloudBuffer(int cloudRadius, CallbackInfoReturnable<Integer> cir) {
        if (LegacyCloudAtmosphere.areLceCloudsEnabled()) {
            int diameter = cloudRadius * 2 + 1;
            cir.setReturnValue(diameter * diameter * 6 * 3);
        }
    }

    @Inject(method = "buildMesh", at = @At("HEAD"), cancellable = true)
    private void legacy$buildCloudMesh(@Coerce Object relativeCameraPos, ByteBuffer buffer, int cellX, int cellZ, boolean fancyClouds, int cloudRadius, CallbackInfo ci) {
        if (!legacy$enabled || texture == null) return;

        CloudTextureDataAccessor textureData = (CloudTextureDataAccessor) (Object) texture;
        long[] cells = textureData.legacy$getCells();
        int width = textureData.legacy$getWidth();
        int height = textureData.legacy$getHeight();
        for (int z = -cloudRadius; z <= cloudRadius; z++) {
            for (int x = -cloudRadius; x <= cloudRadius; x++) {
                long cell = cells[Math.floorMod(cellX + x, width) + Math.floorMod(cellZ + z, height) * width];
                if (cell == 0L) continue;

                legacy$encodeFace(buffer, x, z, Direction.DOWN, false);
                legacy$encodeFace(buffer, x, z, Direction.UP, false);
                if (legacy$insideClouds) {
                    int patchX = Math.floorDiv(x, 8);
                    int patchZ = Math.floorDiv(z, 8);
                    if (patchX >= 0) legacy$encodeFace(buffer, x, z, Direction.WEST, patchX <= 1);
                    if (patchX <= 1) legacy$encodeFace(buffer, x, z, Direction.EAST, false);
                    if (patchZ >= 0) legacy$encodeFace(buffer, x, z, Direction.NORTH, patchZ <= 1);
                    if (patchZ <= 1) legacy$encodeFace(buffer, x, z, Direction.SOUTH, false);
                } else {
                    if ((cell & 1L) != 0L) legacy$encodeFace(buffer, x, z, Direction.WEST, false);
                    if ((cell & 4L) != 0L) legacy$encodeFace(buffer, x, z, Direction.EAST, false);
                    if ((cell & 8L) != 0L) legacy$encodeFace(buffer, x, z, Direction.NORTH, false);
                    if ((cell & 2L) != 0L) legacy$encodeFace(buffer, x, z, Direction.SOUTH, false);
                }
            }
        }
        ci.cancel();
    }

    @Unique
    private void legacy$updateCloudTextureTint() {
        if (legacy$lastTintTexture == texture) return;
        legacy$lastTintTexture = texture;
        legacy$cloudTextureTint = 0xFFFFFFFF;
        if (texture == null) return;

        long red = 0;
        long green = 0;
        long blue = 0;
        long weight = 0;
        for (long cell : ((CloudTextureDataAccessor) (Object) texture).legacy$getCells()) {
            if (cell == 0L) continue;
            int color = (int) (cell >> 4);
            int alpha = ARGB.alpha(color);
            red += (long) ARGB.red(color) * alpha;
            green += (long) ARGB.green(color) * alpha;
            blue += (long) ARGB.blue(color) * alpha;
            weight += alpha;
        }
        if (weight > 0) {
            legacy$cloudTextureTint = ARGB.color(255, (int) (red / weight), (int) (green / weight), (int) (blue / weight));
        }
    }

    @Unique
    private void legacy$encodeFace(ByteBuffer buffer, int x, int z, Direction direction, boolean offset) {
        int flags = direction.get3DDataValue() | (offset ? 16 : 0) | (x & 1) << 7 | (z & 1) << 6;
        buffer.put((byte) (x >> 1)).put((byte) (z >> 1)).put((byte) flags);
    }
}
