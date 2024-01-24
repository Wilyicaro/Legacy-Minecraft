package wily.legacy.client;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import wily.legacy.LegacyMinecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record LegacyWorldTemplate(Component buttonName, ResourceLocation icon, ResourceLocation worldTemplate, String folderName, boolean directJoin) {
    public static final List<LegacyWorldTemplate> list = new ArrayList<>();
    private static final String TEMPLATES = "world_templates.json";
    public static class Manager extends SimplePreparableReloadListener<List<LegacyWorldTemplate>> {
        @Override
        protected List<LegacyWorldTemplate> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            List<LegacyWorldTemplate> templates = new ArrayList<>();
            resourceManager.getNamespaces().forEach(name->{
                resourceManager.getResource(new ResourceLocation(name, TEMPLATES)).ifPresent(r->{
                    try {
                        BufferedReader bufferedReader = r.openAsReader();
                        JsonObject obj = GsonHelper.parse(bufferedReader);
                        obj.asMap().forEach((s,element)->{
                            if (element instanceof JsonObject tabObj) {
                                templates.add(new LegacyWorldTemplate(Component.translatable(s),new ResourceLocation(GsonHelper.getAsString(tabObj,"icon")),new ResourceLocation(GsonHelper.getAsString(tabObj,"templateLocation")),GsonHelper.getAsString(tabObj,"folderName"),GsonHelper.getAsBoolean(tabObj,"directJoin",false)));
                            }
                        });
                        bufferedReader.close();
                    } catch (IOException var8) {
                        LegacyMinecraft.LOGGER.warn(var8.getMessage());
                    }
                });
            });
            return templates;
        }

        @Override
        protected void apply(List<LegacyWorldTemplate> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            list.clear();
            list.addAll(object);;
        }
    }
}