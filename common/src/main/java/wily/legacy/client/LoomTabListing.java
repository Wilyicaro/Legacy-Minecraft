package wily.legacy.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BannerPattern;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static wily.legacy.util.JsonUtil.*;

public class LoomTabListing {
    public static final List<LoomTabListing> list = new ArrayList<>();
    private static final String LOOM_TAB_LISTING = "loom_tab_listing.json";
    public String id;
    public Component displayName;
    public Function<LegacyTabButton, Renderable> icon;
    public final List<ResourceKey<BannerPattern>> patterns = new ArrayList<>();


    public LoomTabListing(String id, Component displayName, Function<LegacyTabButton, Renderable> icon){
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
    }
    public boolean isValid(){
        return displayName != null && id != null && !patterns.isEmpty() && icon != null;
    }
    public static class Manager extends SimplePreparableReloadListener<List<LoomTabListing>> {

        @Override
        protected List<LoomTabListing> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            List<LoomTabListing> listings = new ArrayList<>();
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            JsonUtil.getOrderedNamespaces(manager).forEach(name->manager.getResource(new ResourceLocation(name, LOOM_TAB_LISTING)).ifPresent(r->{
                try {
                    BufferedReader bufferedReader = r.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    obj.asMap().forEach((c,ce)->{
                        if (ce instanceof JsonObject go) {
                            JsonElement listingElement = go.get("listing");
                            listings.stream().filter(l-> c.equals(l.id)).findFirst().ifPresentOrElse(l->{
                                ifJsonStringNotNull(go,"displayName", Component::translatable, n-> l.displayName = n);
                                Function<LegacyTabButton, Renderable> icon = getJsonLegacyTabButtonIconOrNull(go);
                                if (icon != null) l.icon = icon;
                                addBannerPatternsFromJson(l.patterns,listingElement);
                            }, ()->{
                                LoomTabListing listing = new LoomTabListing(c,getJsonStringOrNull(go,"displayName",Component::translatable),getJsonLegacyTabButtonIconOrNull(go));
                                addBannerPatternsFromJson(listing.patterns,listingElement);
                                listings.add(listing);
                            });
                        }
                    });
                    bufferedReader.close();
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            }));
            return listings;
        }

        public static void addBannerPatternsFromJson(List<ResourceKey<BannerPattern>> groups, JsonElement element){
            if (element instanceof JsonArray a) a.forEach(e->{
                if (e instanceof JsonPrimitive p && p.isString()) groups.add(ResourceKey.create(Registries.BANNER_PATTERN, new ResourceLocation(p.getAsString())));
            });
        }
        @Override
        protected void apply(List<LoomTabListing> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            list.clear();
            list.addAll(object);;
        }
    }
}
