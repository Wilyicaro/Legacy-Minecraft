package wily.legacy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class BufferSourceWrapper extends MultiBufferSource.BufferSource {
    public final MultiBufferSource.BufferSource source;
    private RenderType overrideRenderTpe;

    public BufferSourceWrapper(MultiBufferSource.BufferSource source){
        super(source.sharedBuffer,source.fixedBuffers);
        this.source = source;
    }
    public static BufferSourceWrapper translucent(BufferSource source){
        return new BufferSourceWrapper(source){
            @Override
            public VertexConsumer getBuffer(RenderType renderType) {
                if (renderType == RenderType.cutout() || renderType == RenderType.solid() || renderType == RenderType.cutoutMipped()) return super.getBuffer(RenderType.translucent());
                if (renderType.format() == DefaultVertexFormat.NEW_ENTITY && renderType instanceof RenderType.CompositeRenderType r && r.state().textureState instanceof RenderStateShard.TextureStateShard s && s.texture.isPresent()) return super.getBuffer(RenderType.entityTranslucentCull(s.texture.get()));
                return super.getBuffer(renderType);
            }
        };
    }
    public static BufferSourceWrapper of(BufferSource source, RenderType overrideType){
        BufferSourceWrapper wrapper = new BufferSourceWrapper(source);
        wrapper.setOverrideRenderType(overrideType);
        return wrapper;
    }

    @Override
    public void endLastBatch() {
        source.endLastBatch();
    }

    @Override
    public void endBatch() {
        source.endBatch();
    }

    @Override
    public void endBatch(RenderType renderType) {
        source.endBatch(renderType);
    }

    public void setOverrideRenderType(RenderType overrideRenderTpe) {
        this.overrideRenderTpe = overrideRenderTpe;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return source.getBuffer(overrideRenderTpe == null ? renderType : overrideRenderTpe);
    }
}
