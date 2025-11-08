package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import wily.legacy.util.IOUtil;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public record SizeableAsset<T>(Optional<T> fhd, Optional<T> hd, Optional<T> sd, T fallback) implements Supplier<T> {
    public SizeableAsset(T fallback) {
        this(Optional.empty(), Optional.empty(), Optional.empty(), fallback);
    }

    public static <T> Codec<SizeableAsset<T>> create(Codec<T> codec) {
        return RecordCodecBuilder.create(i -> i.group(codec.optionalFieldOf("fhd").forGetter(SizeableAsset::fhd), codec.optionalFieldOf("hd").forGetter(SizeableAsset::fhd), codec.optionalFieldOf("sd").forGetter(SizeableAsset::sd), codec.fieldOf("default").forGetter(SizeableAsset::fallback)).apply(i, SizeableAsset::new));
    }

    public static <T> Codec<SizeableAsset<T>> createWithFallback(Codec<T> codec) {
        return IOUtil.createFallbackCodec(codec.xmap(SizeableAsset::new, SizeableAsset::fallback), create(codec));
    }

    public <O> SizeableAsset<O> map(Function<T, O> mapper) {
        return new SizeableAsset<>(fhd.map(mapper), hd.map(mapper), sd.map(mapper), mapper.apply(fallback));
    }

    @Override
    public T get() {
        return switch (LegacyOptions.getUIMode()) {
            case FHD -> fhd.orElse(fallback);
            case HD -> hd.orElse(fallback);
            case SD -> sd.orElse(fallback);
            default -> fallback;
        };
    }
}
