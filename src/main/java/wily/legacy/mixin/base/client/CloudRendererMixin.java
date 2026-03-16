package wily.legacy.mixin.base.client;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Unique
    private static final float LEGACY_CLOUD_HEIGHT = 128.0f;
    @Unique
    private static final float CLOUD_LAYER_HEIGHT = 4.0f;
    @Unique
    private static final ResourceLocation CONSOLE_CLOUD_TEXTURE = ResourceLocation.fromNamespaceAndPath("legacy", "textures/environment/console_clouds.png");

    @Shadow
    private CloudRenderer.TextureData texture;

    @Redirect(
        method = "prepare",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/resources/ResourceManager;open(Lnet/minecraft/resources/ResourceLocation;)Ljava/io/InputStream;"
        )
    )
    private InputStream legacy$useConsoleCloudTexture(ResourceManager resourceManager, ResourceLocation textureLocation) throws IOException {
        if (!LegacyCloudAtmosphere.areLceCloudsEnabled()) {
            return resourceManager.open(textureLocation);
        }

        ResourceLocation resolvedTexture = resourceManager.getResource(CONSOLE_CLOUD_TEXTURE).isPresent() ? CONSOLE_CLOUD_TEXTURE : textureLocation;
        return resourceManager.open(resolvedTexture);
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private CloudStatus legacy$forceFancyClouds(CloudStatus cloudStatus) {
        if (!LegacyCloudAtmosphere.areLceCloudsEnabled()) {
            return cloudStatus;
        }

        return cloudStatus == CloudStatus.OFF ? cloudStatus : CloudStatus.FANCY;
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
        return LegacyCloudAtmosphere.areLceCloudsEnabled() ? LEGACY_CLOUD_HEIGHT : cloudHeight;
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
        float cloudHeight = LegacyCloudAtmosphere.areLceCloudsEnabled() ? LEGACY_CLOUD_HEIGHT : (float) minecraft.level.dimensionType().cloudHeight().orElse((int) LEGACY_CLOUD_HEIGHT);
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
