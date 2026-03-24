package wily.legacy.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import wily.legacy.Legacy4JClient;

public final class LegacyCloudAtmosphere {
    private static final float TWO_PI = 6.2831855f;
    private static final float SUNRISE_SPAN = 0.4f;
    private static final int SUNRISE_DARK = 0xFFB23333;
    private static final int SUNRISE_BRIGHT = 0xFFFFFF7A;
    private static final int DEFAULT_FOG_COLOR = 0xFFC0D8FF;
    private static final float SUNRISE_CLOUD_TINT_STRENGTH = 1.25f;
    private static final float SUNRISE_FOG_TINT_STRENGTH = 1.35f;

    private LegacyCloudAtmosphere() {
    }

    public static boolean areLceCloudsEnabled() {
        return LegacyOptions.lceClouds.get();
    }

    public static boolean areLegacyCloudHeightAndTextureEnabled() {
        return areLceCloudsEnabled() && LegacyOptions.legacyCloudHeightAndTexture.get();
    }

    public static int getCloudDrawDistanceBlocks() {
        return Math.max(64, (Legacy4JClient.getEffectiveRenderDistance() + 16) * 16);
    }

    public static float getCloudFogEndBlocks(float environmentalEnd) {
        return Math.max(environmentalEnd, getCloudDrawDistanceBlocks());
    }

    public static float getSunriseFogThicknessBlend(ClientLevel level, float partialTick) {
        if (!shouldUseConsoleAtmosphere(level)) {
            return 0.0f;
        }

        int sunriseColor = getSunriseColor(level.getTimeOfDay(partialTick));
        if (sunriseColor == 0) {
            return 0.0f;
        }

        return Mth.clamp(Mth.sqrt(ARGB.alphaFloat(sunriseColor)) * 0.85f, 0.0f, 1.0f);
    }

    public static boolean shouldUseConsoleAtmosphere(ClientLevel level) {
        return areLceCloudsEnabled() && level.effects() instanceof DimensionSpecialEffects.OverworldEffects;
    }

    public static boolean shouldUseWarmCloudTransparency(ClientLevel level, float partialTick) {
        if (!shouldUseConsoleAtmosphere(level) || areLegacyCloudHeightAndTextureEnabled()) {
            return false;
        }

        return getSunriseCloudBlend(level, partialTick) > 0.02f;
    }

    public static int getSkyColor(ClientLevel level, Vec3 position, float partialTick) {
        float brightness = getDayBrightness(level.getTimeOfDay(partialTick));
        BlockPos blockPos = BlockPos.containing(position.x, position.y, position.z);
        Biome biome = level.getBiome(blockPos).value();
        int skyColor = biome.getSkyColor();
        float red = ARGB.redFloat(skyColor) * brightness;
        float green = ARGB.greenFloat(skyColor) * brightness;
        float blue = ARGB.blueFloat(skyColor) * brightness;

        float rainLevel = level.getRainLevel(partialTick);
        if (rainLevel > 0.0f) {
            float greyscale = (red * 0.30f + green * 0.59f + blue * 0.11f) * 0.6f;
            float balance = 1.0f - rainLevel * 0.75f;
            red = red * balance + greyscale * (1.0f - balance);
            green = green * balance + greyscale * (1.0f - balance);
            blue = blue * balance + greyscale * (1.0f - balance);
        }

        float thunderLevel = level.getThunderLevel(partialTick);
        if (thunderLevel > 0.0f) {
            float greyscale = (red * 0.30f + green * 0.59f + blue * 0.11f) * 0.2f;
            float balance = 1.0f - thunderLevel * 0.75f;
            red = red * balance + greyscale * (1.0f - balance);
            green = green * balance + greyscale * (1.0f - balance);
            blue = blue * balance + greyscale * (1.0f - balance);
        }

        int flashTime = level.getSkyFlashTime();
        if (flashTime > 0) {
            float flash = Mth.clamp(flashTime - partialTick, 0.0f, 1.0f) * 0.45f;
            red = red * (1.0f - flash) + 0.8f * flash;
            green = green * (1.0f - flash) + 0.8f * flash;
            blue = blue * (1.0f - flash) + flash;
        }

        return ARGB.colorFromFloat(1.0f, red, green, blue);
    }

    public static int getAtmosphericFogColor(ClientLevel level, Camera camera, int renderDistanceChunks, float partialTick) {
        int fogColor = getDimensionFogColor(level, partialTick);
        float red = ARGB.redFloat(fogColor);
        float green = ARGB.greenFloat(fogColor);
        float blue = ARGB.blueFloat(fogColor);

        if (renderDistanceChunks >= 4) {
            int sunriseColor = getSunriseColor(level.getTimeOfDay(partialTick));
            if (sunriseColor != 0) {
                float blend = getSunriseFogBlend(level, camera, partialTick, sunriseColor);
                if (blend > 0.0f) {
                    red = Mth.lerp(blend, red, ARGB.redFloat(sunriseColor));
                    green = Mth.lerp(blend, green, ARGB.greenFloat(sunriseColor));
                    blue = Mth.lerp(blend, blue, ARGB.blueFloat(sunriseColor));
                }
            }
        }

        int skyColor = getSkyColor(level, camera.getPosition(), partialTick);
        float skyBlend = getFogToSkyBlendFactor(renderDistanceChunks);
        red += (ARGB.redFloat(skyColor) - red) * skyBlend;
        green += (ARGB.greenFloat(skyColor) - green) * skyBlend;
        blue += (ARGB.blueFloat(skyColor) - blue) * skyBlend;

        float rainLevel = level.getRainLevel(partialTick);
        if (rainLevel > 0.0f) {
            float redGreenDamping = 1.0f - rainLevel * 0.5f;
            float blueDamping = 1.0f - rainLevel * 0.4f;
            red *= redGreenDamping;
            green *= redGreenDamping;
            blue *= blueDamping;
        }

        float thunderLevel = level.getThunderLevel(partialTick);
        if (thunderLevel > 0.0f) {
            float damping = 1.0f - thunderLevel * 0.5f;
            red *= damping;
            green *= damping;
            blue *= damping;
        }

        return ARGB.colorFromFloat(1.0f, red, green, blue);
    }

    public static int getSunriseCloudColor(ClientLevel level, float partialTick, int baseCloudColor) {
        if (!shouldUseConsoleAtmosphere(level)) {
            return baseCloudColor;
        }

        int sunriseColor = getSunriseColor(level.getTimeOfDay(partialTick));
        if (sunriseColor == 0) {
            return baseCloudColor;
        }

        float blend = getSunriseCloudBlend(level, partialTick);
        if (blend <= 0.0f) {
            return baseCloudColor;
        }

        float tintBlend = Mth.clamp(blend * SUNRISE_CLOUD_TINT_STRENGTH, 0.0f, 1.0f);
        float red = Mth.lerp(tintBlend, ARGB.redFloat(baseCloudColor), ARGB.redFloat(sunriseColor));
        float green = Mth.lerp(tintBlend, ARGB.greenFloat(baseCloudColor), ARGB.greenFloat(sunriseColor));
        float blue = Mth.lerp(tintBlend, ARGB.blueFloat(baseCloudColor), ARGB.blueFloat(sunriseColor));
        return ARGB.colorFromFloat(1.0f, red, green, blue);
    }

    public static int getSunriseColor(float timeOfDay) {
        float cosine = Mth.cos(timeOfDay * TWO_PI);
        if (cosine < -SUNRISE_SPAN || cosine > SUNRISE_SPAN) {
            return 0;
        }

        float gradient = cosine / SUNRISE_SPAN * 0.5f + 0.5f;
        float alpha = Mth.square(1.0f - (1.0f - Mth.sin(gradient * Mth.PI)) * 0.99f);
        float red = Mth.lerp(gradient, ARGB.redFloat(SUNRISE_DARK), ARGB.redFloat(SUNRISE_BRIGHT));
        float green = Mth.lerp(gradient, ARGB.greenFloat(SUNRISE_DARK), ARGB.greenFloat(SUNRISE_BRIGHT));
        float blue = Mth.lerp(gradient, ARGB.blueFloat(SUNRISE_DARK), ARGB.blueFloat(SUNRISE_BRIGHT));
        return ARGB.colorFromFloat(alpha, red, green, blue);
    }

    private static int getDimensionFogColor(ClientLevel level, float partialTick) {
        float brightness = getDayBrightness(level.getTimeOfDay(partialTick));
        float red = ARGB.redFloat(DEFAULT_FOG_COLOR) * (brightness * 0.94f + 0.06f);
        float green = ARGB.greenFloat(DEFAULT_FOG_COLOR) * (brightness * 0.94f + 0.06f);
        float blue = ARGB.blueFloat(DEFAULT_FOG_COLOR) * (brightness * 0.91f + 0.09f);
        return ARGB.colorFromFloat(1.0f, red, green, blue);
    }

    private static float getDayBrightness(float timeOfDay) {
        return Mth.clamp(Mth.cos(timeOfDay * TWO_PI) * 2.0f + 0.5f, 0.0f, 1.0f);
    }

    private static float getFogToSkyBlendFactor(int renderDistanceChunks) {
        float normalizedDistance = 0.25f + 0.75f * Mth.clamp(renderDistanceChunks, 4, 32) / 32.0f;
        return 1.0f - (float) Math.pow(normalizedDistance, 0.25d);
    }

    private static float getSunriseCloudBlend(ClientLevel level, float partialTick) {
        int sunriseColor = getSunriseColor(level.getTimeOfDay(partialTick));
        if (sunriseColor == 0) {
            return 0.0f;
        }

        float timeWeight = Mth.clamp(Mth.sqrt(ARGB.alphaFloat(sunriseColor)) * 0.95f, 0.0f, 1.0f);
        float directionalWeight = Mth.sqrt(getSunriseViewBlend(level, partialTick));
        return Mth.clamp(timeWeight * (0.32f + directionalWeight * 0.68f), 0.0f, 1.0f);
    }

    private static float getSunriseViewBlend(ClientLevel level, float partialTick) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null) {
            return 0.0f;
        }

        Vector3f look = camera.getLookVector();
        double horizontalLength = Math.sqrt(look.x() * look.x() + look.z() * look.z());
        if (horizontalLength < 1.0e-4d) {
            return 0.0f;
        }

        float sunriseDirection = Mth.sin(level.getSunAngle(partialTick)) > 0.0f ? -1.0f : 1.0f;
        float horizontalFacing = Mth.clamp((float) ((look.x() / horizontalLength) * sunriseDirection), 0.0f, 1.0f);
        float horizonWeight = Mth.clamp((float) (1.0d - Math.abs(look.y()) * 1.35d), 0.0f, 1.0f);
        return Mth.square(horizontalFacing) * horizonWeight;
    }

    private static float getSunriseFogBlend(ClientLevel level, Camera camera, float partialTick, int sunriseColor) {
        float timeWeight = Mth.sqrt(ARGB.alphaFloat(sunriseColor));
        return Mth.clamp(timeWeight * SUNRISE_FOG_TINT_STRENGTH, 0.0f, 1.0f);
    }
}
