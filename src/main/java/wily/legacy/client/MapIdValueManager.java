package wily.legacy.client;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.util.IOUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MapIdValueManager<T extends IdValueInfo<T>, M extends Map<ResourceLocation, T>>(ResourceLocation name,
                                                                                              Codec<List<T>> codec,
                                                                                              M map,
                                                                                              boolean removeInvalid) implements ResourceManagerReloadListener {
    public static <T extends IdValueInfo<T>> MapIdValueManager<T, LinkedHashMap<ResourceLocation, T>> createWithListCodec(ResourceLocation name, Codec<List<T>> codec) {
        return new MapIdValueManager<>(name, codec, new LinkedHashMap<>(), false);
    }

    public static <T extends IdValueInfo<T>> MapIdValueManager<T, LinkedHashMap<ResourceLocation, T>> create(ResourceLocation name, Codec<T> codec) {
        return createWithListCodec(name, codec.listOf());
    }

    public static <T extends IdValueInfo<T>> MapIdValueManager<T, ListMap<ResourceLocation, T>> createListMap(ResourceLocation name, Codec<T> codec) {
        return new MapIdValueManager<>(name, codec.listOf(), new ListMap<>(), true);
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        map.clear();
        IOUtil.getOrderedNamespaces(manager).forEach(name -> manager.getResource(FactoryAPI.createLocation(name, name().getPath() + ".json")).ifPresent(r -> {
            try (BufferedReader bufferedReader = r.openAsReader()) {
                codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader)).resultOrPartial(error -> Legacy4J.LOGGER.warn("Failed to parse {}: {}", getName(), error)).ifPresent(listings -> {
                    for (T listing : listings) {
                        map.put(listing.id(), map.containsKey(listing.id()) ? map.get(listing.id()).copyFrom(listing) : listing);
                    }
                });
            } catch (IOException exception) {
                Legacy4J.LOGGER.warn(exception.getMessage());
            }
        }));
    }
}
