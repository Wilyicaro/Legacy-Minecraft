package wily.legacy.client;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponentPatch;
 //?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
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
import wily.legacy.util.LegacyTipBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public record LegacyTipOverride(BiPredicate<Item, /*? if <1.20.5 {*//*CompoundTag*//*?} else {*/DataComponentPatch/*?}*/> matchItemOverride, Predicate<Block> matchBlockOverride, Predicate<EntityType<?>> matchEntityOverride, LegacyTipBuilder tip) {
    public static final List<LegacyTipOverride> list = new ArrayList<>();
    private static final String TIP_OVERRIDES = "tip_overrides";

    public static final Component SPAWN_EGG_TIP = Component.translatable("item.minecraft.spawn_egg.tip");

    public static BiFunction<Item, /*? if <1.20.5 {*//*CompoundTag*//*?} else {*/DataComponentPatch/*?}*/, LegacyTipBuilder> itemOverrideCache;
    public static Function<EntityType<?>, LegacyTipBuilder> entityOverrideCache;

    public static LegacyTipBuilder getOverride(ItemStack stack){
        return itemOverrideCache.apply(stack.getItem(),stack./*? if <1.20.5 {*//*getTag*//*?} else {*/getComponentsPatch/*?}*/());
    }
    public static LegacyTipBuilder getOverride(EntityType<?> entity){
        return entityOverrideCache.apply(entity);
    }
    public static class Manager implements ResourceManagerReloadListener {

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            list.clear();
            itemOverrideCache = Util.memoize((item,data)->{
                for (LegacyTipOverride legacyTipOverride : list)
                    if ((item instanceof BlockItem b && legacyTipOverride.matchBlockOverride.test(b.getBlock())) || legacyTipOverride.matchItemOverride.test(item,data)) {
                        return legacyTipOverride.tip;
                    }
                return new LegacyTipBuilder();
            });
            entityOverrideCache = Util.memoize(entity->{
                for (LegacyTipOverride legacyTipOverride : list)
                    if (legacyTipOverride.matchEntityOverride.test(entity)){
                        return legacyTipOverride.tip;
                    }
                return new LegacyTipBuilder();
            });
            list.add(new LegacyTipOverride((i,d)-> i instanceof SpawnEggItem, b-> false, e-> false, new LegacyTipBuilder().tip(SPAWN_EGG_TIP)));

            manager.listResources(TIP_OVERRIDES, (string) -> string.getPath().endsWith(".json")).forEach(((location, resource) -> {
                try {
                    BufferedReader bufferedReader = resource.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    JsonElement ioElement = obj.get("overrides");
                    if (ioElement instanceof JsonArray array)
                        array.forEach(e-> {
                            if (e instanceof JsonObject o) list.add(overrideFromJson(o));
                        });
                    else if (ioElement instanceof JsonObject o) list.add(overrideFromJson(o));
                    bufferedReader.close();
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            }));
        }
        
        protected LegacyTipOverride overrideFromJson(JsonObject o){
            return new LegacyTipOverride(JsonUtil.registryMatchesItem(o), JsonUtil.registryMatches(BuiltInRegistries.BLOCK,o), JsonUtil.registryMatches(BuiltInRegistries.ENTITY_TYPE,o), LegacyTipBuilder.CODEC.parse(JsonOps.INSTANCE, o.get("tip")).result().orElse(new LegacyTipBuilder()));
        }
        @Override
        public String getName() {
            return "legacy:tip_override";
        }
    }
}