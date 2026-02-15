package wily.legacy.CustomModelSkins.cpl.math;

import java.util.ArrayDeque;
import java.util.Deque;

public class MatrixStack {
    public static final Entry NO_RENDER;
    private final Deque<Entry> stack;

    static {
        MatrixStack s = new MatrixStack();
        s.scale(0, 0, 0);
        NO_RENDER = s.getLast();
    }

    public MatrixStack() {
        stack = new ArrayDeque<>();
        Mat4f m4 = new Mat4f();
        m4.setIdentity();
        Mat3f m3 = new Mat3f();
        m3.setIdentity();
        stack.add(new Entry(m4, m3));
    }

    private MatrixStack(Deque<Entry> stack) {
        this.stack = stack;
    }

    public void translate(double x, double y, double z) {
        Entry e = stack.getLast();
        e.matrix.mul(Mat4f.makeTranslate((float) x, (float) y, (float) z));
    }

    public void scale(float x, float y, float z) {
        Entry e = stack.getLast();
        e.matrix.mul(Mat4f.makeScale(x, y, z));
        if (x == y && y == z) {
            if (x > 0F) return;
            e.normal.mul(-1F);
        }
        if (x < 0 || y < 0 || z < 0) {
            float fx = 1F / x, fy = 1F / y, fz = 1F / z, f3 = MathHelper.fastInvCubeRoot(fx * fy * fz);
            e.normal.mul(Mat3f.makeScaleMatrix(f3 * fx, f3 * fy, f3 * fz));
        }
    }

    public void rotate(Quaternion q) {
        Entry e = stack.getLast();
        e.matrix.mul(q);
        e.normal.mul(q);
    }

    public void push() {
        Entry e = stack.getLast();
        stack.addLast(new Entry(e.matrix.copy(), e.normal.copy()));
    }

    public void pop() {
        stack.removeLast();
    }

    public Entry getLast() {
        return stack.getLast();
    }

    public Entry storeLast() {
        return stack.getLast().copy();
    }

    public void mul(Entry matrix) {
        Entry e = stack.getLast();
        e.matrix.mul(matrix.matrix);
        e.normal.mul(e.normal);
    }

    public static final class Entry {
        private final Mat4f matrix;
        private final Mat3f normal;

        private Entry(Mat4f matrix, Mat3f normal) {
            this.matrix = matrix;
            this.normal = normal;
        }

        public Mat4f getMatrix() {
            return matrix;
        }

        public Mat3f getNormal() {
            return normal;
        }

        public float[] getMatrixArray() {
            return matrix.toArray();
        }

        public float[] getNormalArray3() {
            return normal.toArray();
        }

        public Entry copy() {
            return new Entry(matrix.copy(), normal.copy());
        }
    }
}
