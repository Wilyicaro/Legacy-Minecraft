package wily.legacy.client;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BannerPattern;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class LoomTabListing implements LegacyTabInfo {
    public static final Codec<List<ResourceKey<BannerPattern>>> PATTERNS_CODEC = ResourceKey.codec(Registries.BANNER_PATTERN).listOf().xmap(ArrayList::new,Function.identity());
    public static final Codec<LoomTabListing> CODEC = RecordCodecBuilder.create(i->i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(LoomTabListing::id), DynamicUtil.getComponentCodec().fieldOf("name").forGetter(LoomTabListing::name), LegacyTabButton.ICON_HOLDER_CODEC.fieldOf("icon").forGetter(LoomTabListing::iconHolder), PATTERNS_CODEC.fieldOf("listing").forGetter(LoomTabListing::patterns)).apply(i,LoomTabListing::new));
    public static final ListMap<ResourceLocation,LoomTabListing> map = new ListMap<>();
    private static final String LOOM_TAB_LISTING = "loom_tab_listing.json";
    private final ResourceLocation id;
    private Component name;
    private LegacyTabButton.IconHolder<?> iconHolder;
    private final List<ResourceKey<BannerPattern>> patterns;

    public LoomTabListing(ResourceLocation id, Component displayName, LegacyTabButton.IconHolder<?> iconHolder, List<ResourceKey<BannerPattern>> patterns){
        this.id = id;
        this.name = displayName;
        this.iconHolder = iconHolder;
        this.patterns = patterns;
    }

    public boolean isValid(){
        return LegacyTabInfo.super.isValid() && !patterns.isEmpty();
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Component name() {
        return name;
    }

    @Override
    public LegacyTabButton.IconHolder<?> iconHolder() {
        return iconHolder;
    }

    public List<ResourceKey<BannerPattern>> patterns(){
        return patterns;
    }

    public void addFrom(LoomTabListing otherListing){
        if (otherListing.name != null) name = otherListing.name;
        if (otherListing.iconHolder != null) iconHolder = otherListing.iconHolder;
        patterns.addAll(otherListing.patterns);
    }

    public static class Manager implements ResourceManagerReloadListener {

        @Override
        public String getName() {
            return "legacy:loom_tab_listing";
        }

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            map.clear();
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            JsonUtil.getOrderedNamespaces(manager).forEach(name->manager.getResource(FactoryAPI.createLocation(name, LOOM_TAB_LISTING)).ifPresent(r->{
                try (BufferedReader bufferedReader = r.openAsReader()) {
                    JsonElement element = JsonParser.parseReader(bufferedReader);
                    if (element instanceof JsonArray a) a.forEach(e-> CODEC.parse(JsonOps.INSTANCE,e).result().ifPresent(listing->{
                        if (map.containsKey(listing.id)){
                            map.get(listing.id).addFrom(listing);
                        } else if (listing.isValid()) map.put(listing.id,listing);
                    }));
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            }));
        }

        public static void addBannerPatternsFromJson(List<ResourceKey<BannerPattern>> groups, JsonElement element){
            if (element instanceof JsonArray a) a.forEach(e->{
                if (e instanceof JsonPrimitive p && p.isString()) groups.add(ResourceKey.create(Registries.BANNER_PATTERN, FactoryAPI.createLocation(p.getAsString())));
            });
        }
    }
}
