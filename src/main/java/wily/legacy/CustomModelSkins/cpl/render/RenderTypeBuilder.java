package wily.legacy.CustomModelSkins.cpl.render;

import wily.legacy.CustomModelSkins.cpl.render.VBuffers.NativeRenderType;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.RenderMode;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

public class RenderTypeBuilder<RL, RT> {
    private Map<RenderMode, Function<RL, RT>> modeFactories = new EnumMap<>(RenderMode.class);
    private Map<RenderMode, Integer> renderLayers = new EnumMap<>(RenderMode.class);
    private Supplier<RL> getDynamic;

    public void build(RenderTypes<RenderMode> renderTypes, TextureHandler<RL, RT> handler) {
        renderTypes.put(RenderMode.NORMAL, new NativeRenderType(0));
        for (Entry<RenderMode, Function<RL, RT>> e : modeFactories.entrySet()) {
            int layer = renderLayers.get(e.getKey());
            renderTypes.put(e.getKey(), new NativeRenderType(e.getValue().apply(handler.getTexture()), layer));
        }
        renderTypes.put(RenderMode.DEFAULT, new NativeRenderType(handler.getRenderType(), 0));
    }

    public static interface TextureHandler<RL, RT> {
        RL getTexture();

        void setTexture(RL texture);

        RT getRenderType();
    }

    public RenderTypeBuilder<RL, RT> register(RenderMode mode, Function<RL, RT> factory, int layer) {
        if (mode == RenderMode.NORMAL) throw new IllegalArgumentException("Can't init built-in layer");
        modeFactories.put(mode, factory);
        renderLayers.put(mode, layer);
        return this;
    }
}
