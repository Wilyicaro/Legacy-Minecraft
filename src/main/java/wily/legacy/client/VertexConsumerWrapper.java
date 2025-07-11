package wily.legacy.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import wily.factoryapi.util.ColorUtil;

public class VertexConsumerWrapper implements VertexConsumer {

    private final VertexConsumer consumer;
    private Integer colorMultiplier = null;

    public VertexConsumerWrapper(VertexConsumer consumer) {
        this.consumer = consumer;
    }

    public VertexConsumerWrapper setColorMultiplier(int colorMultiplier) {
        this.colorMultiplier = colorMultiplier;
        return this;
    }

    @Override
    public VertexConsumer addVertex(float f, float g, float h) {
        return consumer.addVertex(f, g, h);
    }

    @Override
    public VertexConsumer setColor(int i, int j, int k, int l) {
        return colorMultiplier == null ? consumer.setColor(i, j, k, l) : consumer.setColor((ColorUtil.getR(colorMultiplier) * i) / 255, (ColorUtil.getG(colorMultiplier) * j) / 255, (ColorUtil.getB(colorMultiplier) * k) / 255, (ColorUtil.getA(colorMultiplier) * l) / 255);
    }

    @Override
    public VertexConsumer setUv(float f, float g) {
        return consumer.setUv(f, g);
    }

    @Override
    public VertexConsumer setUv1(int i, int j) {
        return consumer.setUv1(i, j);
    }

    @Override
    public VertexConsumer setUv2(int i, int j) {
        return consumer.setUv2(i, j);
    }

    @Override
    public VertexConsumer setNormal(float f, float g, float h) {
        return consumer.setNormal(f, g, h);
    }
}
