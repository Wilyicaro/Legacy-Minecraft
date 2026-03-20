package wily.legacy.client;

import com.google.common.base.Charsets;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.util.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MapIdValueManager<T extends IdValueInfo<T>, M extends Map<ResourceLocation, T>>(ResourceLocation name,
                                                                                              Codec<List<T>> codec,
                                                                                              M map,
                                                                                              boolean removeInvalid) implements ResourceManagerReloadListener {
    public static boolean DEBUG = false;

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

            if (DEBUG) {
                new File(Minecraft.getInstance().gameDirectory, "debug_map_id_values").mkdirs();
                try (JsonWriter w = new JsonWriter(Files.newBufferedWriter(Minecraft.getInstance().gameDirectory.toPath().resolve("debug_map_id_values/" + name().getPath() + ".json"), Charsets.UTF_8))) {
                    w.setSerializeNulls(false);
                    w.setIndent("  ");
                    GsonHelper.writeValue(w, codec.encodeStart(JsonOps.INSTANCE, List.copyOf(map.values())).resultOrPartial(error -> Legacy4J.LOGGER.warn("Failed to write {}: {}", getName(), error)).orElseThrow(), null);
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            }
        }));
    }
}
