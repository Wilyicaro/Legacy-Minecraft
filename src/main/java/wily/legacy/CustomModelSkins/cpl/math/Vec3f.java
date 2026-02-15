package wily.legacy.CustomModelSkins.cpl.math;

import java.util.HashMap;
import java.util.Map;

public class Vec3f {
    public static final Vec3f ZERO = new Vec3f(0.0F, 0.0F, 0.0F);
    public static final Vec3f NEGATIVE_X = new Vec3f(-1.0F, 0.0F, 0.0F);
    public static final Vec3f POSITIVE_X = new Vec3f(1.0F, 0.0F, 0.0F);
    public static final Vec3f NEGATIVE_Y = new Vec3f(0.0F, -1.0F, 0.0F);
    public static final Vec3f POSITIVE_Y = new Vec3f(0.0F, 1.0F, 0.0F);
    public static final Vec3f NEGATIVE_Z = new Vec3f(0.0F, 0.0F, -1.0F);
    public static final Vec3f POSITIVE_Z = new Vec3f(0.0F, 0.0F, 1.0F);
    public static final int MAX_POS = 3 * 16;
    public float x, y, z;

    public Vec3f() {
    }

    public Vec3f(float xyz) {
        this(xyz, xyz, xyz);
    }

    public Vec3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3f(Vec3f v) {
        this(v.x, v.y, v.z);
    }

    public Vec3f(Map<String, Object> m, Vec3f def) {
        this(def);
        if (m == null) return;
        x = ((Number) m.get("x")).floatValue();
        y = ((Number) m.get("y")).floatValue();
        z = ((Number) m.get("z")).floatValue();
    }

    public Vec3f add(Vec3f v) {
        return new Vec3f(x + v.x, y + v.y, z + v.z);
    }

    public Vec3f sub(float v) {
        return new Vec3f(x - v, y - v, z - v);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(3);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        return map;
    }

    @Override
    public String toString() {
        return String.format("Vec3[%.3f, %.3f, %.3f]", x, y, z);
    }

    private static int bits(float v) {
        return Float.floatToIntBits(v);
    }

    @Override
    public int hashCode() {
        int h = 31 + bits(x);
        h = 31 * h + bits(y);
        h = 31 * h + bits(z);
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vec3f)) return false;
        Vec3f o = (Vec3f) obj;
        return bits(x) == bits(o.x) && bits(y) == bits(o.y) && bits(z) == bits(o.z);
    }

    public void round(int i) {
        float s = 1.0F / i;
        x = Math.round(x * i) * s;
        y = Math.round(y * i) * s;
        z = Math.round(z * i) * s;
    }

    public void mul(float x, float y, float z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
    }

    public Vec3f copy() {
        return new Vec3f(this);
    }

    public void transform(Mat3f m) {
        float ox = x, oy = y, oz = z;
        x = m.m00 * ox + m.m01 * oy + m.m02 * oz;
        y = m.m10 * ox + m.m11 * oy + m.m12 * oz;
        z = m.m20 * ox + m.m21 * oy + m.m22 * oz;
    }

    public boolean epsilon(float e) {
        return Math.abs(x) < e && Math.abs(y) < e && Math.abs(z) < e;
    }
}
