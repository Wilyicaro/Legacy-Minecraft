package wily.legacy.client;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import wily.legacy.LegacyMinecraft;
import wily.legacy.util.CompoundTagUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static wily.legacy.util.CompoundTagUtil.getJsonStringOrNull;
import static wily.legacy.util.CompoundTagUtil.ifJsonStringNotNull;

public class LegacyCraftingTabListing {
    public static final List<LegacyCraftingTabListing> list = new ArrayList<>();
    private static final String CRAFTING_TAB_LISTING = "crafting_tab_listing.json";
    public String id;
    public Component displayName;
    public ResourceLocation icon;
    public CompoundTag itemIconTag;
    public final Map<String,List<RecipeValue<CraftingContainer, CraftingRecipe>>> craftings = new LinkedHashMap<>();


    public LegacyCraftingTabListing(String id, Component displayName, ResourceLocation icon, CompoundTag itemIconTag){
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.itemIconTag = itemIconTag;
    }
    public boolean isValid(){
        return displayName != null && id != null && !craftings.isEmpty() && icon != null;
    }
    public static class Manager extends SimplePreparableReloadListener<List<LegacyCraftingTabListing>> {
        @Override
        protected List<LegacyCraftingTabListing> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            List<LegacyCraftingTabListing> listings = new ArrayList<>();
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            manager.getNamespaces().stream().sorted(Comparator.comparingInt(s-> s.equals("legacy") ? 0 : 1)).forEach(name->manager.getResource(new ResourceLocation(name, CRAFTING_TAB_LISTING)).ifPresent(r->{
                try {
                    BufferedReader bufferedReader = r.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    obj.asMap().forEach((c,ce)->{
                        if (ce instanceof JsonObject go) {
                            JsonElement listingElement = go.get("listing");
                            listings.stream().filter(l-> c.equals(l.id)).findFirst().ifPresentOrElse(l->{
                                ifJsonStringNotNull(go,"displayName", Component::translatable, n-> l.displayName = n);
                                ifJsonStringNotNull(go,"icon", ResourceLocation::new, n-> l.icon = n);
                                ifJsonStringNotNull(go,"nbt", CompoundTagUtil::parseCompoundTag, n-> l.itemIconTag = n);
                                addGroupedRecipeValuesFromJson(l.craftings,listingElement);
                            }, ()->{
                                LegacyCraftingTabListing listing = new LegacyCraftingTabListing(c,getJsonStringOrNull(go,"displayName",Component::translatable),getJsonStringOrNull(go,"icon",ResourceLocation::new), getJsonStringOrNull(go,"nbt",CompoundTagUtil::parseCompoundTag));
                                addGroupedRecipeValuesFromJson(listing.craftings,listingElement);
                                listings.add(listing);
                            });
                        }
                    });
                    bufferedReader.close();
                } catch (IOException exception) {
                    LegacyMinecraft.LOGGER.warn(exception.getMessage());
                }
            }));
            return listings;
        }
        public static <K,V> void addMapListEntry(Map<K,List<V>> map, K key, V entry){
            map.computeIfAbsent(key,k-> new ArrayList<>()).add(entry);
        }
        public static <C extends Container, T extends Recipe<C>> void addRecipeValue(Map<String,List<RecipeValue<C, T>>> map, String key, String recipeString){
            addMapListEntry(map,key.isEmpty() ? recipeString : key, RecipeValue.create(recipeString));
        }
        public static <C extends Container, T extends Recipe<C>> void addGroupedRecipeValuesFromJson(Map<String,List<RecipeValue<C,T>>> groups, JsonElement element){
            if (element instanceof JsonArray a) a.forEach(e->{
                if (e instanceof JsonPrimitive p && p.isString()) addRecipeValue(groups,p.getAsString(),p.getAsString());
                else if(e instanceof JsonObject obj && obj.get("recipes") instanceof JsonArray rcps) rcps.forEach(r-> {
                    if (r instanceof JsonPrimitive p && p.isString())  addRecipeValue(groups,GsonHelper.getAsString(obj,"group",p.getAsString()), p.getAsString());
                });
            });
            else if (element instanceof JsonObject obj) obj.asMap().forEach((g,ge)->{
                if (ge instanceof JsonArray a) a.forEach(e-> {
                    if (e instanceof JsonPrimitive p && p.isString())  addRecipeValue(groups,g, p.getAsString());
                });
            });
        }


        @Override
        protected void apply(List<LegacyCraftingTabListing> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            list.clear();
            list.addAll(object);;
        }
    }
}
