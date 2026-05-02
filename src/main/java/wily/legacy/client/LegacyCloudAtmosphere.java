package wily.legacy.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class LegacyCloudAtmosphere {
    private static final float TWO_PI = 6.2831855f;
    private static final ResourceLocation VANILLA_CLOUD_SHADER = ResourceLocation.withDefaultNamespace("shaders/core/rendertype_clouds.fsh");

    private static final class CloudGeometry {
        private static final float LEGACY_HEIGHT = 128.0f;
        private static final int DRAW_DISTANCE_CHUNKS = 14;

        private CloudGeometry() {
        }
    }

    private static final class SunriseColors {
        private static final float SPAN = 0.4f;
        private static final int DARK = 0xFFB23333;
        private static final int BRIGHT = 0xFFFFE533;

        private SunriseColors() {
        }
    }

    private static final class FogTuning {
        private static final float SUNRISE_TINT_STRENGTH = 0.96f;

        private FogTuning() {
        }
    }

    private static final class CloudTintTuning {
        private static final float NORMAL_WARM_VIEW_THRESHOLD = 0.2f;
        private static final float LEGACY_HEIGHT_WARM_VIEW_THRESHOLD = 0.16f;
        private static final float CLOUD_DIRECTIONAL_TINT_STRENGTH = 0.28f;
        private static final float CLOUD_DIRECTIONAL_TINT_COLOR_MIX = 0.45f;

        private CloudTintTuning() {
        }
    }

    private LegacyCloudAtmosphere() {
    }

    public static boolean areLceCloudsEnabled() {
        return LegacyOptions.lceClouds.get();
    }

    public static boolean areLegacyCloudHeightAndTextureEnabled() {
        return areLceCloudsEnabled() && LegacyOptions.legacyCloudHeightAndTexture.get();
    }

    public static int getCloudDrawDistanceBlocks() {
        return CloudGeometry.DRAW_DISTANCE_CHUNKS * 16;
    }

    public static float getCloudFogEndBlocks(float environmentalEnd) {
        return Math.max(environmentalEnd, getCloudDrawDistanceBlocks());
    }

    public static boolean shouldUseConsoleAtmosphere(ClientLevel level) {
        return areLceCloudsEnabled() && level.effects() instanceof DimensionSpecialEffects.OverworldEffects;
    }

    public static boolean shouldUseWarmCloudTransparency(ClientLevel level, float partialTick) {
        if (!shouldUseConsoleAtmosphere(level)) {
            return false;
        }
        float sampledPartialTick = partialTick - Mth.floor(partialTick);
        if (getSunriseColor(level.getTimeOfDay(sampledPartialTick)) == 0) {
            return false;
        }

        float warmViewThreshold = areLegacyCloudHeightAndTextureEnabled()
            ? CloudTintTuning.LEGACY_HEIGHT_WARM_VIEW_THRESHOLD
            : CloudTintTuning.NORMAL_WARM_VIEW_THRESHOLD;
        return getSunriseCloudViewBlend(level, sampledPartialTick) > warmViewThreshold;
    }

    public static boolean shouldUsePackCloudShader() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getResourceManager().getResourceStack(VANILLA_CLOUD_SHADER).size() > 1;
    }

    public static int getSkyColor(ClientLevel level, Vec3 position, float partialTick) {
        float brightness = getDayBrightness(level.getTimeOfDay(partialTick));
        Vec3 color = CubicSampler.gaussianSampleVec3(position.subtract(2.0F, 2.0F, 2.0F).scale(0.25F), (ix, jx, kx) -> Vec3.fromRGB24((level.getBiomeManager().getNoiseBiomeAtQuart(ix, jx, kx).value()).getSkyColor())).scale(brightness);

        float[] rgb = new float[] { (float) color.x, (float) color.y, (float) color.z};
        
        applyWeatherGreyscale(rgb, level.getRainLevel(partialTick), 0.6f);
        applyWeatherGreyscale(rgb, level.getThunderLevel(partialTick), 0.2f);

        int flashTime = level.getSkyFlashTime();
        if (flashTime > 0) {
            float flash = Mth.clamp(flashTime - partialTick, 0.0f, 1.0f) * 0.45f;
            rgb[0] = rgb[0] * (1.0f - flash) + 0.8f * flash;
            rgb[1] = rgb[1] * (1.0f - flash) + 0.8f * flash;
            rgb[2] = rgb[2] * (1.0f - flash) + flash;
        }

        return ARGB.colorFromFloat(1.0f, rgb[0], rgb[1], rgb[2]);
    }

    public static int getAtmosphericFogColor(ClientLevel level, Camera camera, int renderDistanceChunks, float partialTick) {
        float[] rgb = getDimensionFogRgb(level, camera, partialTick);

        if (renderDistanceChunks >= 4) {
            int sunriseColor = getSunriseColor(level.getTimeOfDay(partialTick));
            if (sunriseColor != 0) {
                float sunriseBlend = getSunriseFogBlend(level, camera, partialTick, sunriseColor);
                if (sunriseBlend > 0.0f) {
                    rgb[0] = Mth.lerp(sunriseBlend, rgb[0], ARGB.redFloat(sunriseColor));
                    rgb[1] = Mth.lerp(sunriseBlend, rgb[1], ARGB.greenFloat(sunriseColor));
                    rgb[2] = Mth.lerp(sunriseBlend, rgb[2], ARGB.blueFloat(sunriseColor));
                }
            }
        }

        int skyColor = getSkyColor(level, camera.getPosition(), partialTick);
        float skyBlend = getFogToSkyBlendFactor(renderDistanceChunks);
        rgb[0] += (ARGB.redFloat(skyColor) - rgb[0]) * skyBlend;
        rgb[1] += (ARGB.greenFloat(skyColor) - rgb[1]) * skyBlend;
        rgb[2] += (ARGB.blueFloat(skyColor) - rgb[2]) * skyBlend;

        applyWeatherDamping(rgb, level.getRainLevel(partialTick));
        applyThunderDamping(rgb, level.getThunderLevel(partialTick));

        return ARGB.colorFromFloat(1.0f, rgb[0], rgb[1], rgb[2]);
    }

    public static int getSunriseCloudColor(ClientLevel level, float partialTick, int baseCloudColor) {
        if (!shouldUseConsoleAtmosphere(level)) {
            return baseCloudColor;
        }

        int sunriseColor = getSunriseColor(level.getTimeOfDay(partialTick));
        if (sunriseColor == 0) {
            return baseCloudColor;
        }

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null) {
            return baseCloudColor;
        }

        float directionalBlend = getSunriseFogBlend(level, camera, partialTick, sunriseColor);
        if (directionalBlend <= 0.0f) {
            return baseCloudColor;
        }

        float tintBlend = directionalBlend * ARGB.alphaFloat(sunriseColor) * CloudTintTuning.CLOUD_DIRECTIONAL_TINT_STRENGTH;
        float redTarget = Mth.lerp(CloudTintTuning.CLOUD_DIRECTIONAL_TINT_COLOR_MIX, ARGB.redFloat(baseCloudColor), ARGB.redFloat(sunriseColor));
        float greenTarget = Mth.lerp(CloudTintTuning.CLOUD_DIRECTIONAL_TINT_COLOR_MIX, ARGB.greenFloat(baseCloudColor), ARGB.greenFloat(sunriseColor));
        float blueTarget = Mth.lerp(CloudTintTuning.CLOUD_DIRECTIONAL_TINT_COLOR_MIX, ARGB.blueFloat(baseCloudColor), ARGB.blueFloat(sunriseColor));
        float red = Mth.lerp(tintBlend, ARGB.redFloat(baseCloudColor), redTarget);
        float green = Mth.lerp(tintBlend, ARGB.greenFloat(baseCloudColor), greenTarget);
        float blue = Mth.lerp(tintBlend, ARGB.blueFloat(baseCloudColor), blueTarget);
        return ARGB.colorFromFloat(1.0f, red, green, blue);
    }

    public static int getSunriseColor(float timeOfDay) {
        float cosine = Mth.cos(timeOfDay * TWO_PI);
        if (cosine < -SunriseColors.SPAN || cosine > SunriseColors.SPAN) {
            return 0;
        }

        float gradient = cosine / SunriseColors.SPAN * 0.5f + 0.5f;
        float alpha = Mth.square(1.0f - (1.0f - Mth.sin(gradient * Mth.PI)) * 0.99f);
        float red = Mth.lerp(gradient, ARGB.redFloat(SunriseColors.DARK), ARGB.redFloat(SunriseColors.BRIGHT));
        float green = Mth.lerp(gradient, ARGB.greenFloat(SunriseColors.DARK), ARGB.greenFloat(SunriseColors.BRIGHT));
        float blue = Mth.lerp(gradient, ARGB.blueFloat(SunriseColors.DARK), ARGB.blueFloat(SunriseColors.BRIGHT));
        return ARGB.colorFromFloat(alpha, red, green, blue);
    }

    private static float[] getDimensionFogRgb(ClientLevel level, Camera camera, float partialTick) {
        float brightness = getDayBrightness(level.getTimeOfDay(partialTick));
        BlockPos blockPos = BlockPos.containing(camera.getPosition());
        Biome biome = level.getBiome(blockPos).value();
        Vec3 fogColor = level.effects().getBrightnessDependentFogColor(Vec3.fromRGB24(biome.getFogColor()), brightness);
        return new float[]{
            (float) fogColor.x,
            (float) fogColor.y,
            (float) fogColor.z
        };
    }

    private static float getDayBrightness(float timeOfDay) {
        return Mth.clamp(Mth.cos(timeOfDay * TWO_PI) * 2.0f + 0.5f, 0.0f, 1.0f);
    }

    private static float getFogToSkyBlendFactor(int renderDistanceChunks) {
        float normalizedDistance = 0.25f + 0.75f * Mth.clamp(renderDistanceChunks, 4, 32) / 32.0f;
        return 1.0f - (float) Math.pow(normalizedDistance, 0.25d);
    }

    private static float getCloudHeight(ClientLevel level) {
        return areLegacyCloudHeightAndTextureEnabled()
            ? CloudGeometry.LEGACY_HEIGHT
            : (float) level.dimensionType().cloudHeight().orElse((int) CloudGeometry.LEGACY_HEIGHT);
    }

    private static float getSunriseFogBlend(ClientLevel level, Camera camera, float partialTick, int sunriseColor) {
        float horizontalFacing = getSunriseHorizontalFacing(level, camera, partialTick);
        if (horizontalFacing <= 0.0f) {
            return 0.0f;
        }

        Vector3f look = camera.getLookVector();
        float horizonWeight = Mth.clamp((float) (1.0d - Math.abs(look.y()) * 1.0d), 0.0f, 1.0f);
        float timeWeight = Mth.sqrt(ARGB.alphaFloat(sunriseColor));
        return Mth.clamp(timeWeight * FogTuning.SUNRISE_TINT_STRENGTH * Mth.sqrt(horizontalFacing) * Mth.sqrt(horizonWeight), 0.0f, 1.0f);
    }

    private static float getSunriseCloudViewBlend(ClientLevel level, float partialTick) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null) {
            return 0.0f;
        }

        float horizontalFacing = getSunriseHorizontalFacing(level, camera, partialTick);
        if (horizontalFacing <= 0.0f) {
            return 0.0f;
        }

        Vector3f look = camera.getLookVector();
        float horizonWeight = Mth.clamp((float) (1.0d - Math.abs(look.y()) * 1.05d), 0.0f, 1.0f);
        float softenedFacing = Mth.sqrt(horizontalFacing);
        float directionalWeight = softenedFacing * (0.65f + 0.35f * softenedFacing);
        return Mth.clamp(directionalWeight * Mth.sqrt(horizonWeight), 0.0f, 1.0f);
    }

    private static float getSunriseHorizontalFacing(ClientLevel level, Camera camera, float partialTick) {
        Vector3f look = camera.getLookVector();
        double horizontalLength = Math.sqrt(look.x() * look.x() + look.z() * look.z());
        if (horizontalLength < 1.0e-4d) {
            return 0.0f;
        }

        float sunriseDirection = Mth.sin(level.getSunAngle(partialTick)) > 0.0f ? -1.0f : 1.0f;
        return Mth.clamp((float) ((look.x() / horizontalLength) * sunriseDirection), 0.0f, 1.0f);
    }

    private static void applyWeatherGreyscale(float[] rgb, float weatherStrength, float greyscaleStrength) {
        if (weatherStrength <= 0.0f) {
            return;
        }

        float greyscale = (rgb[0] * 0.30f + rgb[1] * 0.59f + rgb[2] * 0.11f) * greyscaleStrength;
        float balance = 1.0f - weatherStrength * 0.75f;
        rgb[0] = rgb[0] * balance + greyscale * (1.0f - balance);
        rgb[1] = rgb[1] * balance + greyscale * (1.0f - balance);
        rgb[2] = rgb[2] * balance + greyscale * (1.0f - balance);
    }

    private static void applyWeatherDamping(float[] rgb, float rainLevel) {
        if (rainLevel <= 0.0f) {
            return;
        }

        float redGreenDamping = 1.0f - rainLevel * 0.5f;
        float blueDamping = 1.0f - rainLevel * 0.4f;
        rgb[0] *= redGreenDamping;
        rgb[1] *= redGreenDamping;
        rgb[2] *= blueDamping;
    }

    private static void applyThunderDamping(float[] rgb, float thunderLevel) {
        if (thunderLevel <= 0.0f) {
            return;
        }

        float damping = 1.0f - thunderLevel * 0.5f;
        rgb[0] *= damping;
        rgb[1] *= damping;
        rgb[2] *= damping;
    }
}
