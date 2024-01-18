/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package wily.legacy.client;

import com.google.common.base.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.architectury.registry.registries.Registrar;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;
import wily.legacy.LegacyMinecraft;

@Environment(value=EnvType.CLIENT)
public class KnownListing<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path path;
    public final List<ResourceLocation> list = new ArrayList<>();
    private final Registrar<T> registrar;
    private final String listingFile;
    public KnownListing(ResourceKey<Registry<T>> registryKey, Path path){
        registrar = LegacyMinecraft.REGISTRIES.get().get(registryKey);

        listingFile = "known_"+ registrar.key().location().getPath()+ ".json";
        this.path = path.resolve(listingFile);
        if (Files.exists(path)) {
            try (BufferedReader bufferedReader = Files.newBufferedReader(this.path, Charsets.UTF_8);){
                JsonArray array = GsonHelper.parseArray(bufferedReader);
                array.forEach(e-> {
                    if (e instanceof JsonPrimitive p && p.isString()) list.add(new ResourceLocation(p.getAsString()));
                });
            } catch (Exception exception) {
                LOGGER.error("Failed to read {}, known "+registrar.key().location().getPath()+" will be reset", listingFile, exception);
            }
        }
    }
    public boolean contains(T obj){
        return list.contains(registrar.getId(obj));
    }
    public void add(T obj) {
        if (!contains(obj))
            list.add(registrar.getId(obj));
    }

    public void save() {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path, Charsets.UTF_8)){
            JsonArray a = new JsonArray();
            list.forEach(l->a.add(l.toString()));
            GsonHelper.writeValue(new JsonWriter(bufferedWriter),a, String::compareTo);
        } catch (IOException iOException) {
            LOGGER.error("Failed to write {}, new known "+registrar.key().location().getPath()+ "won't be present", listingFile, iOException);
        }
    }

}

