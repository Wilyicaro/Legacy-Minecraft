package wily.legacy.CustomModelSkins.cpl.render;

public abstract class DirectBuffer<B> implements VertexBuffer {
    protected B buffer;
    private float x, y, z;
    private float red, green, blue, alpha;
    private float u, v;
    private float nx, ny, nz;

    public DirectBuffer(B buffer) {
        this.buffer = buffer;
    }

    @Override
    public VertexBuffer pos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public VertexBuffer color(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        return this;
    }

    @Override
    public VertexBuffer tex(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public VertexBuffer normal(float x, float y, float z) {
        this.nx = x;
        this.ny = y;
        this.nz = z;
        return this;
    }

    @Override
    public void endVertex() {
        pushVertex(x, y, z, red, green, blue, alpha, u, v, nx, ny, nz);
    }

    protected abstract void pushVertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, float nx, float ny, float nz);

    @Override
    public abstract void finish();
}
