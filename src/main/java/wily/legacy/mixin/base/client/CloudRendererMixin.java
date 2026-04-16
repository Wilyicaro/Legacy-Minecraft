package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
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
import wily.legacy.client.LegacyCloudAtmosphere;
import wily.legacy.client.LegacyRenderPipelines;

import java.nio.ByteBuffer;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Unique
    private static final float LEGACY_CLOUD_HEIGHT = 128.0f;
    @Unique
    private static final float CLOUD_BASE_HEIGHT = 4.0f;
    @Unique
    private static final float CLOUD_TOP_EXTENSION = 0.25f;
    @Unique
    private static final float CLOUD_BOTTOM_EXTENSION = 2.0f / 3.0f;
    @Unique
    private static final float CLOUD_STATE_HYSTERESIS = 0.05f;
    @Unique
    private boolean legacy$lastLceCloudState;
    @Unique
    private boolean legacy$lastLegacyCloudHeightAndTextureState;
    @Unique
    private boolean legacy$lastPackCloudShaderState;
    @Unique
    private int legacy$currentRelativeCameraPos;
    @Unique
    private int legacy$lastRelativeCameraPos = Integer.MIN_VALUE;
    @Unique
    private boolean legacy$useWarmCloudPipelines;

    @Shadow
    private boolean needsRebuild;

    @Shadow
    private CloudRenderer.TextureData texture;

    @Inject(method = "render", at = @At("HEAD"))
    private void legacy$markCloudsForRebuildWhenModeChanges(int color, CloudStatus cloudStatus, float cloudHeight, Vec3 cameraPosition, float ticks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        legacy$useWarmCloudPipelines = minecraft.level != null && LegacyCloudAtmosphere.shouldUseWarmCloudTransparency(minecraft.level, ticks);
        boolean lceCloudsEnabled = LegacyCloudAtmosphere.areLceCloudsEnabled();
        boolean legacyCloudHeightAndTextureEnabled = LegacyCloudAtmosphere.areLegacyCloudHeightAndTextureEnabled();
        boolean packCloudShaderEnabled = LegacyCloudAtmosphere.shouldUsePackCloudShader();
        int relativeCameraPos = legacy$getRelativeCameraPos();
        legacy$currentRelativeCameraPos = relativeCameraPos;
        if (legacy$lastLceCloudState != lceCloudsEnabled || legacy$lastLegacyCloudHeightAndTextureState != legacyCloudHeightAndTextureEnabled || legacy$lastPackCloudShaderState != packCloudShaderEnabled || legacy$lastRelativeCameraPos != relativeCameraPos) {
            legacy$lastLceCloudState = lceCloudsEnabled;
            legacy$lastLegacyCloudHeightAndTextureState = legacyCloudHeightAndTextureEnabled;
            legacy$lastPackCloudShaderState = packCloudShaderEnabled;
            legacy$lastRelativeCameraPos = relativeCameraPos;
            needsRebuild = true;
        }
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private CloudStatus legacy$forceFancyClouds(CloudStatus cloudStatus) {
        if (!LegacyCloudAtmosphere.areLceCloudsEnabled()) {
            return cloudStatus;
        }

        return cloudStatus == CloudStatus.OFF ? cloudStatus : CloudStatus.FANCY;
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/RenderPipelines;CLOUDS:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )
    )
    private RenderPipeline legacy$useLegacyCloudPipeline() {
        if (!LegacyCloudAtmosphere.areLceCloudsEnabled()) {
            return RenderPipelines.CLOUDS;
        }

        if (LegacyCloudAtmosphere.shouldUsePackCloudShader()) {
            return LegacyRenderPipelines.LEGACY_PACK_CLOUDS;
        }

        return legacy$useWarmCloudPipelines ? LegacyRenderPipelines.LEGACY_WARM_CLOUDS : LegacyRenderPipelines.LEGACY_CLOUDS;
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/RenderPipelines;FLAT_CLOUDS:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )
    )
    private RenderPipeline legacy$useLegacyFlatCloudPipeline() {
        if (!LegacyCloudAtmosphere.areLceCloudsEnabled()) {
            return RenderPipelines.FLAT_CLOUDS;
        }

        if (LegacyCloudAtmosphere.shouldUsePackCloudShader()) {
            return LegacyRenderPipelines.LEGACY_PACK_FLAT_CLOUDS;
        }

        return legacy$useWarmCloudPipelines ? LegacyRenderPipelines.LEGACY_WARM_FLAT_CLOUDS : LegacyRenderPipelines.LEGACY_FLAT_CLOUDS;
    }

    @ModifyVariable(method = "render", at = @At(value = "STORE"), index = 6)
    private int legacy$useRenderDistanceCloudDistanceBlocks(int cloudDistanceBlocks) {
        return LegacyCloudAtmosphere.areLceCloudsEnabled() ? legacy$getRenderDistanceCloudDistanceBlocks() : cloudDistanceBlocks;
    }

    @ModifyVariable(method = "render", at = @At(value = "STORE"), index = 7)
    private int legacy$useRenderDistanceCloudRadius(int cloudRadius) {
        return LegacyCloudAtmosphere.areLceCloudsEnabled() ? legacy$getRenderDistanceCloudRadius() : cloudRadius;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float legacy$useLegacyCloudHeight(float cloudHeight) {
        return LegacyCloudAtmosphere.areLegacyCloudHeightAndTextureEnabled() ? LEGACY_CLOUD_HEIGHT : cloudHeight;
    }

    @Inject(method = "buildMesh", at = @At("HEAD"), cancellable = true)
    private void legacy$buildSquareCloudMesh(
        @Coerce Object relativeCameraPos,
        ByteBuffer buffer,
        int cellX,
        int cellZ,
        boolean fancyClouds,
        int cloudRadius,
        CallbackInfo ci
    ) {
        if (!LegacyCloudAtmosphere.areLceCloudsEnabled() || this.texture == null) {
            return;
        }

        CloudTextureDataAccessor textureData = (CloudTextureDataAccessor) (Object) this.texture;
        long[] cells = textureData.legacy$getCells();
        int width = textureData.legacy$getWidth();
        int height = textureData.legacy$getHeight();

        if (fancyClouds) {
            for (int offsetZ = -cloudRadius; offsetZ <= cloudRadius; offsetZ++) {
                for (int offsetX = -cloudRadius; offsetX <= cloudRadius; offsetX++) {
                    legacy$tryBuildSquareCell(buffer, cellX, cellZ, true, true, offsetX, width, offsetZ, height, cells);
                }
            }
            for (int offsetZ = -cloudRadius; offsetZ <= cloudRadius; offsetZ++) {
                for (int offsetX = -cloudRadius; offsetX <= cloudRadius; offsetX++) {
                    legacy$tryBuildSquareCell(buffer, cellX, cellZ, true, false, offsetX, width, offsetZ, height, cells);
                }
            }
        } else {
            for (int offsetZ = -cloudRadius; offsetZ <= cloudRadius; offsetZ++) {
                for (int offsetX = -cloudRadius; offsetX <= cloudRadius; offsetX++) {
                    legacy$tryBuildSquareCell(buffer, cellX, cellZ, false, false, offsetX, width, offsetZ, height, cells);
                }
            }
        }

        ci.cancel();
    }

    @Unique
    private void legacy$tryBuildSquareCell(
        ByteBuffer buffer,
        int cellX,
        int cellZ,
        boolean fancyClouds,
        boolean emitTopAndBottomFaces,
        int offsetX,
        int textureWidth,
        int offsetZ,
        int textureHeight,
        long[] cells
    ) {
        int wrappedX = Math.floorMod(cellX + offsetX, textureWidth);
        int wrappedZ = Math.floorMod(cellZ + offsetZ, textureHeight);
        long cellData = cells[wrappedX + wrappedZ * textureWidth];
        if (cellData == 0L) {
            return;
        }

        if (fancyClouds) {
            legacy$buildSquareExtrudedCell(buffer, offsetX, offsetZ, emitTopAndBottomFaces, cellData);
            return;
        }

        legacy$encodeFace(buffer, offsetX, offsetZ, Direction.DOWN, 32);
    }

    @Unique
    private void legacy$buildSquareExtrudedCell(ByteBuffer buffer, int offsetX, int offsetZ, boolean emitTopAndBottomFaces, long cellData) {
        int relativeCameraPos = legacy$currentRelativeCameraPos;
        boolean renderInsideFaces = relativeCameraPos == 0 && offsetX == 0 && offsetZ == 0;
        if (emitTopAndBottomFaces) {
            if (relativeCameraPos != -1) {
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.UP, 0);
            }

            if (relativeCameraPos != 1) {
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.DOWN, 0);
            }

            if (renderInsideFaces) {
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.UP, 16);
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.DOWN, 16);
            }
        } else {
            if (renderInsideFaces) {
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.NORTH, 16);
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.SOUTH, 16);
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.WEST, 16);
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.EAST, 16);
            }

            if (legacy$isNorthEmpty(cellData) && offsetZ > 0) {
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.NORTH, 0);
            }

            if (legacy$isSouthEmpty(cellData) && offsetZ < 0) {
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.SOUTH, 0);
            }

            if (legacy$isWestEmpty(cellData) && offsetX > 0) {
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.WEST, 0);
            }

            if (legacy$isEastEmpty(cellData) && offsetX < 0) {
                legacy$encodeFace(buffer, offsetX, offsetZ, Direction.EAST, 0);
            }
        }

    }

    @Unique
    private int legacy$getRelativeCameraPos() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0;
        }

        double cameraY = minecraft.gameRenderer.getMainCamera().getPosition().y;
        float cloudHeight = LegacyCloudAtmosphere.areLegacyCloudHeightAndTextureEnabled() ? LEGACY_CLOUD_HEIGHT : (float) minecraft.level.dimensionType().cloudHeight().orElse((int) LEGACY_CLOUD_HEIGHT);
        double top = cloudHeight + CLOUD_BASE_HEIGHT + CLOUD_TOP_EXTENSION;
        double bottom = cloudHeight - CLOUD_BOTTOM_EXTENSION;

        if (legacy$lastRelativeCameraPos == 0) {
            if (cameraY > top + CLOUD_STATE_HYSTERESIS) {
                return 1;
            }

            if (cameraY < bottom - CLOUD_STATE_HYSTERESIS) {
                return -1;
            }

            return 0;
        }

        if (legacy$lastRelativeCameraPos > 0) {
            return cameraY > top - CLOUD_STATE_HYSTERESIS ? 1 : 0;
        }

        if (legacy$lastRelativeCameraPos < 0) {
            return cameraY < bottom + CLOUD_STATE_HYSTERESIS ? -1 : 0;
        }

        if (cameraY > top) {
            return 1;
        }

        if (cameraY < bottom) {
            return -1;
        }

        return 0;
    }

    @Unique
    private int legacy$getRenderDistanceCloudDistanceBlocks() {
        return LegacyCloudAtmosphere.getCloudDrawDistanceBlocks();
    }

    @Unique
    private int legacy$getRenderDistanceCloudRadius() {
        return Math.max(1, (int) Math.ceil(legacy$getRenderDistanceCloudDistanceBlocks() / 12.0d));
    }

    @Unique
    private void legacy$encodeFace(ByteBuffer buffer, int offsetX, int offsetZ, Direction direction, int flags) {
        int encodedFace = direction.get3DDataValue() | flags;
        encodedFace |= (offsetX & 1) << 7;
        encodedFace |= (offsetZ & 1) << 6;
        buffer.put((byte) (offsetX >> 1));
        buffer.put((byte) (offsetZ >> 1));
        buffer.put((byte) encodedFace);
    }

    @Unique
    private static boolean legacy$isNorthEmpty(long cellData) {
        return ((cellData >> 3) & 1L) != 0L;
    }

    @Unique
    private static boolean legacy$isEastEmpty(long cellData) {
        return ((cellData >> 2) & 1L) != 0L;
    }

    @Unique
    private static boolean legacy$isSouthEmpty(long cellData) {
        return ((cellData >> 1) & 1L) != 0L;
    }

    @Unique
    private static boolean legacy$isWestEmpty(long cellData) {
        return (cellData & 1L) != 0L;
    }
}
