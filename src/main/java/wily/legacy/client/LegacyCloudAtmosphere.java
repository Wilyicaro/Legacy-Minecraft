package wily.legacy.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class LegacyCloudAtmosphere {
    private static final float TWO_PI = 6.2831855f;
    private static final float SUNRISE_SPAN = 0.4f;
    private static final int SUNRISE_DARK = 0xFFB23333;
    private static final int SUNRISE_BRIGHT = 0xFFFFE533;
    private static final float LEGACY_CLOUD_HEIGHT = 128.0f;
    private static final float CLOUD_WARM_INNER_TINT_STRENGTH = 0.22f;

    private LegacyCloudAtmosphere() {
    }

    public static boolean areLceCloudsEnabled() {
        return LegacyOptions.lceClouds.get();
    }

    public static boolean areLegacyCloudHeightAndTextureEnabled() {
        return areLceCloudsEnabled() && LegacyOptions.legacyCloudHeightAndTexture.get();
    }

    public static boolean shouldUseConsoleAtmosphere(DimensionSpecialEffects effects) {
        return areLceCloudsEnabled() && effects.skyType() == overworldSkyType();
    }

    public static boolean isSunriseOrSunset(float timeOfDay) {
        return getSunriseColor(timeOfDay) != 0;
    }

    public static float[] getSunriseColorFloats(float timeOfDay) {
        int color = getSunriseColor(timeOfDay);
        if (color == 0) return null;
        return new float[]{redFloat(color), greenFloat(color), blueFloat(color), alphaFloat(color)};
    }

    public static int getSunriseAndSunsetColor(ClientLevel level, float partialTick) {
        return getSunriseColor(getTimeOfDay(level, partialTick));
    }

    public static float getCloudHeight(float cloudHeight) {
        return areLegacyCloudHeightAndTextureEnabled() ? LEGACY_CLOUD_HEIGHT : cloudHeight;
    }

    public static Vec3 getWarmCloudColor(Vec3 color, float timeOfDay) {
        int sunriseColor = getSunriseColor(timeOfDay);
        if (sunriseColor == 0) return color;

        float blend = alphaFloat(sunriseColor) * CLOUD_WARM_INNER_TINT_STRENGTH;
        return new Vec3(
                Mth.lerp(blend, (float) color.x, redFloat(sunriseColor)),
                Mth.lerp(blend, (float) color.y, greenFloat(sunriseColor)),
                Mth.lerp(blend, (float) color.z, blueFloat(sunriseColor)));
    }

    public static int getWarmCloudColor(int color, float timeOfDay) {
        int sunriseColor = getSunriseColor(timeOfDay);
        if (sunriseColor == 0) return color;

        float blend = alphaFloat(sunriseColor) * CLOUD_WARM_INNER_TINT_STRENGTH;
        return colorFromFloat(
                alphaFloat(color),
                Mth.lerp(blend, redFloat(color), redFloat(sunriseColor)),
                Mth.lerp(blend, greenFloat(color), greenFloat(sunriseColor)),
                Mth.lerp(blend, blueFloat(color), blueFloat(sunriseColor)));
    }

    public static int getSunriseColor(float timeOfDay) {
        float cosine = Mth.cos(timeOfDay * TWO_PI);
        if (cosine < -SUNRISE_SPAN || cosine > SUNRISE_SPAN) return 0;

        float gradient = cosine / SUNRISE_SPAN * 0.5f + 0.5f;
        float alpha = Mth.square(1.0f - (1.0f - Mth.sin(gradient * Mth.PI)) * 0.99f);
        float red = Mth.lerp(gradient, redFloat(SUNRISE_DARK), redFloat(SUNRISE_BRIGHT));
        float green = Mth.lerp(gradient, greenFloat(SUNRISE_DARK), greenFloat(SUNRISE_BRIGHT));
        float blue = Mth.lerp(gradient, blueFloat(SUNRISE_DARK), blueFloat(SUNRISE_BRIGHT));
        return colorFromFloat(alpha, red, green, blue);
    }

    public static float getTimeOfDay(ClientLevel level, float partialTick) {
        double day = Mth.frac((level.getDayTime() + partialTick) / 24000.0d - 0.25d);
        double smoothedDay = 0.5d - Math.cos(day * Math.PI) / 2.0d;
        return (float) (day * 2.0d + smoothedDay) / 3.0f;
    }

    private static DimensionSpecialEffects.SkyType overworldSkyType() {
        return /*? if <1.21.2 {*/DimensionSpecialEffects.SkyType.NORMAL/*?} else {*//*DimensionSpecialEffects.SkyType.OVERWORLD*//*?}*/;
    }

    private static float alphaFloat(int color) {
        return (color >>> 24) / 255.0f;
    }

    private static float redFloat(int color) {
        return ((color >> 16) & 255) / 255.0f;
    }

    private static float greenFloat(int color) {
        return ((color >> 8) & 255) / 255.0f;
    }

    private static float blueFloat(int color) {
        return (color & 255) / 255.0f;
    }

    private static int colorFromFloat(float alpha, float red, float green, float blue) {
        return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, (int) (value * 255.0f)));
    }
}
