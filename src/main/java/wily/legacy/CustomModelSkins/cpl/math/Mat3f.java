package wily.legacy.CustomModelSkins.cpl.math;

public class Mat3f {
    protected float m00, m01, m02, m10, m11, m12, m20, m21, m22;

    public Mat3f() {
    }

    public Mat3f(Quaternion q) {
        float x = q.getX(), y = q.getY(), z = q.getZ(), w = q.getW();
        float x2 = 2f * x * x, y2 = 2f * y * y, z2 = 2f * z * z;
        m00 = 1f - y2 - z2;
        m11 = 1f - z2 - x2;
        m22 = 1f - x2 - y2;
        float xy = x * y, yz = y * z, zx = z * x, xw = x * w, yw = y * w, zw = z * w;
        m10 = 2f * (xy + zw);
        m01 = 2f * (xy - zw);
        m20 = 2f * (zx - yw);
        m02 = 2f * (zx + yw);
        m21 = 2f * (yz + xw);
        m12 = 2f * (yz - xw);
    }

    public static Mat3f makeScaleMatrix(float x, float y, float z) {
        Mat3f m = new Mat3f();
        m.m00 = x;
        m.m11 = y;
        m.m22 = z;
        return m;
    }

    public Mat3f(Mat3f o) {
        m00 = o.m00;
        m01 = o.m01;
        m02 = o.m02;
        m10 = o.m10;
        m11 = o.m11;
        m12 = o.m12;
        m20 = o.m20;
        m21 = o.m21;
        m22 = o.m22;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Mat3f m && Float.compare(m.m00, m00) == 0 && Float.compare(m.m01, m01) == 0 && Float.compare(m.m02, m02) == 0 && Float.compare(m.m10, m10) == 0 && Float.compare(m.m11, m11) == 0 && Float.compare(m.m12, m12) == 0 && Float.compare(m.m20, m20) == 0 && Float.compare(m.m21, m21) == 0 && Float.compare(m.m22, m22) == 0);
    }

    private static int bits0(float v) {
        return v == 0f ? 0 : Float.floatToIntBits(v);
    }

    @Override
    public int hashCode() {
        int i = bits0(m00);
        i = 31 * i + bits0(m01);
        i = 31 * i + bits0(m02);
        i = 31 * i + bits0(m10);
        i = 31 * i + bits0(m11);
        i = 31 * i + bits0(m12);
        i = 31 * i + bits0(m20);
        i = 31 * i + bits0(m21);
        return 31 * i + bits0(m22);
    }

    @Override
    public String toString() {
        return "Matrix3f:\n" + m00 + " " + m01 + " " + m02 + "\n" + m10 + " " + m11 + " " + m12 + "\n" + m20 + " " + m21 + " " + m22 + "\n";
    }

    public void setIdentity() {
        m00 = m11 = m22 = 1f;
        m01 = m02 = m10 = m12 = m20 = m21 = 0f;
    }

    public void mul(Mat3f o) {
        float f = m00 * o.m00 + m01 * o.m10 + m02 * o.m20, f1 = m00 * o.m01 + m01 * o.m11 + m02 * o.m21, f2 = m00 * o.m02 + m01 * o.m12 + m02 * o.m22;
        float f3 = m10 * o.m00 + m11 * o.m10 + m12 * o.m20, f4 = m10 * o.m01 + m11 * o.m11 + m12 * o.m21, f5 = m10 * o.m02 + m11 * o.m12 + m12 * o.m22;
        float f6 = m20 * o.m00 + m21 * o.m10 + m22 * o.m20, f7 = m20 * o.m01 + m21 * o.m11 + m22 * o.m21, f8 = m20 * o.m02 + m21 * o.m12 + m22 * o.m22;
        m00 = f;
        m01 = f1;
        m02 = f2;
        m10 = f3;
        m11 = f4;
        m12 = f5;
        m20 = f6;
        m21 = f7;
        m22 = f8;
    }

    public void mul(Quaternion q) {
        mul(new Mat3f(q));
    }

    public void mul(float s) {
        m00 *= s;
        m01 *= s;
        m02 *= s;
        m10 *= s;
        m11 *= s;
        m12 *= s;
        m20 *= s;
        m21 *= s;
        m22 *= s;
    }

    public Mat3f copy() {
        return new Mat3f(this);
    }

    public float[] toArray() {
        return new float[]{m00, m01, m02, m10, m11, m12, m20, m21, m22};
    }

    public Mat3f invert(Mat3f dest) {
        float a = MathHelper.fma(m00, m11, -m01 * m10), b = MathHelper.fma(m02, m10, -m00 * m12), c = MathHelper.fma(m01, m12, -m02 * m11);
        float d = MathHelper.fma(a, m22, MathHelper.fma(b, m21, c * m20)), s = 1f / d;
        dest.m00 = MathHelper.fma(m11, m22, -m21 * m12) * s;
        dest.m01 = MathHelper.fma(m21, m02, -m01 * m22) * s;
        dest.m02 = c * s;
        dest.m10 = MathHelper.fma(m20, m12, -m10 * m22) * s;
        dest.m11 = MathHelper.fma(m00, m22, -m20 * m02) * s;
        dest.m12 = b * s;
        dest.m20 = MathHelper.fma(m10, m21, -m20 * m11) * s;
        dest.m21 = MathHelper.fma(m20, m01, -m00 * m21) * s;
        dest.m22 = a * s;
        return dest;
    }
}
