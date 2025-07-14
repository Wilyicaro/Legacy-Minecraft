package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.util.ListMap;

public class CommonValue<T> extends Stocker<T> {
    public static final ListMap<ResourceLocation, CommonValue<?>> COMMON_VALUES = new ListMap<>();

    public final T defaultValue;
    public final Codec<T> codec;
    public CommonValue(T defaultValue, Codec<T> codec) {
        super(defaultValue);
        this.defaultValue = defaultValue;
        this.codec = codec;
    }
    public void reset() {
        set(defaultValue);
    }

    public static final CommonValue<Boolean> WIDGET_TEXT_SHADOW = registerCommonValue("widget_text_shadow",true, Codec.BOOL);
    public static final CommonValue<Float> LEGACY_FONT_DIM_FACTOR = registerCommonValue("legacy_font_dim_factor",0.0f, Codec.FLOAT);


    public void parse(Dynamic<?> dynamic){
        codec.parse(dynamic).result().ifPresent(this::set);
    }

    public static <T> CommonValue<T> registerCommonValue(String path, T defaultValue, Codec<T> codec) {
        return registerCommonValue(FactoryAPI.createVanillaLocation(path), defaultValue, codec);
    }

    public static <T> CommonValue<T> registerCommonValue(ResourceLocation id, T defaultValue, Codec<T> codec) {
        return registerCommonValue(id, new CommonValue<>(defaultValue, codec));
    }

    public static <T> CommonValue<T> registerCommonValue(ResourceLocation id, CommonValue<T> commonValue) {
        COMMON_VALUES.put(id, commonValue);
        return commonValue;
    }
}
