package wily.legacy.CustomModelSkins.cpl.render;

import wily.legacy.CustomModelSkins.cpl.render.VBuffers.NativeRenderType;

import java.util.EnumMap;

public class RenderTypes<E extends Enum<E>> {
    private EnumMap<E, NativeRenderType> types;

    public RenderTypes(Class<E> clazz) {
        types = new EnumMap<>(clazz);
    }

    public NativeRenderType get(E key) {
        return types.get(key);
    }

    public NativeRenderType put(E key, NativeRenderType value) {
        return types.put(key, value);
    }

    public void clear() {
        types.clear();
    }

    public void putAll(RenderTypes<E> cbi) {
    }
}
