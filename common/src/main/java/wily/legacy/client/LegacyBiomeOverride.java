package wily.legacy.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import wily.legacy.Legacy4J;
import wily.legacy.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LegacyBiomeOverride {
    public static final Map<ResourceLocation,LegacyBiomeOverride> map = new HashMap<>();
    private static final String BIOME_OVERRIDES = "biome_overrides.json";
    public static final ResourceLocation DEFAULT_LOCATION = ResourceLocation.parse("default");
    public static final LegacyBiomeOverride DEFAULT = new LegacyBiomeOverride(){
        public float waterTransparency() {
            return this.waterTransparency == null ? 1.0f : waterTransparency;
        }

    };
    private ItemStack icon = ItemStack.EMPTY;
    Integer waterColor;
    Integer waterFogColor;
    Float waterTransparency;

    public static LegacyBiomeOverride getOrDefault(Optional<ResourceKey<Biome>> optionalKey){
        return optionalKey.isEmpty() ? DEFAULT : getOrDefault(optionalKey.get().location());
    }
    public static LegacyBiomeOverride getOrDefault(ResourceLocation location){
        return map.getOrDefault(location, DEFAULT);
    }
    public LegacyBiomeOverride(){
    }

    public ItemStack icon() {
        return icon;
    }
    public float waterTransparency() {
        return waterTransparency == null ? DEFAULT.waterTransparency() : waterTransparency;
    }
    public Integer waterColor() {
        return waterColor == null ? DEFAULT.waterColor : waterColor;
    }
    public Integer waterFogColor() {
        return waterFogColor == null ? DEFAULT.waterFogColor : waterFogColor;
    }

    public static class Manager extends SimplePreparableReloadListener<Map<ResourceLocation,LegacyBiomeOverride>> {
        @Override
        protected Map<ResourceLocation,LegacyBiomeOverride> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            Map<ResourceLocation,LegacyBiomeOverride> overrides = new HashMap<>();
            overrides.put(DEFAULT_LOCATION,DEFAULT);
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            JsonUtil.getOrderedNamespaces(manager).forEach(name->manager.getResource(ResourceLocation.fromNamespaceAndPath(name,BIOME_OVERRIDES)).ifPresent(r->{
                try {
                    BufferedReader bufferedReader = r.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    JsonElement ioElement = obj.get("overrides");
                    if (ioElement instanceof JsonObject jsonObject)
                        jsonObject.asMap().forEach((s,e)-> {
                            if (e instanceof JsonObject o){
                                LegacyBiomeOverride override = overrides.computeIfAbsent(ResourceLocation.parse(s), resourceLocation-> new LegacyBiomeOverride());
                                override.icon = JsonUtil.getItemFromJson(o,true).get();
                                Integer i;
                                if ((i = JsonUtil.optionalJsonColor(o, "water_color", null)) != null) override.waterColor = i;
                                if ((i = JsonUtil.optionalJsonColor(o, "water_fog_color", null)) != null) override.waterFogColor = i;
                                if (o.get("water_transparency") instanceof JsonPrimitive p && p.isNumber()) override.waterTransparency = p.getAsFloat();
                            }
                            });
                    bufferedReader.close();
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            }));
            return overrides;
        }

        @Override
        protected void apply(Map<ResourceLocation,LegacyBiomeOverride> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            map.clear();
            map.putAll(object);;
        }
    }
}
