package wily.legacy.util;

import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.registry.registries.Registrar;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import wily.legacy.Legacy4J;
import wily.legacy.client.RecipeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class JsonUtil {
    public static <T> Predicate<T> registryMatches(Registry<T> registry, JsonObject o) {
        String name = registry.key().location().getPath();
        if (!o.has(name) && !o.has(name + "s")) return t -> false;
        List<T> tip = new ArrayList<>();
        List<T> tipExclusions = new ArrayList<>();
        List<TagKey<T>> tipTags = new ArrayList<>();

        if (o.has(name) && o.get(name) instanceof JsonPrimitive j && j.isString()) {
            String s = j.getAsString();
            if (s.startsWith("#"))
                tipTags.add(TagKey.create(registry.key(), new ResourceLocation(s.replaceFirst("#", ""))));
            else tip.add(registry.get(new ResourceLocation(s)));
        }
        if (o.has(name + "s") && o.get(name + "s") instanceof JsonArray a) {
            a.forEach(ie -> {
                if (ie instanceof JsonPrimitive p && p.isString()) {
                    String s = p.getAsString();
                    if (s.startsWith("#"))
                        tipTags.add(TagKey.create(registry.key(), new ResourceLocation(s.replaceFirst("#", ""))));
                    else if (s.startsWith("!")) {
                        ResourceLocation l = new ResourceLocation(s.replaceFirst("!", ""));
                        registry.getOptional(l).ifPresent(tipExclusions::add);
                    } else tip.add(registry.get(new ResourceLocation(s)));
                }
            });
        }
        return t -> !tipExclusions.contains(t) && (tip.contains(t) || tipTags.stream().anyMatch(registry.getHolderOrThrow(registry.getResourceKey(t).orElseThrow())::is));
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
        Predicate<Item> p = registryMatches(BuiltInRegistries.ITEM,o);
        return (item, t) -> p.test(item) && NbtUtils.compareNbt(tag, t, true);
    }
    public static ItemStack getItemFromJson(JsonElement element, boolean allowNbt){
        JsonElement itemElement = element;
        if (element instanceof JsonObject o) itemElement = o.get("item");
        if (itemElement instanceof JsonPrimitive j && j.isString() && BuiltInRegistries.ITEM.containsKey(new ResourceLocation(j.getAsString()))) {
            ItemStack i = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(j.getAsString())));
            if (allowNbt && element instanceof JsonObject o && o.get("nbt") instanceof JsonPrimitive p && p.isString()) {
                i.setTag(CompoundTagUtil.parseCompoundTag(p.getAsString()));
            }
            return i;
        }
        return ItemStack.EMPTY;
    }

    public static <C extends Container, T extends Recipe<C>> void addGroupedRecipeValuesFromJson(Map<String,List<RecipeValue<C,T>>> groups, JsonElement element){
        if (element instanceof JsonArray a) a.forEach(e->{
            if (e instanceof JsonPrimitive p && p.isString()) addRecipeValue(groups,p.getAsString(),p.getAsString());
            else if(e instanceof JsonObject obj && obj.get("recipes") instanceof JsonArray rcps) rcps.forEach(r-> {
                if (r instanceof JsonPrimitive p && p.isString())  addRecipeValue(groups, GsonHelper.getAsString(obj,"group",p.getAsString()), p.getAsString());
            });
        });
        else if (element instanceof JsonObject obj) obj.asMap().forEach((g,ge)->{
            if (ge instanceof JsonArray a) a.forEach(e-> {
                if (e instanceof JsonPrimitive p && p.isString())  addRecipeValue(groups,g, p.getAsString());
            });
        });
    }

    public static <C extends Container, T extends Recipe<C>> void addRecipeValue(Map<String,List<RecipeValue<C, T>>> map, String key, String recipeString){
        addMapListEntry(map,key.isEmpty() ? recipeString : key, RecipeValue.create(recipeString));
    }

    public static <K,V> void addMapListEntry(Map<K,List<V>> map, K key, V entry){
        map.computeIfAbsent(key,k-> new ArrayList<>()).add(entry);
    }

    public static <T> T getJsonStringOrNull(JsonObject object, String element, Function<String,T> constructor){
        String s = GsonHelper.getAsString(object,element, null);
        return s == null ? null : constructor.apply(s);
    }

    public static <T> void ifJsonStringNotNull(JsonObject object, String element, Function<String,T> constructor, Consumer<T> consumer){
        T obj = getJsonStringOrNull(object,element,constructor);
        if  (obj != null) consumer.accept(obj);
    }
}
