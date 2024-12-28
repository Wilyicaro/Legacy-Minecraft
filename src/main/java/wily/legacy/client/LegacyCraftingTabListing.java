package wily.legacy.client;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static wily.legacy.util.JsonUtil.*;

public class LegacyCraftingTabListing implements LegacyTabInfo {
    public static final Codec<LegacyCraftingTabListing> CODEC = RecordCodecBuilder.create(i-> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(LegacyCraftingTabListing::id), DynamicUtil.getComponentCodec().optionalFieldOf("name",null).forGetter(LegacyCraftingTabListing::name),LegacyTabButton.ICON_HOLDER_CODEC.optionalFieldOf("icon",null).forGetter(LegacyCraftingTabListing::iconHolder),RecipeInfo.Filter.LISTING_CODEC.fieldOf("listing").forGetter(LegacyCraftingTabListing::craftings)).apply(i,LegacyCraftingTabListing::new));
    public static final ListMap<ResourceLocation,LegacyCraftingTabListing> map = new ListMap<>();
    private static final String CRAFTING_TAB_LISTING = "crafting_tab_listing.json";
    private final ResourceLocation id;
    public Component name;
    public LegacyTabButton.IconHolder<?> iconHolder;
    private final Map<String, List<RecipeInfo.Filter>> craftings;

    @Deprecated
    public LegacyCraftingTabListing(ResourceLocation id, Component name, LegacyTabButton.IconHolder<?> iconHolder){
        this(id,name,iconHolder,new LinkedHashMap<>());
    }

    public LegacyCraftingTabListing(ResourceLocation id, Component name, LegacyTabButton.IconHolder<?> iconHolder, Map<String,List<RecipeInfo.Filter>> craftings){
        this.id = id;
        this.name = name;
        this.iconHolder = iconHolder;
        this.craftings = craftings;
    }

    @Override
    public boolean isValid() {
        return LegacyTabInfo.super.isValid() && !craftings.isEmpty();
    }

    public ResourceLocation id(){
        return id;
    }

    public Component name(){
        return name;
    }

    public LegacyTabButton.IconHolder<?> iconHolder(){
        return iconHolder;
    }

    public final Map<String, List<RecipeInfo.Filter>> craftings(){
        return craftings;
    }

    public static class Manager implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            map.clear();
            JsonUtil.getOrderedNamespaces(manager).forEach(name->manager.getResource(FactoryAPI.createLocation(name, CRAFTING_TAB_LISTING)).ifPresent(r->{
                try (BufferedReader bufferedReader = r.openAsReader()) {
                    JsonElement element = JsonParser.parseReader(bufferedReader);
                    if (element instanceof JsonObject obj) {
                        Legacy4J.LOGGER.warn("The Crafting Tab Listing {} is using a deprecated syntax, please contact this resource creator or try updating it.", name+":"+CRAFTING_TAB_LISTING);
                        obj.asMap().forEach((c, ce) -> {
                            ResourceLocation id = FactoryAPI.createLocation(c);
                            if (ce instanceof JsonObject go) {
                                JsonElement listingElement = go.get("listing");
                                if (map.containsKey(id)) {
                                    LegacyCraftingTabListing l = map.get(id);
                                    ifJsonStringNotNull(go, "displayName", Component::translatable, n -> l.name = n);
                                    LegacyTabButton.DEPRECATED_ICON_HOLDER_CODEC.parse(JsonOps.INSTANCE, go).result().ifPresent(icon -> l.iconHolder = icon);
                                    JsonUtil.addGroupedRecipeValuesFromJson(l.craftings, listingElement);
                                } else  {
                                    LegacyCraftingTabListing listing = new LegacyCraftingTabListing(id, getJsonStringOrNull(go, "displayName", Component::translatable), LegacyTabButton.DEPRECATED_ICON_HOLDER_CODEC.parse(JsonOps.INSTANCE, go).result().orElse(null));
                                    JsonUtil.addGroupedRecipeValuesFromJson(listing.craftings, listingElement);
                                    map.put(listing.id(),listing);
                                }
                            }
                        });
                    } else if (element instanceof JsonArray a) a.forEach(e-> CODEC.parse(JsonOps.INSTANCE,e).result().ifPresent(listing->{
                        if (map.containsKey(listing.id)){
                            LegacyCraftingTabListing l = map.get(listing.id);
                            if (listing.name != null) l.name = listing.name;
                            if (listing.iconHolder != null) l.iconHolder = listing.iconHolder;
                            listing.craftings.forEach((s,f)->{
                                if (l.craftings.containsKey(s)) l.craftings.get(s).addAll(f);
                                else l.craftings.put(s,f);
                            });
                        } else if (listing.isValid()) map.put(listing.id,listing);
                    }));
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            }));
        }

        @Override
        public String getName() {
            return "legacy:crafting_tab_listing";
        }
    }
}
