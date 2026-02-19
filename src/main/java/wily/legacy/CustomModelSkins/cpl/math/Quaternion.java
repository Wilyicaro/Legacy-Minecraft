package wily.legacy.CustomModelSkins.cpl.math;

public class Quaternion {
    private float x, y, z, w;
    private Vec3f axis;
    private float angle;

    public Quaternion(Vec3f axis, float angle, boolean degrees) {
        if (degrees) angle *= ((float) Math.PI / 180F);
        float s = sin(angle * 0.5F);
        this.x = axis.x * s;
        this.y = axis.y * s;
        this.z = axis.z * s;
        this.w = cos(angle * 0.5F);
        this.axis = axis;
        this.angle = angle;
    }

    public Quaternion(float x, float y, float z, RotationOrder order) {
        this(x, y, z, order, true);
    }

    public Quaternion(float x, float y, float z, RotationOrder order, boolean degrees) {
        if (degrees) {
            float d = ((float) Math.PI / 180F);
            x *= d;
            y *= d;
            z *= d;
        }
        float l = cos(0.5F * x), c = cos(0.5F * y), h = cos(0.5F * z), u = sin(0.5F * x), d = sin(0.5F * y), p = sin(0.5F * z);
        switch (order) {
            case XYZ -> {
                this.x = u * c * h + l * d * p;
                this.y = l * d * h - u * c * p;
                this.z = l * c * p + u * d * h;
                this.w = l * c * h - u * d * p;
            }
            case ZXY -> {
                this.x = u * c * h - l * d * p;
                this.y = l * d * h + u * c * p;
                this.z = l * c * p + u * d * h;
                this.w = l * c * h - u * d * p;
            }
            case ZYX -> {
                this.x = u * c * h - l * d * p;
                this.y = l * d * h + u * c * p;
                this.z = l * c * p - u * d * h;
                this.w = l * c * h + u * d * p;
            }
            default -> {
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Quaternion q && Float.compare(q.x, x) == 0 && Float.compare(q.y, y) == 0 && Float.compare(q.z, z) == 0 && Float.compare(q.w, w) == 0);
    }

    @Override
    public int hashCode() {
        int i = Float.floatToIntBits(x);
        i = 31 * i + Float.floatToIntBits(y);
        i = 31 * i + Float.floatToIntBits(z);
        return 31 * i + Float.floatToIntBits(w);
    }

    @Override
    public String toString() {
        return "Quaternion[" + getW() + " + " + getX() + "i + " + getY() + "j + " + getZ() + "k]";
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getW() {
        return w;
    }

    private static float cos(float v) {
        return (float) Math.cos(v);
    }

    private static float sin(float v) {
        return (float) Math.sin(v);
    }

    @FunctionalInterface
    public static interface Qmap<T> {
        T apply(float x, float y, float z, float w);
    }

    private static float deg(float r) {
        return (float) Math.toDegrees(r);
    }

    public static Vec3f matrixToRotation(Mat4f m, RotationOrder to) {
        float a = m.m02, c = m.m12, d = m.m22, s = m.m01, r = m.m00, u = m.m21, l = m.m11, h = m.m20, o = m.m10;
        return switch (to) {
            case XYZ -> {
                float x, y, z;
                y = (float) Math.asin(MathHelper.clamp(a, -1, 1));
                if (Math.abs(a) < .9999999) {
                    x = (float) Math.atan2(-c, d);
                    z = (float) Math.atan2(-s, r);
                } else {
                    x = (float) Math.atan2(u, l);
                    z = 0;
                }
                yield new Vec3f(deg(x), deg(y), deg(z));
            }
            case ZXY -> {
                float x, y, z;
                x = (float) Math.asin(MathHelper.clamp(u, -1, 1));
                if (Math.abs(u) < .9999999) {
                    y = (float) Math.atan2(-h, d);
                    z = (float) Math.atan2(-s, l);
                } else {
                    z = (float) Math.atan2(o, r);
                    y = 0;
                }
                yield new Vec3f(deg(x), deg(y), deg(z));
            }
            case ZYX -> {
                float x, y, z;
                y = (float) Math.asin(-MathHelper.clamp(h, -1, 1));
                if (Math.abs(h) < .9999999) {
                    x = (float) Math.atan2(u, d);
                    z = (float) Math.atan2(o, r);
                } else {
                    z = (float) Math.atan2(-s, l);
                    x = 0;
                }
                yield new Vec3f(deg(x), deg(y), deg(z));
            }
            default -> new Vec3f();
        };
    }

    public static enum RotationOrder {ZYX, ZXY, XYZ}
}
