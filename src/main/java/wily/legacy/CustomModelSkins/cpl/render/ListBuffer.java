package wily.legacy.CustomModelSkins.cpl.render;

import wily.legacy.CustomModelSkins.cpl.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

public class ListBuffer implements VertexBuffer {
    private final List<Entry> toReplay = new ArrayList<>();
    private Entry entry = new Entry();

    @Override
    public VertexBuffer pos(float x, float y, float z) {
        entry.x = x;
        entry.y = y;
        entry.z = z;
        return this;
    }

    @Override
    public VertexBuffer color(float r, float g, float b, float a) {
        entry.red = r;
        entry.green = g;
        entry.blue = b;
        entry.alpha = a;
        return this;
    }

    @Override
    public VertexBuffer tex(float u, float v) {
        entry.u = u;
        entry.v = v;
        return this;
    }

    @Override
    public VertexBuffer normal(float x, float y, float z) {
        entry.nx = x;
        entry.ny = y;
        entry.nz = z;
        return this;
    }

    @Override
    public void endVertex() {
        toReplay.add(entry);
        entry = new Entry();
    }

    @Override
    public void finish() {
    }

    private static class Entry {
        private float x, y, z, red, green, blue, alpha, u, v, nx, ny, nz;

        void replay(MatrixStack s, VertexBuffer p) {
            p.addVertex(s.getLast(), x, y, z, red, green, blue, alpha, u, v, nx, ny, nz);
        }

        void replay(MatrixStack s, VertexBuffer p, float r, float g, float b, float a) {
            p.addVertex(s.getLast(), x, y, z, r, g, b, a, u, v, nx, ny, nz);
        }

        void replay(VertexBuffer p) {
            p.addVertex(x, y, z, red, green, blue, alpha, u, v, nx, ny, nz);
        }

        void replay(VertexBuffer p, float r, float g, float b, float a) {
            p.addVertex(x, y, z, r, g, b, a, u, v, nx, ny, nz);
        }
    }

    public void draw(MatrixStack stack, VertexBuffer buf, float r, float g, float b, float a) {
        for (Entry e : toReplay) e.replay(stack, buf, r, g, b, a);
    }
}
