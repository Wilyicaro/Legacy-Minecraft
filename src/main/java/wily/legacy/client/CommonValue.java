package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.Identifier;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.util.ListMap;

public class CommonValue<T> extends Stocker<T> {
    public static final ListMap<Identifier, CommonValue<?>> COMMON_VALUES = new ListMap<>();
    public static final CommonValue<Boolean> WIDGET_TEXT_SHADOW = registerCommonValue("widget_text_shadow", true, Codec.BOOL);
    public static final CommonValue<Float> LEGACY_FONT_DIM_FACTOR = registerCommonValue("legacy_font_dim_factor", 0.0f, Codec.FLOAT);
    public static final CommonValue<Boolean> PS4_END_CRYSTAL_MODEL = registerCommonValue("ps4_end_crystal_model", false, Codec.BOOL);
    public final T defaultValue;
    public final Codec<T> codec;
    private boolean overridden;

    public CommonValue(T defaultValue, Codec<T> codec) {
        super(defaultValue);
        this.defaultValue = defaultValue;
        this.codec = codec;
    }

    public static <T> CommonValue<T> registerCommonValue(String path, T defaultValue, Codec<T> codec) {
        return registerCommonValue(FactoryAPI.createVanillaLocation(path), defaultValue, codec);
    }

    public static <T> CommonValue<T> registerCommonValue(Identifier id, T defaultValue, Codec<T> codec) {
        return registerCommonValue(id, new CommonValue<>(defaultValue, codec));
    }

    public static <T> CommonValue<T> registerCommonValue(Identifier id, CommonValue<T> commonValue) {
        COMMON_VALUES.put(id, commonValue);
        return commonValue;
    }

    public void reset() {
        set(defaultValue);
        overridden = false;
    }

    public void parse(Dynamic<?> dynamic) {
        codec.parse(dynamic).result().ifPresent(value -> {
            set(value);
            overridden = true;
        });
    }

    public boolean isOverridden() {
        return overridden;
    }

    public <V> V encode(DynamicOps<V> ops) {
        return codec.encodeStart(ops, get()).result().get();
    }
}
