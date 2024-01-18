package wily.legacy.client;

import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import wily.legacy.LegacyMinecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record LegacyCreativeTabListing(Component name, ResourceLocation icon, List<ItemStack> displayItems) {
    public static final List<LegacyCreativeTabListing> list = new ArrayList<>();
    private static final ResourceLocation LISTING_LOCATION = new ResourceLocation(LegacyMinecraft.MOD_ID,"creative_tab_listing.json");
    public static class Manager extends SimplePreparableReloadListener<List<LegacyCreativeTabListing>> {
        @Override
        protected List<LegacyCreativeTabListing> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            try {
                List<LegacyCreativeTabListing> creativeTabListing = new ArrayList<>();
                BufferedReader bufferedReader = Minecraft.getInstance().getResourceManager().openAsReader(LISTING_LOCATION);
                JsonObject obj = GsonHelper.parse(bufferedReader);
                obj.asMap().forEach((s,element)->{
                    if (element instanceof JsonObject tabObj) {
                        LegacyCreativeTabListing l = new LegacyCreativeTabListing(Component.translatable(s),new ResourceLocation(GsonHelper.getAsString(tabObj,"icon")), new ArrayList<>());
                        if (tabObj.get("listing") instanceof JsonArray a) {
                            a.forEach(e -> {
                                JsonElement itemElement = e;
                                if (e instanceof JsonObject o) itemElement = o.get("item");
                                if (itemElement instanceof JsonPrimitive j && j.isString() && BuiltInRegistries.ITEM.containsKey(new ResourceLocation(j.getAsString()))) {
                                    ItemStack i = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(j.getAsString())));
                                    l.displayItems.add(i);
                                    if (e instanceof JsonObject o && o.get("nbt") instanceof JsonPrimitive p && p.isString()) {
                                        try {
                                            i.setTag(TagParser.parseTag(p.getAsString()));
                                        } catch (CommandSyntaxException ex) {
                                            throw new JsonSyntaxException("Invalid nbt tag: " + ex.getMessage());
                                        }
                                    }
                                }
                            });
                        }
                        creativeTabListing.add(l);
                    }
                });

                bufferedReader.close();
                return creativeTabListing;
            } catch (IOException var8) {
                return Collections.emptyList();
            }
        }

        @Override
        protected void apply(List<LegacyCreativeTabListing> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            list.clear();
            list.addAll(object);;
        }
    }
}