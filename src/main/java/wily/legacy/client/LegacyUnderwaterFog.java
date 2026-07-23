package wily.legacy.client;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import wily.legacy.entity.LegacyLocalPlayer;

public final class LegacyUnderwaterFog {
    private static final long BIOME_TRANSITION_MILLIS = 4000L;
    private static final int DEFAULT_FOG_COLOR = 0x007CB7;
    private static final float DEFAULT_FOG_DISTANCE = 15.0F;
    private static final float FOG_START = 4.0F;
    private static final float FOG_END_OFFSET = 12.0F;
    private static final float MAX_DEPTH = 40.0F;
    private static final float DEEPEST_DISTANCE_SCALE = 1.0F / 3.0F;
    private static final int[] X_OFFSETS = {-2, 0, 2, -4, 0, 4, -2, 0, 2};
    private static final int[] Z_OFFSETS = {-2, -4, -2, 0, 0, 0, 2, 4, 2};

    private static ClientLevel lastLevel;
    private static Entity lastCameraEntity;
    private static BlockPos lastCameraPos = BlockPos.ZERO;
    private static boolean underwater;
    private static double entrySurfaceY;
    private static long colorTransitionTime;
    private static float colorCurrentRed;
    private static float colorCurrentGreen;
    private static float colorCurrentBlue;
    private static float colorTargetRed;
    private static float colorTargetGreen;
    private static float colorTargetBlue;
    private static long distanceTransitionTime;
    private static float distanceCurrent;
    private static float distanceTarget;

    private LegacyUnderwaterFog() {
    }

    public static boolean isEnabled() {
        return LegacyBiomeOverride.hasDefaultWaterFogDistance();
    }

    public static int getFogColor(ClientLevel level, Camera camera) {
        long now = Util.getMillis();
        Entity cameraEntity = camera.getEntity();
        update(level, cameraEntity, cameraEntity.blockPosition(), cameraEntity.getY(), now);
        return packLceColor(colorCurrentRed, colorCurrentGreen, colorCurrentBlue);
    }

    public static boolean setupFog(FogRenderer.FogData fogData, Entity cameraEntity, ClientLevel level) {
        if (!isEnabled()) {
            reset();
            return false;
        }
        BlockPos cameraPos = cameraEntity.blockPosition();
        if (!underwater || lastLevel != level || lastCameraEntity != cameraEntity || !lastCameraPos.equals(cameraPos)) {
            update(level, cameraEntity, cameraPos, cameraEntity.getY(), Util.getMillis());
        }
        float clarity = cameraEntity instanceof LegacyLocalPlayer legacyPlayer ? legacyPlayer.getLegacyUnderwaterVisionClarity() : 1.0F;
        fogData.start = FOG_START;
        fogData.end = FOG_END_OFFSET + distanceCurrent * (0.4F + 0.6F * clarity);
        return true;
    }

    public static void reset() {
        underwater = false;
        lastLevel = null;
        lastCameraEntity = null;
        lastCameraPos = BlockPos.ZERO;
    }

    private static void update(ClientLevel level, Entity cameraEntity, BlockPos cameraPos, double cameraY, long now) {
        if (!isEnabled()) {
            reset();
            return;
        }
        Sample sample = sample(level, cameraPos);
        if (!underwater || lastLevel != level || lastCameraEntity != cameraEntity) {
            underwater = true;
            lastLevel = level;
            lastCameraEntity = cameraEntity;
            entrySurfaceY = Math.max(cameraY, level.getSeaLevel());
            colorCurrentRed = colorTargetRed = sample.red;
            colorCurrentGreen = colorTargetGreen = sample.green;
            colorCurrentBlue = colorTargetBlue = sample.blue;
            colorTransitionTime = now;
            distanceCurrent = distanceTarget = applyDepth(sample.distance, cameraPos.getY());
            distanceTransitionTime = now;
        } else {
            updateColorTarget(sample, now);
            updateDistanceTarget(applyDepth(sample.distance, cameraPos.getY()), now);
            advanceTransitions(now);
        }
        lastCameraPos = cameraPos.immutable();
    }

    private static void updateColorTarget(Sample sample, long now) {
        if (Float.compare(sample.red, colorTargetRed) == 0
                && Float.compare(sample.green, colorTargetGreen) == 0
                && Float.compare(sample.blue, colorTargetBlue) == 0) {
            return;
        }
        colorTargetRed = sample.red;
        colorTargetGreen = sample.green;
        colorTargetBlue = sample.blue;
        colorTransitionTime = now;
    }

    private static void updateDistanceTarget(float target, long now) {
        if (Float.compare(target, distanceTarget) == 0) return;
        distanceTarget = target;
        distanceTransitionTime = now;
    }

    private static void advanceTransitions(long now) {
        float colorProgress = transitionProgress(now, colorTransitionTime);
        colorCurrentRed = Mth.lerp(colorProgress, colorCurrentRed, colorTargetRed);
        colorCurrentGreen = Mth.lerp(colorProgress, colorCurrentGreen, colorTargetGreen);
        colorCurrentBlue = Mth.lerp(colorProgress, colorCurrentBlue, colorTargetBlue);
        distanceCurrent = Mth.lerp(transitionProgress(now, distanceTransitionTime), distanceCurrent, distanceTarget);
    }

    private static Sample sample(ClientLevel level, BlockPos center) {
        float red = 0.0F;
        float green = 0.0F;
        float blue = 0.0F;
        float distance = 0.0F;
        int count = 0;
        BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
        for (int y = -1; y <= 1; y++) {
            for (int i = 0; i < X_OFFSETS.length; i++) {
                samplePos.set(center.getX() + X_OFFSETS[i], center.getY() + y, center.getZ() + Z_OFFSETS[i]);
                Holder<Biome> biome = level.getBiome(samplePos);
                LegacyBiomeOverride override = LegacyBiomeOverride.getOrDefault(biome.unwrapKey());
                Integer colorValue = override.waterFogColor();
                if (colorValue == null) colorValue = override.waterColor();
                int color = colorValue == null ? DEFAULT_FOG_COLOR : colorValue;
                Float fogDistance = override.waterFogDistance();
                red += color >> 16 & 0xFF;
                green += color >> 8 & 0xFF;
                blue += color & 0xFF;
                distance += fogDistance == null ? DEFAULT_FOG_DISTANCE : fogDistance;
                count++;
            }
        }
        return new Sample(red / count, green / count, blue / count, distance / count);
    }

    private static float applyDepth(float distance, int cameraBlockY) {
        float depth = (float) (entrySurfaceY - cameraBlockY);
        if (depth <= 0.0F) return distance;
        float depthProgress = Mth.clamp(depth / MAX_DEPTH, 0.0F, 1.0F);
        return distance * Mth.lerp(depthProgress, 1.0F, DEEPEST_DISTANCE_SCALE);
    }

    private static float transitionProgress(long now, long transitionTime) {
        return Mth.clamp((float) (now - transitionTime) / BIOME_TRANSITION_MILLIS, 0.0F, 1.0F);
    }

    private static int packLceColor(float red, float green, float blue) {
        int r = Mth.clamp(Math.round(red * (255.0F / 256.0F)), 0, 255);
        int g = Mth.clamp(Math.round(green * (255.0F / 256.0F)), 0, 255);
        int b = Mth.clamp(Math.round(blue * (255.0F / 256.0F)), 0, 255);
        return r << 16 | g << 8 | b;
    }

    private record Sample(float red, float green, float blue, float distance) {
    }
}
