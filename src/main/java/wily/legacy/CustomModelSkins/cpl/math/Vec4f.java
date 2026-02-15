package wily.legacy.CustomModelSkins.cpl.math;

public class Vec4f {
    public float x;
    public float y;
    public float z;
    public float w;

    public Vec4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void transform(Mat4f matrixIn) {
        float f = this.x;
        float f1 = this.y;
        float f2 = this.z;
        float f3 = this.w;
        this.x = matrixIn.m00 * f + matrixIn.m01 * f1 + matrixIn.m02 * f2 + matrixIn.m03 * f3;
        this.y = matrixIn.m10 * f + matrixIn.m11 * f1 + matrixIn.m12 * f2 + matrixIn.m13 * f3;
        this.z = matrixIn.m20 * f + matrixIn.m21 * f1 + matrixIn.m22 * f2 + matrixIn.m23 * f3;
        this.w = matrixIn.m30 * f + matrixIn.m31 * f1 + matrixIn.m32 * f2 + matrixIn.m33 * f3;
    }
}
