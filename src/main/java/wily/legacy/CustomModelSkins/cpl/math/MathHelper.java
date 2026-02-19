package wily.legacy.CustomModelSkins.cpl.math;

public class MathHelper {
    public static final double LOG2 = Math.log(2);
    public static final float PI = (float) Math.PI;

    public static float clamp(float num, float min, float max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }

    public static int clamp(int num, int min, int max) {
        if (num < min) {
            return min;
        } else {
            return num > max ? max : num;
        }
    }

    public static int ceil(float value) {
        int i = (int) value;
        return value > i ? i + 1 : i;
    }

    public static float fastInvCubeRoot(float number) {
        int i = Float.floatToIntBits(number);
        i = 1419967116 - i / 3;
        float f = Float.intBitsToFloat(i);
        f = 0.6666667F * f + 1.0F / (3.0F * f * f * number);
        return 0.6666667F * f + 1.0F / (3.0F * f * f * number);
    }

    public static float cos(float f) {
        return (float) Math.cos(f);
    }

    public static float sin(float f) {
        return (float) Math.sin(f);
    }

    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    public static int floor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    public static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    public static float fma(float a, float b, float c) {
        return a * b + c;
    }
}
