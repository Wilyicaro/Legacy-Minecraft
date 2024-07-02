package wily.legacy.util;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import wily.legacy.client.RecipeValue;

import java.util.*;
import java.util.function.*;

public class JsonUtil {
    public static final Map<ResourceLocation, Supplier<ItemStack>> COMMON_ITEMS = new HashMap<>();
    public static <T> Predicate<T> registryMatches(Registry<T> registry, JsonObject o) {
        String name = registry.key().location().getPath();
        if (!o.has(name) && !o.has(name + "s")) return t -> false;
        List<T> tip = new ArrayList<>();
        List<T> tipExclusions = new ArrayList<>();
        List<TagKey<T>> tipTags = new ArrayList<>();

        if (o.has(name) && o.get(name) instanceof JsonPrimitive j && j.isString()) {
            String s = j.getAsString();
            if (s.startsWith("#"))
                tipTags.add(TagKey.create(registry.key(), ResourceLocation.parse(s.replaceFirst("#", ""))));
            else tip.add(registry.get(ResourceLocation.parse(s)));
        }
        if (o.has(name + "s") && o.get(name + "s") instanceof JsonArray a) {
            a.forEach(ie -> {
                if (ie instanceof JsonPrimitive p && p.isString()) {
                    String s = p.getAsString();
                    if (s.startsWith("#"))
                        tipTags.add(TagKey.create(registry.key(), ResourceLocation.parse(s.replaceFirst("#", ""))));
                    else if (s.startsWith("!")) {
                        ResourceLocation l = ResourceLocation.parse(s.replaceFirst("!", ""));
                        registry.getOptional(l).ifPresent(tipExclusions::add);
                    } else tip.add(registry.get(ResourceLocation.parse(s)));
                }
            });
        }
        return t -> !tipExclusions.contains(t) && (tip.contains(t) || tipTags.stream().anyMatch(registry.getHolderOrThrow(registry.getResourceKey(t).orElseThrow())::is));
    }

    public static BiPredicate<Item, DataComponentPatch> registryMatchesItem(JsonObject o){
        DataComponentPatch itemPatch = o.has("components") ? getComponentsFromJson(o.getAsJsonObject("components")) : null;
        Predicate<Item> p = registryMatches(BuiltInRegistries.ITEM,o);
        return (item, t) -> p.test(item) && (itemPatch == null || Objects.equals(t,itemPatch));
    }
    public static Supplier<ItemStack> getItemFromJson(JsonElement element, boolean allowComponent){
        JsonElement itemElement = element;
        ItemStack itemStack;
        if (element instanceof JsonObject o){
            if (o.has("common_item")) return COMMON_ITEMS.getOrDefault(ResourceLocation.tryParse(GsonHelper.getAsString(o, "common_item")),()-> ItemStack.EMPTY);
            itemElement = o.get("item");
        }
        if (itemElement instanceof JsonPrimitive j && j.isString() && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(j.getAsString()))) {
            Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.getHolder(ResourceLocation.parse(j.getAsString()));
            itemStack = (allowComponent && element instanceof JsonObject o && o.get("components") instanceof JsonObject obj ? item.map(i-> new ItemStack(i, 1,getComponentsFromJson(obj))) : item.map(ItemStack::new)).get();
        } else {
            itemStack = ItemStack.EMPTY;
        }
        return ()-> itemStack;
    }
    public static DataComponentPatch getComponentsFromJson(JsonObject jsonComponents){
        return DataComponentPatch.CODEC.parse(JsonOps.INSTANCE,jsonComponents).getOrThrow();
    }

    public static <C extends RecipeInput, T extends Recipe<C>> void addGroupedRecipeValuesFromJson(Map<String,List<RecipeValue<C,T>>> groups, JsonElement element){
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

    public static <C extends RecipeInput, T extends Recipe<C>> void addRecipeValue(Map<String,List<RecipeValue<C, T>>> map, String key, String recipeString){
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
