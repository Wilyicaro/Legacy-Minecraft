package wily.legacy.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import wily.factoryapi.FactoryAPI;
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
    public static final ResourceLocation DEFAULT_LOCATION = FactoryAPI.createVanillaLocation("default");
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

    public int getWaterARGBOrDefault(int defaultColor){
        return (int) (waterTransparency()*255) << 24 | (waterColor() == null ? defaultColor : waterColor()) & 16777215;
    }
    public Integer waterFogColor() {
        return waterFogColor == null ? DEFAULT.waterFogColor : waterFogColor;
    }

    public static class Manager implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            map.clear();
            map.put(DEFAULT_LOCATION,DEFAULT);
            JsonUtil.getOrderedNamespaces(resourceManager).forEach(name->resourceManager.getResource(FactoryAPI.createLocation(name,BIOME_OVERRIDES)).ifPresent(r->{
                try {
                    BufferedReader bufferedReader = r.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    JsonElement ioElement = obj.get("overrides");
                    if (ioElement instanceof JsonObject jsonObject)
                        jsonObject.asMap().forEach((s,e)-> {
                            if (e instanceof JsonObject o){
                                LegacyBiomeOverride override = map.computeIfAbsent(FactoryAPI.createLocation(s), resourceLocation-> new LegacyBiomeOverride());
                                JsonUtil.getItemFromJson(o,true).ifPresent(i->override.icon = i);
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
        }

        @Override
        public String getName() {
            return "legacy:biome_overrides";
        }
    }
}
