package wily.legacy.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import wily.legacy.Legacy4J;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record LegacyWorldTemplate(Component buttonMessage, ResourceLocation icon, ResourceLocation worldTemplate, String folderName, boolean directJoin, boolean isLocked) {
    public static final Codec<LegacyWorldTemplate> CODEC = RecordCodecBuilder.create(i -> i.group(ComponentSerialization.CODEC.fieldOf("buttonMessage").forGetter(LegacyWorldTemplate::buttonMessage), ResourceLocation.CODEC.fieldOf("icon").forGetter(LegacyWorldTemplate::icon), ResourceLocation.CODEC.fieldOf("templateLocation").forGetter(LegacyWorldTemplate::worldTemplate), Codec.STRING.fieldOf("folderName").forGetter(LegacyWorldTemplate::folderName),Codec.BOOL.fieldOf("directJoin").orElse(false).forGetter(LegacyWorldTemplate::directJoin),Codec.BOOL.fieldOf("isLocked").orElse(true).forGetter(LegacyWorldTemplate::isLocked)).apply(i, LegacyWorldTemplate::new));

    public static final List<LegacyWorldTemplate> list = new ArrayList<>();
    private static final String TEMPLATES = "world_templates.json";
    public static class Manager extends SimplePreparableReloadListener<List<LegacyWorldTemplate>> {
        @Override
        protected List<LegacyWorldTemplate> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            List<LegacyWorldTemplate> templates = new ArrayList<>();
            JsonUtil.getOrderedNamespaces(resourceManager).forEach(name->{
                resourceManager.getResource(ResourceLocation.fromNamespaceAndPath(name, TEMPLATES)).ifPresent(r->{
                    try (BufferedReader bufferedReader = r.openAsReader()) {
                        JsonElement element = JsonParser.parseReader(bufferedReader);
                        if (element instanceof JsonObject obj) {
                            Legacy4J.LOGGER.warn("The World Template {} is using a deprecated syntax, please contact this resource creator or try updating it.", name+":"+TEMPLATES);
                            obj.asMap().forEach((s, e) -> {
                                if (e instanceof JsonObject tabObj) {
                                    templates.add(new LegacyWorldTemplate(Component.translatable(s), ResourceLocation.parse(GsonHelper.getAsString(tabObj, "icon")), ResourceLocation.parse(GsonHelper.getAsString(tabObj, "templateLocation")), GsonHelper.getAsString(tabObj, "folderName"), GsonHelper.getAsBoolean(tabObj, "directJoin", false), true));
                                }
                            });
                        } else if (element instanceof JsonArray a) a.forEach(e->CODEC.parse(JsonOps.INSTANCE,e).result().ifPresent(templates::add));
                    } catch (IOException var8) {
                        Legacy4J.LOGGER.warn(var8.getMessage());
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