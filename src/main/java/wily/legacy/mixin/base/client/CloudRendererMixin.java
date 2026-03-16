package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.profiling.ProfilerFiller;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Unique
    private static final float LEGACY_CLOUD_HEIGHT = 128.0f;
    @Unique
    private static final float CLOUD_LAYER_HEIGHT = 4.0f;
    @Unique
    private static final ResourceLocation VANILLA_CLOUD_TEXTURE = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    @Unique
    private static final ResourceLocation CONSOLE_CLOUD_TEXTURE = ResourceLocation.fromNamespaceAndPath("legacy", "textures/environment/console_clouds.png");
    @Unique
    private boolean legacy$lastLceCloudState;
    @Unique
    private boolean legacy$lastLegacyCloudHeightAndTextureState;
    @Unique
    private boolean legacy$useWarmCloudPipelines;

    @Shadow
    private boolean needsRebuild;

    @Shadow
    private CloudRenderer.TextureData texture;

    @Inject(method = "prepare", at = @At("HEAD"), cancellable = true)
    private void legacy$useConsoleCloudTexture(
        ResourceManager resourceManager,
        ProfilerFiller profilerFiller,
        CallbackInfoReturnable<Optional<CloudRenderer.TextureData>> cir
    ) {
        ResourceLocation textureLocation = legacy$getCloudTextureLocation(resourceManager);
        if (!textureLocation.equals(CONSOLE_CLOUD_TEXTURE)) {
            return;
        }

        try {
            cir.setReturnValue(Optional.of(legacy$loadTextureData(resourceManager, textureLocation)));
        } catch (IOException ignored) {
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void legacy$markCloudsForRebuildWhenModeChanges(int color, CloudStatus cloudStatus, float cloudHeight, Vec3 cameraPosition, float ticks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        legacy$useWarmCloudPipelines = minecraft.level != null && LegacyCloudAtmosphere.shouldUseWarmCloudTransparency(minecraft.level, ticks);
        boolean lceCloudsEnabled = LegacyCloudAtmosphere.areLceCloudsEnabled();
        boolean legacyCloudHeightAndTextureEnabled = LegacyCloudAtmosphere.areLegacyCloudHeightAndTextureEnabled();
        if (legacy$lastLceCloudState != lceCloudsEnabled || legacy$lastLegacyCloudHeightAndTextureState != legacyCloudHeightAndTextureEnabled) {
            legacy$refreshCloudTexture();
            legacy$lastLceCloudState = lceCloudsEnabled;
            legacy$lastLegacyCloudHeightAndTextureState = legacyCloudHeightAndTextureEnabled;
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

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private Vec3 legacy$widenCloudBand(Vec3 cameraPosition) {
        return cameraPosition;
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

        for (int offsetZ = -cloudRadius; offsetZ <= cloudRadius; offsetZ++) {
            for (int offsetX = -cloudRadius; offsetX <= cloudRadius; offsetX++) {
                legacy$tryBuildSquareCell(buffer, cellX, cellZ, fancyClouds, offsetX, width, offsetZ, height, cells);
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
            legacy$buildSquareExtrudedCell(buffer, offsetX, offsetZ, cellData);
            return;
        }

        legacy$encodeFace(buffer, offsetX, offsetZ, Direction.DOWN, 32);
    }

    @Unique
    private void legacy$buildSquareExtrudedCell(ByteBuffer buffer, int offsetX, int offsetZ, long cellData) {
        int relativeCameraPos = legacy$getRelativeCameraPos();
        if (relativeCameraPos != -1) {
            legacy$encodeFace(buffer, offsetX, offsetZ, Direction.UP, 0);
        }

        if (relativeCameraPos != 1) {
            legacy$encodeFace(buffer, offsetX, offsetZ, Direction.DOWN, 0);
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

        if (Math.abs(offsetX) <= 1 && Math.abs(offsetZ) <= 1) {
            for (Direction direction : Direction.values()) {
                legacy$encodeFace(buffer, offsetX, offsetZ, direction, 16);
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
        if (cameraY > cloudHeight + CLOUD_LAYER_HEIGHT) {
            return 1;
        }

        if (cameraY < cloudHeight) {
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
    private void legacy$refreshCloudTexture() {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        try {
            texture = legacy$loadTextureData(resourceManager, legacy$getCloudTextureLocation(resourceManager));
        } catch (IOException ignored) {
        }
    }

    @Unique
    private static ResourceLocation legacy$getCloudTextureLocation(ResourceManager resourceManager) {
        return LegacyCloudAtmosphere.areLegacyCloudHeightAndTextureEnabled() && resourceManager.getResource(CONSOLE_CLOUD_TEXTURE).isPresent() ? CONSOLE_CLOUD_TEXTURE : VANILLA_CLOUD_TEXTURE;
    }

    @Unique
    private CloudRenderer.TextureData legacy$loadTextureData(ResourceManager resourceManager, ResourceLocation textureLocation) throws IOException {
        try (InputStream inputStream = resourceManager.open(textureLocation); NativeImage nativeImage = NativeImage.read(inputStream)) {
            int width = nativeImage.getWidth();
            int height = nativeImage.getHeight();
            long[] cells = new long[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = nativeImage.getPixel(x, y);
                    if (legacy$isCellEmpty(pixel)) {
                        continue;
                    }

                    boolean northEmpty = legacy$isCellEmpty(nativeImage.getPixel(x, Math.floorMod(y - 1, height)));
                    boolean eastEmpty = legacy$isCellEmpty(nativeImage.getPixel(Math.floorMod(x + 1, width), y));
                    boolean southEmpty = legacy$isCellEmpty(nativeImage.getPixel(x, Math.floorMod(y + 1, height)));
                    boolean westEmpty = legacy$isCellEmpty(nativeImage.getPixel(Math.floorMod(x - 1, width), y));
                    cells[x + y * width] = legacy$packCellData(pixel, northEmpty, eastEmpty, southEmpty, westEmpty);
                }
            }

            return new CloudRenderer.TextureData(cells, width, height);
        }
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

    @Unique
    private static boolean legacy$isCellEmpty(int pixel) {
        return ARGB.alpha(pixel) < 10;
    }

    @Unique
    private static long legacy$packCellData(int pixel, boolean northEmpty, boolean eastEmpty, boolean southEmpty, boolean westEmpty) {
        return ((long) pixel << 4)
            | ((northEmpty ? 1L : 0L) << 3)
            | ((eastEmpty ? 1L : 0L) << 2)
            | ((southEmpty ? 1L : 0L) << 1)
            | (westEmpty ? 1L : 0L);
    }
}
