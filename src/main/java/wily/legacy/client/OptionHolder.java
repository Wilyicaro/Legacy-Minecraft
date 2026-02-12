package wily.legacy.client;

import com.mojang.serialization.Codec;
import net.minecraft.resources.*;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.RegisterListing;

import java.util.function.Function;

public class OptionHolder<T> implements RegisterListing.Holder<T> {
    public static final OptionHolder<?> AUTO = new OptionHolder<>(null, r -> null) {
        @Override
        public String toString() {
            return "auto";
        }
    };
    public static final OptionHolder<?> NONE = new OptionHolder<>(null, r -> null) {
        @Override
        public String toString() {
            return "none";
        }
    };
    private final /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ id;
    private final Function</*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/, T> valueGetter;

    public OptionHolder(/*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ id, Function</*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/, T> valueGetter) {
        this.id = id;
        this.valueGetter = valueGetter;
    }

    public static <T> Codec<OptionHolder<T>> createCodecWithAuto(Function</*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/, T> valueGetter) {
        return Codec.STRING.xmap(value -> parseWithAuto(value, valueGetter), OptionHolder::toString);
    }

    public static <T> Codec<OptionHolder<T>> createCodec(Function</*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/, T> valueGetter) {
        return Codec.STRING.xmap(value -> parse(value, valueGetter), OptionHolder::toString);
    }

    public static <T> OptionHolder<T> parseWithAuto(String stringId, Function</*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/, T> valueGetter) {
        if (stringId.equals("auto")) return auto();
        /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ id = FactoryAPI.createLocation(stringId);
        return id == null ? auto() : new OptionHolder<>(id, valueGetter);
    }

    public static <T> OptionHolder<T> parse(String stringId, Function</*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/, T> valueGetter) {
        if (stringId.equals("none")) return none();
        /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ id = FactoryAPI.createLocation(stringId);
        return id == null ? none() : new OptionHolder<>(id, valueGetter);
    }

    public static <T> OptionHolder<T> auto() {
        return (OptionHolder<T>) AUTO;
    }

    public static <T> OptionHolder<T> none() {
        return (OptionHolder<T>) NONE;
    }

    public static <T extends IdValueInfo<T>> OptionHolder<T> of(T value) {
        return new OptionHolder<>(value.id(), r -> value);
    }

    public boolean isAuto() {
        return isNone();
    }

    public boolean isNone() {
        return get() == null;
    }

    public T orElse(T alternative) {
        T value = get();
        return value == null ? alternative : value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof OptionHolder<?> h && h.get() == get();
    }

    @Override
    public /*? if <1.21.11 {*//*ResourceLocation*//*?} else {*/Identifier/*?}*/ getId() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public T get() {
        return valueGetter.apply(id);
    }
}
