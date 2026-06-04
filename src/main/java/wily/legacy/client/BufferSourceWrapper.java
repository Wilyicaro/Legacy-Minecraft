package wily.legacy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.mixin.base.client.BufferSourceAccessor;
import wily.legacy.mixin.base.client.RenderSetupAccessor;
import wily.legacy.mixin.base.client.RenderTypeAccessor;

import java.util.function.Function;

public class BufferSourceWrapper extends MultiBufferSource.BufferSource {
    public final MultiBufferSource.BufferSource source;
    private RenderType overrideRenderType;
    private Function<VertexConsumer, VertexConsumer> vertexConsumerFunction = Function.identity();

    public BufferSourceWrapper(MultiBufferSource.BufferSource source) {
        super(((BufferSourceAccessor) source).buffer(), ((BufferSourceAccessor) source).fixedBuffers());
        this.source = source;
    }

    public static BufferSourceWrapper translucent(BufferSource source, float opacity) {
        return new BufferSourceWrapper(source) {
            @Override
            public VertexConsumer getBuffer(RenderType renderType) {
                if (renderType == Sheets.cutoutBlockSheet()) return super.getBuffer(Sheets.translucentBlockItemSheet());
                else if (renderType.format() == DefaultVertexFormat./*? if >=26.1 {*/ENTITY/*?} else {*//*NEW_ENTITY*//*?}*/ && !((RenderSetupAccessor)(Object) ((RenderTypeAccessor) renderType).getState()).getTextureBindings().isEmpty())
                    return super.getBuffer(
                            RenderTypes./*? if >=26.1 {*/entityTranslucentCullItemTarget/*?} else {*//*itemEntityTranslucentCull*//*?}*/(
                                    ((RenderSetupAccessor)(Object) ((RenderTypeAccessor) renderType).getState()).getTextureBindings().get("Sampler0").location()));
                return super.getBuffer(renderType);
            }
        }.setVertexConsumerFunction(consumer -> new VertexConsumerWrapper(consumer).setColorMultiplier(ColorUtil.withAlpha(0xFFFFFF, opacity)));
    }

    public static BufferSourceWrapper of(BufferSource source, RenderType overrideType) {
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

    public BufferSourceWrapper setOverrideRenderType(RenderType overrideRenderTpe) {
        this.overrideRenderType = overrideRenderTpe;
        return this;
    }

    public BufferSourceWrapper setVertexConsumerFunction(Function<VertexConsumer, VertexConsumer> vertexConsumerFunction) {
        this.vertexConsumerFunction = vertexConsumerFunction;
        return this;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return vertexConsumerFunction.apply(source.getBuffer(overrideRenderType == null ? renderType : overrideRenderType));
    }
}
