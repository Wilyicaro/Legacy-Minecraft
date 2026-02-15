package wily.legacy.CustomModelSkins.cpl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class VBuffers {
    private final Function<NativeRenderType, VertexBuffer> bufferFactory;
    private final Map<NativeRenderType, VertexBuffer> buffers = new HashMap<>();
    private VertexBuffer normalBuffer;
    private boolean batched;

    public VBuffers(Function<NativeRenderType, VertexBuffer> bufferFactory, VertexBuffer normalBuffer) {
        this.bufferFactory = bufferFactory;
        this.normalBuffer = normalBuffer;
    }

    public VBuffers(Function<NativeRenderType, VertexBuffer> bufferFactory) {
        this(bufferFactory, null);
    }

    public VertexBuffer getBuffer(NativeRenderType type) {
        if (type == null) return normalBuffer != null ? normalBuffer : VertexBuffer.NULL;
        if (type == NativeRenderType.DISABLE) return VertexBuffer.NULL;
        if (type.nativeType == null && normalBuffer != null) {
            buffers.put(type, normalBuffer);
            return new VBuf(normalBuffer, this, type);
        }
        return new VBuf(buffers.computeIfAbsent(type, bufferFactory), this, type);
    }

    public <E extends Enum<E>> VertexBuffer getBuffer(RenderTypes<E> types, E type) {
        return getBuffer(types.get(type));
    }

    public void finish(NativeRenderType type) {
        VertexBuffer buf = buffers.remove(type);
        if (buf != null) buf.finish();
    }

    public void finishAll() {
        var rts = new ArrayList<>(buffers.keySet());
        rts.sort((a, b) -> Integer.compare(a.layer, b.layer));
        for (var rt : rts) buffers.get(rt).finish();
        buffers.clear();
    }

    public static class NativeRenderType {
        public static final NativeRenderType DISABLE = new NativeRenderType(0);
        private final Object nativeType;
        private int layer;

        public NativeRenderType(Object nativeType, int layer) {
            this.nativeType = nativeType;
            this.layer = layer;
        }

        public NativeRenderType(int layer) {
            this(null, layer);
        }

        @SuppressWarnings("unchecked")
        public <RT> RT getNativeType() {
            return (RT) nativeType;
        }
    }

    public VBuffers replay() {
        return new VBuffers(rt -> new ReplayBuffer(() -> getBuffer(rt)), normalBuffer).setBatched(true);
    }

    private static class VBuf extends WrappedBuffer {
        private final VBuffers bufs;
        private final NativeRenderType rt;

        public VBuf(VertexBuffer buffer, VBuffers bufs, NativeRenderType rt) {
            super(buffer);
            this.bufs = bufs;
            this.rt = rt;
        }

        @Override
        public void finish() {
            bufs.finish(rt);
        }
    }

    public VBuffers map(UnaryOperator<VertexBuffer> map) {
        return new VBuffers(rt -> map.apply(getBuffer(rt)), normalBuffer != null ? map.apply(normalBuffer) : null).setBatched(batched);
    }

    public boolean isBatched() {
        return batched;
    }

    public VBuffers setBatched(boolean batched) {
        this.batched = batched;
        return this;
    }

    public static VBuffers record(BiConsumer<NativeRenderType, RecordBuffer> recordStarted) {
        return new VBuffers(rt -> {
            RecordBuffer buffer = new RecordBuffer();
            recordStarted.accept(rt, buffer);
            return buffer;
        }).setBatched(true);
    }
}
