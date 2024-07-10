package wily.legacy.util;

import com.google.gson.*;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import wily.legacy.client.screen.LegacyTabButton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Stream;

public class JsonUtil {
    public static final Map<ResourceLocation, Supplier<ItemStack>> COMMON_ITEMS = new HashMap<>();
    public static RegistryOps<JsonElement> jsonRegistryOps;
    public static final Map<JsonElement,ItemStack> JSON_ITEMS = new ConcurrentHashMap<>();
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

    public static BiPredicate<Item, DataComponentPatch> registryMatchesItem(JsonObject o){
        DataComponentPatch itemPatch = o.has("components") ? getComponentsFromJson(o.getAsJsonObject("components")) : null;
        Predicate<Item> p = registryMatches(BuiltInRegistries.ITEM,o);
        return (item, t) -> p.test(item) && (itemPatch == null || Objects.equals(t,itemPatch));
    }
    public static Supplier<ItemStack> getItemFromJson(JsonElement element, boolean allowComponent){
        JsonElement itemElement = element;
        if (element instanceof JsonObject o){
            if (o.has("common_item")) return COMMON_ITEMS.getOrDefault(ResourceLocation.tryParse(GsonHelper.getAsString(o, "common_item")),()-> ItemStack.EMPTY);
            itemElement = o.get("item");
        }
        if (itemElement instanceof JsonPrimitive j && j.isString() && BuiltInRegistries.ITEM.containsKey(new ResourceLocation(j.getAsString()))) {
            Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.getHolder(new ResourceLocation(j.getAsString()));
            return ()-> JSON_ITEMS.computeIfAbsent(element, e-> (allowComponent && element instanceof JsonObject o && o.get("components") instanceof JsonObject obj ? item.map(i-> new ItemStack(i, 1,getComponentsFromJson(obj))) : item.map(ItemStack::new)).get());
        }
        return ()-> ItemStack.EMPTY;
    }
    public static DataComponentPatch getComponentsFromJson(JsonObject jsonComponents){
        return DataComponentPatch.CODEC.parse(jsonRegistryOps == null ? JsonOps.INSTANCE : jsonRegistryOps,jsonComponents).getOrThrow();
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
    public static Function<LegacyTabButton, Renderable> getJsonLegacyTabButtonIconOrNull(JsonObject object){
        if (!object.has("icon")) return null;
        String type = GsonHelper.getAsString(object,"iconType","sprite");
        switch (type){
            case "item"-> {
                Supplier<ItemStack> stack = getItemFromJson(object.get("icon"),true);
                return t-> LegacyTabButton.iconOf(stack.get()).apply(t);
            }
            case "sprite"-> {
                return getJsonStringOrNull(object,"icon",s->LegacyTabButton.iconOf(new ResourceLocation(s)));
            }
            default -> {
                return null;
            }
        }
    }

    public static <T> T getJsonStringOrNull(JsonObject object, String element, Function<String,T> constructor){
        String s = GsonHelper.getAsString(object,element, null);
        return s == null ? null : constructor.apply(s);
    }

    public static <T> void ifJsonStringNotNull(JsonObject object, String element, Function<String,T> constructor, Consumer<T> consumer){
        T obj = getJsonStringOrNull(object,element,constructor);
        if  (obj != null) consumer.accept(obj);
    }
    public static Stream<String> getOrderedNamespaces(ResourceManager manager){
        return manager.getNamespaces().stream().sorted(Comparator.comparingInt(s-> s.equals("legacy") ? 0 : 1));
    }

    public static Integer optionalJsonColor(JsonObject object, String element, Integer fallback) {
        return optionalJsonColor(object.get(element),fallback);
    }
    public static Integer optionalJsonColor(JsonElement element, Integer fallback) {
        if (element instanceof JsonPrimitive p){
            if (p.isString() && p.getAsString().startsWith("#")) return (int) Long.parseLong(p.getAsString().substring(1), 16);
            return p.getAsInt();
        }
        return fallback;
    }
}
