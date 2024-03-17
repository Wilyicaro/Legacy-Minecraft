package wily.legacy.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.registry.registries.Registrar;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import wily.legacy.LegacyMinecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class JsonUtil {
    public static <T> Predicate<T> registryMatches(ResourceKey<Registry<T>> key, JsonObject o) {
        String name = key.location().getPath();
        if (!o.has(name) && !o.has(name + "s")) return t -> false;
        List<T> tip = new ArrayList<>();
        List<T> tipExclusions = new ArrayList<>();
        List<TagKey<T>> tipTags = new ArrayList<>();
        Registrar<T> registrar = LegacyMinecraft.REGISTRIES.get().get(key);
        if (o.has(name) && o.get(name) instanceof JsonPrimitive j && j.isString()) {
            String s = j.getAsString();
            if (s.startsWith("#"))
                tipTags.add(TagKey.create(key, new ResourceLocation(s.replaceFirst("#", ""))));
            else tip.add(registrar.get(new ResourceLocation(s)));
        }
        if (o.has(name + "s") && o.get(name + "s") instanceof JsonArray a) {
            a.forEach(ie -> {
                if (ie instanceof JsonPrimitive p && p.isString()) {
                    String s = p.getAsString();
                    if (s.startsWith("#"))
                        tipTags.add(TagKey.create(key, new ResourceLocation(s.replaceFirst("#", ""))));
                    else if (s.startsWith("!")) {
                        ResourceLocation l = new ResourceLocation(s.replaceFirst("!", ""));
                        if (registrar.contains(l))
                            tipExclusions.add(registrar.get(l));
                    } else tip.add(registrar.get(new ResourceLocation(s)));
                }
            });
        }
        return t -> !tipExclusions.contains(t) && (tip.contains(t) || tipTags.stream().anyMatch(registrar.getHolder(registrar.getId(t))::is));
    }

    public static BiPredicate<Item, CompoundTag> registryMatchesItem(JsonObject o){
        CompoundTag tag;
        if (o.get("nbt") instanceof JsonPrimitive p && p.isString()) {
            try {
                tag = TagParser.parseTag(p.getAsString());
            } catch (CommandSyntaxException ex) {
                throw new JsonSyntaxException("Invalid nbt tag: " + ex.getMessage());
            }
        } else tag = null;
        Predicate<Item> p = registryMatches(Registries.ITEM,o);
        return (item, t) -> p.test(item) && NbtUtils.compareNbt(tag, t, true);
    }
}
