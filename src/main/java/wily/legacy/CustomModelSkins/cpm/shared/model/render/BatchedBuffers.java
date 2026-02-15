package wily.legacy.CustomModelSkins.cpm.shared.model.render;

import wily.legacy.CustomModelSkins.cpl.render.DirectBuffer;
import wily.legacy.CustomModelSkins.cpl.render.VBuffers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class BatchedBuffers<B, RT, V> {
    private List<VBuffers> buffers = new ArrayList<>();
    private ModelRenderManager.RedirectHolder<?, B, ?, ?> holder;
    private BiFunction<B, RT, V> getBuffer;

    public BatchedBuffers() {
        this.holder = null;
        this.getBuffer = null;
    }

    public BatchedBuffers(ModelRenderManager.RedirectHolder<?, B, ?, ?> holder, BiFunction<B, RT, V> getBuffer) {
        this.holder = holder;
        this.getBuffer = getBuffer;
    }

    public VBuffers nextBatch(Supplier<BufferOutput<V>> prepBuffer, V def) {
        BufferOutput<V> out = prepBuffer.get();
        Buffer<V> db = new Buffer<>(out, def);
        if (holder == null || getBuffer == null) return new VBuffers(rt -> db, db).replay();
        B bs = holder.addDt;
        VBuffers vb = new VBuffers(rt -> new Buffer<>(out, getBuffer.apply(bs, rt.getNativeType())), db).replay();
        buffers.add(vb);
        return vb;
    }

    public void flush() {
        buffers.forEach(VBuffers::finishAll);
        buffers.clear();
    }

    public static interface BufferOutput<V> {
        void push(V buf, float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, float nx, float ny, float nz);

        default void finish(V buf) {
        }
    }

    private static class Buffer<V> extends DirectBuffer<V> {
        private BufferOutput<V> out;

        public Buffer(BufferOutput<V> out, V buffer) {
            super(buffer);
            this.out = out;
        }

        @Override
        protected void pushVertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, float nx, float ny, float nz) {
            out.push(buffer, x, y, z, red, green, blue, alpha, u, v, nx, ny, nz);
        }

        @Override
        public void finish() {
            out.finish(buffer);
        }
    }
}
