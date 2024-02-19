package wily.legacy.client;

import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import wily.legacy.LegacyMinecraft;
import wily.legacy.util.CompoundTagUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record LegacyCreativeTabListing(Component name, ResourceLocation icon, CompoundTag itemIconTag, List<ItemStack> displayItems) {
    public static final List<LegacyCreativeTabListing> list = new ArrayList<>();
    private static final String LISTING = "creative_tab_listing.json";
    public static class Manager extends SimplePreparableReloadListener<List<LegacyCreativeTabListing>> {
        @Override
        protected List<LegacyCreativeTabListing> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            List<LegacyCreativeTabListing> creativeTabListing = new ArrayList<>();
            resourceManager.getNamespaces().forEach(name->{
                resourceManager.getResource(new ResourceLocation(name,LISTING)).ifPresent(r->{
                    try {
                        BufferedReader bufferedReader = r.openAsReader();
                        JsonObject obj = GsonHelper.parse(bufferedReader);
                        obj.asMap().forEach((s,element)->{
                            if (element instanceof JsonObject tabObj) {
                                CompoundTag tag = new CompoundTag();
                                ResourceLocation tabIcon = new ResourceLocation(GsonHelper.getAsString(tabObj,"icon"));
                                if (BuiltInRegistries.ITEM.containsKey(tabIcon) && tabObj.get("nbt") instanceof JsonPrimitive p && p.isString()) tag = CompoundTagUtil.parseCompoundTag(p.getAsString());
                                LegacyCreativeTabListing l = new LegacyCreativeTabListing(Component.translatable(s),new ResourceLocation(GsonHelper.getAsString(tabObj,"icon")), tag, new ArrayList<>());
                                if (tabObj.get("listing") instanceof JsonArray a) {
                                    a.forEach(e -> {
                                        JsonElement itemElement = e;
                                        if (e instanceof JsonObject o) itemElement = o.get("item");
                                        if (itemElement instanceof JsonPrimitive j && j.isString() && BuiltInRegistries.ITEM.containsKey(new ResourceLocation(j.getAsString()))) {
                                            ItemStack i = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(j.getAsString())));
                                            l.displayItems.add(i);
                                            if (e instanceof JsonObject o && o.get("nbt") instanceof JsonPrimitive p && p.isString()) {
                                                i.setTag(CompoundTagUtil.parseCompoundTag(p.getAsString()));
                                            }
                                        }
                                    });
                                }
                                creativeTabListing.add(l);
                            }
                        });

                        bufferedReader.close();
                    } catch (IOException var8) {
                        LegacyMinecraft.LOGGER.warn(var8);
                    }
                });
            });
            return creativeTabListing;
        }

        @Override
        protected void apply(List<LegacyCreativeTabListing> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            list.clear();
            list.addAll(object);;
        }
    }
}