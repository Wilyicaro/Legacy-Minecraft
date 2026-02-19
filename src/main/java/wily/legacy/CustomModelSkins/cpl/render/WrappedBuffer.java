package wily.legacy.CustomModelSkins.cpl.render;

public class WrappedBuffer implements VertexBuffer {
    protected final VertexBuffer buffer;

    public WrappedBuffer(VertexBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public VertexBuffer pos(float x, float y, float z) {
        buffer.pos(x, y, z);
        return this;
    }

    @Override
    public VertexBuffer tex(float u, float v) {
        buffer.tex(u, v);
        return this;
    }

    @Override
    public VertexBuffer color(float red, float green, float blue, float alpha) {
        buffer.color(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexBuffer normal(float x, float y, float z) {
        buffer.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        buffer.endVertex();
    }

    @Override
    public void finish() {
        buffer.finish();
    }
}
