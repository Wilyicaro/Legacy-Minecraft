package wily.legacy.util;

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.core.Registry;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponentPatch;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IOUtil {

    public static <T> Predicate<T> registryMatches(Registry<T> registry, JsonObject o) {
        String name = registry.key().location().getPath();
        if (!o.has(name) && !o.has(name + "s")) return t -> false;
        List<T> tip = new ArrayList<>();
        List<T> tipExclusions = new ArrayList<>();
        List<TagKey<T>> tipTags = new ArrayList<>();

        if (o.has(name) && o.get(name) instanceof JsonPrimitive j && j.isString()) {
            String s = j.getAsString();
            if (s.startsWith("#"))
                tipTags.add(TagKey.create(registry.key(), FactoryAPI.createLocation(s.replaceFirst("#", ""))));
            else tip.add(FactoryAPIPlatform.getRegistryValue(FactoryAPI.createLocation(s), registry));
        }
        if (o.has(name + "s") && o.get(name + "s") instanceof JsonArray a) {
            a.forEach(ie -> {
                if (ie instanceof JsonPrimitive p && p.isString()) {
                    String s = p.getAsString();
                    if (s.startsWith("#"))
                        tipTags.add(TagKey.create(registry.key(), FactoryAPI.createLocation(s.replaceFirst("#", ""))));
                    else if (s.startsWith("!")) {
                        ResourceLocation l = FactoryAPI.createLocation(s.replaceFirst("!", ""));
                        registry.getOptional(l).ifPresent(tipExclusions::add);
                    } else tip.add(FactoryAPIPlatform.getRegistryValue(FactoryAPI.createLocation(s), registry));
                }
            });
        }
        return t -> !tipExclusions.contains(t) && (tip.contains(t) || tipTags.stream().anyMatch(registry./*? if <1.21.2 {*//*getHolderOrThrow*//*?} else {*/getOrThrow/*?}*/(registry.getResourceKey(t).orElseThrow())::is));
    }

    public static BiPredicate<Item, /*? if <1.20.5 {*/ /*CompoundTag*//*?} else {*/DataComponentPatch/*?}*/> registryMatchesItem(JsonObject o) {
        //? if <1.20.5 {
        /*CompoundTag data = o.has("nbt") ? CompoundTag.CODEC.parse(JsonOps.INSTANCE,o.get("nbt")).result().orElse(null) : null;
         *///?} else {
        DataComponentPatch data = o.has("components") ? DataComponentPatch.CODEC.parse(DynamicUtil.getActualRegistryOps(JsonOps.INSTANCE), o.getAsJsonObject("components")).getOrThrow() : null;
        //?}
        Predicate<Item> p = registryMatches(BuiltInRegistries.ITEM, o);
        return (item, d) -> p.test(item) && (data == null || /*? if <1.20.5 {*//*NbtUtils.compareNbt(data,d,true)*//*?} else {*/Objects.equals(d, data)/*?}*/);
    }

    public static ArbitrarySupplier<ItemStack> getItemFromJson(JsonElement element, boolean allowData) {
        return DynamicUtil.getItemFromDynamic(new Dynamic<>(JsonOps.INSTANCE, element), allowData);
    }

    public static <K, V> void addMapListEntry(Map<K, List<V>> map, K key, V entry) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
    }

    public static <T> T getJsonStringOrNull(JsonObject object, String element, Function<String, T> constructor) {
        String s = GsonHelper.getAsString(object, element, null);
        return s == null ? null : constructor.apply(s);
    }

    public static <T> void ifJsonStringNotNull(JsonObject object, String element, Function<String, T> constructor, Consumer<T> consumer) {
        T obj = getJsonStringOrNull(object, element, constructor);
        if (obj != null) consumer.accept(obj);
    }

    public static Stream<String> getOrderedNamespaces(ResourceManager manager) {
        return manager.getNamespaces().stream().sorted(Comparator.comparingInt(s -> s.equals("legacy") ? 0 : 1));
    }

    public static <T> FallbackCodec<T> createFallbackCodec(Codec<T> main, Codec<T> fallback) {
        return new FallbackCodec<>(main, main, fallback);
    }

    public static <K, V> Codec<Map<K, V>> createIdMapCodec(Codec<K> keyCodec, Codec<V> codec, String keyField) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> dynamic = new Dynamic<>(ops, input);
                DataResult<Map<K, V>> defaultMap = dynamic.asMapOpt().map(s -> s.collect(Collectors.toMap(p -> keyCodec.parse(p.getFirst()).result().orElseThrow(), p -> codec.parse(p.getSecond().set(keyField, p.getFirst())).resultOrPartial(Legacy4J.LOGGER::error).get(), (a, b) -> b, LinkedHashMap::new)));
                return defaultMap.map(m -> Pair.of(m, input));
            }

            @Override
            public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T prefix) {
                return DataResult.success(ops.createMap(input.entrySet().stream().map(e -> Pair.of(keyCodec.encodeStart(ops, e.getKey()).getOrThrow(), codec.encodeStart(ops, e.getValue()).resultOrPartial(Legacy4J.LOGGER::error).get()))));
            }
        };
    }

    public static <V> Codec<List<V>> createListIdMapCodec(Codec<V> codec, String keyField) {
        Codec<List<V>> listCodec = codec.listOf();
        return createFallbackCodec(new Codec<>() {
            @Override
            public <T> DataResult<Pair<List<V>, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> dynamic = new Dynamic<>(ops, input);
                DataResult<List<V>> defaultMap = dynamic.asMapOpt().map(s -> s.map(p -> codec.parse(p.getSecond().set(keyField, p.getFirst())).resultOrPartial(Legacy4J.LOGGER::error).get()).toList());
                return defaultMap.map(m -> Pair.of(m, input));
            }

            @Override
            public <T> DataResult<T> encode(List<V> input, DynamicOps<T> ops, T prefix) {
                return listCodec.encode(input, ops, prefix);
            }
        }, listCodec);
    }

    public static <E> Codec<Map<String, E>> createListingCodec(Codec<E> codec, String keyField, String valueField, Function<E, String> fallBackKey) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<Map<String, E>, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> dynamic = new Dynamic<>(ops, input);
                DataResult<Map<String, E>> defaultMap = dynamic.asMapOpt().map(s -> s.collect(Collectors.toMap(p -> p.getFirst().asString().result().orElseThrow(), p -> codec.parse(p.getSecond()).result().orElseThrow(), (a, b) -> b, LinkedHashMap::new)));
                return (defaultMap.result().isPresent() ? defaultMap : dynamic.asListOpt(d -> {
                    E element = codec.parse(d).result().orElseGet(() -> d.get(valueField).flatMap(codec::parse).resultOrPartial(Legacy4J.LOGGER::error).get());
                    return Pair.of(d.get(keyField).asString().result().orElseGet(() -> fallBackKey.apply(element)), element);
                }).map(l -> l.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (a, b) -> b, LinkedHashMap::new)))).map(m -> Pair.of(m, input));
            }

            @Override
            public <T> DataResult<T> encode(Map<String, E> input, DynamicOps<T> ops, T prefix) {
                return DataResult.success(ops.createList(input.entrySet().stream().map(e -> ops.createMap(Map.of(ops.createString(keyField), ops.createString(e.getKey()), ops.createString(valueField), codec.encodeStart(ops, e.getValue()).resultOrPartial(Legacy4J.LOGGER::error).get())))));
            }
        };
    }

    public record FallbackCodec<T>(Encoder<T> encoder, Decoder<T> decoder, Codec<T> fallback) implements Codec<T> {

        @Override
        public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> dynamicOps, T1 t1) {
            DataResult<Pair<T, T1>> decoded = decoder.decode(dynamicOps, t1);
            return decoded.isError() ? fallback.decode(dynamicOps, t1) : decoded;
        }

        @Override
        public <T1> DataResult<T1> encode(T t, DynamicOps<T1> dynamicOps, T1 t1) {
            DataResult<T1> encoded = encoder.encode(t, dynamicOps, t1);
            return encoded.isError() ? fallback.encode(t, dynamicOps, t1) : encoded;
        }
    }
}
