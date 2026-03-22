package wily.legacy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import wily.legacy.mixin.base.client.RenderSetupAccessor;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import wily.legacy.mixin.base.client.CompositeRenderTypeAccessor;
*///?}
import net.minecraft.client.renderer.Sheets;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.mixin.base.client.BufferSourceAccessor;
import wily.legacy.mixin.base.client.RenderTypeMixin;

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
                //~ !identifier
                if (renderType == Sheets.cutoutBlockSheet()) return super.getBuffer(Sheets.translucentItemSheet());
                //? if >=1.21.11 {
                else if (renderType.format() == DefaultVertexFormat.NEW_ENTITY && !((RenderSetupAccessor)(Object) ((RenderTypeMixin) renderType).getState()).getTextureBindings().isEmpty())
                    return super.getBuffer(
                        RenderTypes.itemEntityTranslucentCull(
                            ((RenderSetupAccessor)(Object) ((RenderTypeMixin) renderType).getState()).getTextureBindings().values().stream().findFirst().get().location()));
                //?} else {
                /*else if (renderType.format() == DefaultVertexFormat.NEW_ENTITY && renderType instanceof RenderType.CompositeRenderType r && ((CompositeRenderTypeAccessor) (Object) r).getState().textureState instanceof RenderStateShard.TextureStateShard s && s.texture.isPresent())
                    return super.getBuffer(RenderType.itemEntityTranslucentCull(s.texture.get()));
                 *///?}
                return super.getBuffer(renderType);
                //~ identifier
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
