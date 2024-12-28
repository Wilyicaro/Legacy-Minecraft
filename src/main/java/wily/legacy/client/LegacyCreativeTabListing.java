package wily.legacy.client;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class LegacyCreativeTabListing implements LegacyTabInfo {
    public static final Codec<LegacyCreativeTabListing> CODEC = RecordCodecBuilder.create(i->i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(LegacyCreativeTabListing::id),DynamicUtil.getComponentCodec().fieldOf("name").forGetter(LegacyCreativeTabListing::name), LegacyTabButton.ICON_HOLDER_CODEC.fieldOf("icon").forGetter(LegacyCreativeTabListing::iconHolder), DynamicUtil.ITEM_SUPPLIER_CODEC.listOf().fieldOf("listing").forGetter(LegacyCreativeTabListing::displayItems)).apply(i,LegacyCreativeTabListing::new));
    public static final ListMap<ResourceLocation,LegacyCreativeTabListing> map = new ListMap<>();
    private static final String LISTING = "creative_tab_listing.json";
    private final ResourceLocation id;
    private Component name;
    private LegacyTabButton.IconHolder<?> iconHolder;
    private final List<ArbitrarySupplier<ItemStack>> displayItems;

    public static void rebuildVanillaCreativeTabsItems(Minecraft minecraft){
        if (minecraft.getConnection() != null && CreativeModeTabs.tryRebuildTabContents(minecraft.getConnection().enabledFeatures(), minecraft.options.operatorItemsTab().get(), minecraft.getConnection().registryAccess())){
            //? if >=1.21 {
            List<ItemStack> list = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
            minecraft.getConnection().searchTrees().updateCreativeTooltips(minecraft.getConnection().registryAccess(), list);
            minecraft.getConnection().searchTrees().updateCreativeTags(list);
            //?}
        }
    }
    public LegacyCreativeTabListing(ResourceLocation id, Component name, LegacyTabButton.IconHolder<?> iconHolder, List<ArbitrarySupplier<ItemStack>> displayItems){
        this.id = id;
        this.name = name;
        this.iconHolder = iconHolder;
        this.displayItems = displayItems;
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

    public List<ArbitrarySupplier<ItemStack>> displayItems(){
        return displayItems;
    }

    public static class Manager implements ResourceManagerReloadListener {

        public Manager(){
            DynamicUtil.COMMON_ITEMS.put(FactoryAPI.createVanillaLocation("ominous_banner"), ()-> {
                if (Minecraft.getInstance().getConnection() == null) return ItemStack.EMPTY;
                return Raid./*? >=1.21.2 {*/ getOminousBannerInstance/*?} else {*//*getLeaderBannerInstance*//*?}*/(/*? if >=1.20.5 {*/Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)/*?}*/);
            });
        }
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            map.clear();
            JsonUtil.getOrderedNamespaces(resourceManager).forEach(name-> resourceManager.getResource(FactoryAPI.createLocation(name,LISTING)).ifPresent(r->{
                try (BufferedReader bufferedReader = r.openAsReader()) {
                    JsonElement json = JsonParser.parseReader(bufferedReader);
                    if (json instanceof JsonObject obj) {
                        Legacy4J.LOGGER.warn("The Creative Tab Listing {} is using a deprecated syntax, please contact this resource creator or try updating it.", name+":"+LISTING);
                        obj.asMap().forEach((s, element) -> {
                            if (element instanceof JsonObject tabObj) {
                                LegacyCreativeTabListing l = new LegacyCreativeTabListing(FactoryAPI.createLocation(s.toLowerCase(Locale.ENGLISH)),Component.translatable(s), LegacyTabButton.DEPRECATED_ICON_HOLDER_CODEC.parse(JsonOps.INSTANCE, tabObj).result().orElse(null), new ArrayList<>());
                                if (tabObj.get("listing") instanceof JsonArray a)
                                    a.forEach(e -> l.displayItems.add(JsonUtil.getItemFromJson(e, true)));
                                map.put(l.id(), l);
                            }
                        });
                    } else if (json instanceof JsonArray a) a.forEach(e->CODEC.parse(JsonOps.INSTANCE,e).result().ifPresent(listing->{
                        if (map.containsKey(listing.id)){
                            LegacyCreativeTabListing l = map.get(listing.id);
                            if (listing.name != null) l.name = listing.name;
                            if (listing.iconHolder != null) l.iconHolder = listing.iconHolder;
                            l.displayItems.addAll(listing.displayItems);
                        } else if (listing.isValid()) map.put(listing.id,listing);
                    }));
                } catch (IOException var8) {
                    Legacy4J.LOGGER.warn(var8);
                }
            }));
        }

        @Override
        public String getName() {
            return "legacy:creative_tab_listing";
        }
    }
}