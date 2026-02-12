package wily.legacy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
//? if <1.21.11 {
/*
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
*///?} else {
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import wily.legacy.mixin.base.RenderSetupAccessor;
import wily.legacy.mixin.base.TextureBindingAccessor;
import java.util.Map;
//?}
import net.minecraft.client.renderer.Sheets;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.mixin.base.client.BufferSourceAccessor;
import wily.legacy.mixin.base.client.CompositeRenderTypeAccessor;

import java.util.function.Function;

public class BufferSourceWrapper extends MultiBufferSource.BufferSource {
    public final MultiBufferSource.BufferSource source;
    private RenderType overrideRenderType;
    private Function<VertexConsumer, VertexConsumer> vertexConsumerFunction = Function.identity();

    public BufferSourceWrapper(MultiBufferSource.BufferSource source) {
        super(((BufferSourceAccessor) source).buffer(), ((BufferSourceAccessor) source).fixedBuffers());
        this.source = source;
    }

    //? if <1.21.11 {
    /*
    public static BufferSourceWrapper translucent(BufferSource source, float opacity) {
        return new BufferSourceWrapper(source) {
            @Override
            public VertexConsumer getBuffer(RenderType renderType) {
                if (renderType == Sheets.cutoutBlockSheet()) return super.getBuffer(Sheets.translucentItemSheet());
                else if (renderType.format() == DefaultVertexFormat.NEW_ENTITY && renderType instanceof RenderType.CompositeRenderType r && ((CompositeRenderTypeAccessor) (Object) r).getState().textureState instanceof RenderStateShard.TextureStateShard s && s.texture.isPresent())
                    return super.getBuffer(RenderType.itemEntityTranslucentCull(s.texture.get()));
                return super.getBuffer(renderType);
            }
        }.setVertexConsumerFunction(consumer -> new VertexConsumerWrapper(consumer).setColorMultiplier(ColorUtil.withAlpha(0xFFFFFF, opacity)));
    }
    *///?} else {
    public static BufferSourceWrapper translucent(BufferSource source, float opacity) {
        return new BufferSourceWrapper(source) {
            @Override
            public VertexConsumer getBuffer(RenderType renderType) {
                if (renderType == Sheets.cutoutBlockSheet()) return super.getBuffer(Sheets.translucentItemSheet());
                else if (renderType.format() == DefaultVertexFormat.NEW_ENTITY && renderType instanceof RenderType r) {
                    RenderSetup setup = ((CompositeRenderTypeAccessor) (Object) r).getState();
                    Map<String, ?> textures = ((RenderSetupAccessor) (Object) setup).getTexturesRaw();
                    if (textures.containsKey("Sampler0")) {
                        Object textureBinding = textures.get("Sampler0");
                        Identifier textureLocation = ((TextureBindingAccessor) textureBinding).getLocation();
                        return super.getBuffer(RenderTypes.itemEntityTranslucentCull(textureLocation));
                    }
                }
                return super.getBuffer(renderType);
            }
        }.setVertexConsumerFunction(consumer -> new VertexConsumerWrapper(consumer).setColorMultiplier(ColorUtil.withAlpha(0xFFFFFF, opacity)));
    }
    //?}

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
