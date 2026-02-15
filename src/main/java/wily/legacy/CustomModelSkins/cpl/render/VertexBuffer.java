package wily.legacy.CustomModelSkins.cpl.render;

import wily.legacy.CustomModelSkins.cpl.math.*;

public interface VertexBuffer {
    VertexBuffer pos(float x, float y, float z);

    VertexBuffer tex(float u, float v);

    VertexBuffer color(float red, float green, float blue, float alpha);

    VertexBuffer normal(float x, float y, float z);

    void endVertex();

    default void addVertex(float x, float y, float z, float red, float green, float blue, float alpha, float texU, float texV, float normalX, float normalY, float normalZ) {
        pos(x, y, z);
        color(red, green, blue, alpha);
        tex(texU, texV);
        normal(normalX, normalY, normalZ);
        endVertex();
    }

    default void addVertex(MatrixStack.Entry mat, float x, float y, float z, float red, float green, float blue, float alpha, float texU, float texV, float normalX, float normalY, float normalZ) {
        pos(mat.getMatrix(), x, y, z);
        color(red, green, blue, alpha);
        tex(texU, texV);
        normal(mat.getNormal(), normalX, normalY, normalZ);
        endVertex();
    }

    default VertexBuffer pos(Mat4f m, float x, float y, float z) {
        Vec4f v = new Vec4f(x, y, z, 1F);
        v.transform(m);
        return pos(v.x, v.y, v.z);
    }

    default VertexBuffer normal(Mat3f m, float x, float y, float z) {
        Vec3f v = new Vec3f(x, y, z);
        v.transform(m);
        return normal(v.x, v.y, v.z);
    }

    void finish();

    VertexBuffer NULL = new VertexBuffer() {
        public VertexBuffer pos(float x, float y, float z) {
            return this;
        }

        public VertexBuffer tex(float u, float v) {
            return this;
        }

        public VertexBuffer color(float red, float green, float blue, float alpha) {
            return this;
        }

        public VertexBuffer normal(float x, float y, float z) {
            return this;
        }

        public void endVertex() {
        }

        public void finish() {
        }
    };
}
