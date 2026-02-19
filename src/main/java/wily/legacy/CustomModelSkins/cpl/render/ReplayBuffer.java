package wily.legacy.CustomModelSkins.cpl.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ReplayBuffer implements VertexBuffer {
    private final Supplier<VertexBuffer> target;
    private final List<float[]> verts = new ArrayList<>();
    private float x, y, z, u, v, r, g, b, a, nx, ny, nz;

    public ReplayBuffer(Supplier<VertexBuffer> target) {
        this.target = target;
    }

    @Override
    public VertexBuffer pos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public VertexBuffer tex(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public VertexBuffer color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    @Override
    public VertexBuffer normal(float x, float y, float z) {
        nx = x;
        ny = y;
        nz = z;
        return this;
    }

    @Override
    public void endVertex() {
        verts.add(new float[]{x, y, z, r, g, b, a, u, v, nx, ny, nz});
    }

    @Override
    public void finish() {
        VertexBuffer out = target.get();
        for (float[] f : verts) out.addVertex(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11]);
        verts.clear();
        out.finish();
    }
}
