package wily.legacy.client;

import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.util.ListMap;

public class CommonValue<T> extends Stocker<T> {
    public static final ListMap<ResourceLocation, CommonValue<?>> COMMON_VALUES = new ListMap<>();

    public final T defaultValue;
    public CommonValue(T defaultValue) {
        super(defaultValue);
        this.defaultValue = defaultValue;
    }
    public void reset() {
        set(defaultValue);
    }

    public static final CommonValue<Boolean> WIDGET_TEXT_SHADOW = registerCommonValue("widget_text_shadow",true);
    public static final CommonValue<Float> LEGACY_FONT_DIM_FACTOR = registerCommonValue("legacy_font_dim_factor",0.0f);


    public static <T> CommonValue<T> registerCommonValue(String path, T defaultValue) {
        return registerCommonValue(FactoryAPI.createVanillaLocation(path),defaultValue);
    }
    public static <T> CommonValue<T> registerCommonValue(ResourceLocation id, T defaultValue) {
        CommonValue<T> value = new CommonValue<>(defaultValue);
        COMMON_VALUES.put(id, value);
        return value;
    }
}
