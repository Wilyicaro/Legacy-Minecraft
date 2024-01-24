package wily.legacy.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import dev.architectury.registry.registries.Registrar;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import wily.legacy.LegacyMinecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public record LegacyTipOverride(BiPredicate<Item,CompoundTag> matchItemOverride, Predicate<Block> matchBlockOverride, Predicate<EntityType<?>> matchEntityOverride, Component tip) {
    public static final List<LegacyTipOverride> list = new ArrayList<>();
    private static final String TIP_OVERRIDES = "tip_overrides";

    public static final LoadingCache<Pair<Item,CompoundTag>,Component> itemOverrideCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public Component load(Pair<Item,CompoundTag> itemPair) {
            for (LegacyTipOverride legacyTipOverride : list)
                if ((itemPair.getFirst() instanceof BlockItem b && legacyTipOverride.matchBlockOverride.test(b.getBlock())) || legacyTipOverride.matchItemOverride.test(itemPair.getFirst(),itemPair.getSecond()))
                    return legacyTipOverride.tip;
            return Component.empty();
        }
    });
    public static final LoadingCache<EntityType<?>,Component> entityOverrideCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public Component load(EntityType<?> entity) {
            for (LegacyTipOverride legacyTipOverride : list)
                if (legacyTipOverride.matchEntityOverride.test(entity))
                    return legacyTipOverride.tip;
            return Component.empty();
        }
    });

    public static Component getOverride(ItemStack stack){
        return itemOverrideCache.getUnchecked(Pair.of(stack.getItem(),stack.getTag()));
    }
    public static Component getOverride(EntityType<?> entity){
        return entityOverrideCache.getUnchecked(entity);
    }
    public static class Manager extends SimplePreparableReloadListener<List<LegacyTipOverride>> {
        @Override
        protected List<LegacyTipOverride> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            List<LegacyTipOverride> overrides = new ArrayList<>();
            ResourceManager manager = Minecraft.getInstance().getResourceManager();

            manager.listResources(TIP_OVERRIDES, (string) -> string.getPath().endsWith(".json")).forEach(((location, resource) -> {
                try {
                    BufferedReader bufferedReader = resource.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    JsonElement ioElement = obj.get("overrides");
                    if (ioElement instanceof JsonArray array)
                        array.forEach(e-> {
                            if (e instanceof JsonObject o) overrides.add(overrideFromJson(o));
                        });
                    else if (ioElement instanceof JsonObject o) overrides.add(overrideFromJson(o));
                    bufferedReader.close();
                } catch (IOException exception) {
                    LegacyMinecraft.LOGGER.warn(exception.getMessage());
                }
            }));
            return overrides;
        }
        protected LegacyTipOverride overrideFromJson(JsonObject o){
            return new LegacyTipOverride(getJsonItemMatch(o), registryMatchesTip(Registries.BLOCK,o),registryMatchesTip(Registries.ENTITY_TYPE,o), Component.translatable(GsonHelper.getAsString(o, "tip")));
        }
        protected <T>Predicate<T> registryMatchesTip(ResourceKey<Registry<T>> key, JsonObject o){
            String name = key.location().getPath();
            if (!o.has(name) && !o.has(name + "s")) return t-> false;
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
        protected BiPredicate<Item,CompoundTag> getJsonItemMatch(JsonObject o){
            CompoundTag tag;
            if (o.get("nbt") instanceof JsonPrimitive p && p.isString()) {
                try {
                    tag = TagParser.parseTag(p.getAsString());
                } catch (CommandSyntaxException ex) {
                    throw new JsonSyntaxException("Invalid nbt tag: " + ex.getMessage());
                }
            } else tag = null;
            Predicate<Item> p = registryMatchesTip(Registries.ITEM,o);
            return (item, t) -> p.test(item) && NbtUtils.compareNbt(tag, t, true);
        }

        @Override
        protected void apply(List<LegacyTipOverride> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            list.clear();
            itemOverrideCache.invalidateAll();
            list.addAll(object);;
        }
    }
}