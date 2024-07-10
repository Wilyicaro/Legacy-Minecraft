package wily.legacy.client;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.util.CompoundTagUtil;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public record LegacyCreativeTabListing(Component name, Function<LegacyTabButton, Renderable> icon, List<Supplier<ItemStack>> displayItems) {
    public static final List<LegacyCreativeTabListing> list = new ArrayList<>();
    private static final String LISTING = "creative_tab_listing.json";
    public static void rebuildVanillaCreativeTabsItems(Minecraft minecraft){
        if (minecraft.player != null) CreativeModeTabs.tryRebuildTabContents(minecraft.player.connection.enabledFeatures(), minecraft.options.operatorItemsTab().get(), minecraft.player.level().registryAccess());
    }
    public static class Manager extends SimplePreparableReloadListener<List<LegacyCreativeTabListing>> {

        public Manager(){
            JsonUtil.COMMON_ITEMS.put(new ResourceLocation("ominous_banner"), ()-> {
                if (Minecraft.getInstance().getConnection() == null) return ItemStack.EMPTY;
                return Raid.getLeaderBannerInstance(Minecraft.getInstance().getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN));
            });
        }
        @Override
        protected List<LegacyCreativeTabListing> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            List<LegacyCreativeTabListing> creativeTabListing = new ArrayList<>();
            JsonUtil.getOrderedNamespaces(resourceManager).forEach(name->{
                resourceManager.getResource(new ResourceLocation(name,LISTING)).ifPresent(r->{
                    try {
                        BufferedReader bufferedReader = r.openAsReader();
                        JsonObject obj = GsonHelper.parse(bufferedReader);
                        obj.asMap().forEach((s,element)->{
                            if (element instanceof JsonObject tabObj) {
                                LegacyCreativeTabListing l = new LegacyCreativeTabListing(Component.translatable(s),JsonUtil.getJsonLegacyTabButtonIconOrNull(tabObj), new ArrayList<>());
                                if (tabObj.get("listing") instanceof JsonArray a) a.forEach(e -> l.displayItems.add(JsonUtil.getItemFromJson(e,true)));
                                creativeTabListing.add(l);
                            }
                        });

                        bufferedReader.close();
                    } catch (IOException var8) {
                        Legacy4J.LOGGER.warn(var8);
                    }
                });
            });
            return creativeTabListing;
        }

        @Override
        protected void apply(List<LegacyCreativeTabListing> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            list.clear();
            list.addAll(object);
        }
    }
}