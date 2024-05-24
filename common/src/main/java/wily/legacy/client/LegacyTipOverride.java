package wily.legacy.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import wily.legacy.Legacy4J;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public record LegacyTipOverride(BiPredicate<Item,CompoundTag> matchItemOverride, Predicate<Block> matchBlockOverride, Predicate<EntityType<?>> matchEntityOverride, Component tip) {
    public static final List<LegacyTipOverride> list = new ArrayList<>();
    private static final String TIP_OVERRIDES = "tip_overrides";
    public static final Component SPAWN_EGG_TIP = Component.translatable("item.minecraft.spawn_egg.tip");

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
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            }));
            return overrides;
        }
        protected LegacyTipOverride overrideFromJson(JsonObject o){
            return new LegacyTipOverride(JsonUtil.registryMatchesItem(o), JsonUtil.registryMatches(BuiltInRegistries.BLOCK,o), JsonUtil.registryMatches(BuiltInRegistries.ENTITY_TYPE,o), Component.translatable(GsonHelper.getAsString(o, "tip")));
        }

        @Override
        protected void apply(List<LegacyTipOverride> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            list.clear();
            itemOverrideCache.invalidateAll();
            list.add(new LegacyTipOverride((i,d)-> i instanceof SpawnEggItem, b-> false, e-> false,SPAWN_EGG_TIP));
            list.addAll(object);;
        }
    }
}