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
import net.minecraft.world.level.biome.Biome;
import wily.legacy.LegacyMinecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record LegacyBiomeOverride(Integer waterColor, Integer waterFogColor, float waterTransparency) {
    public static final Map<ResourceLocation,LegacyBiomeOverride> map = new HashMap<>();
    private static final String BIOME_OVERRIDES = "biome_overrides.json";
    public static Integer DEFAULT_WATER_COLOR;

    public static final LegacyBiomeOverride EMPTY = new LegacyBiomeOverride(null,null,1.0f);

    public static LegacyBiomeOverride getOrDefault(Optional<ResourceKey<Biome>> optionalKey){
        return optionalKey.isEmpty() ? EMPTY : getOrDefault(optionalKey.get().location());
    }
    public static LegacyBiomeOverride getOrDefault(ResourceLocation location){
        return map.getOrDefault(location, getDefault());
    }
    public static LegacyBiomeOverride getDefault(){
        return getDefault(map);
    }
    public static LegacyBiomeOverride getDefault(Map<ResourceLocation,LegacyBiomeOverride> map){
        return map.getOrDefault(new ResourceLocation("default"), EMPTY);
    }
    public static class Manager extends SimplePreparableReloadListener<Map<ResourceLocation,LegacyBiomeOverride>> {
        @Override
        protected Map<ResourceLocation,LegacyBiomeOverride> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            Map<ResourceLocation,LegacyBiomeOverride> overrides = new HashMap<>();
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            DEFAULT_WATER_COLOR = null;
            manager.getNamespaces().forEach(name->manager.getResource(new ResourceLocation(name,BIOME_OVERRIDES)).ifPresent(r->{
                try {
                    BufferedReader bufferedReader = r.openAsReader();
                    JsonObject obj = GsonHelper.parse(bufferedReader);
                    JsonElement ioElement = obj.get("overrides");
                    if (ioElement instanceof JsonObject jsonObject)
                        jsonObject.asMap().forEach((s,e)-> {
                            if (e instanceof JsonObject o)
                                overrides.put(new ResourceLocation(s),new LegacyBiomeOverride(optionalJsonColor(o,"water_color",getDefault(overrides).waterColor), optionalJsonColor(o,"water_fog_color",getDefault(overrides).waterFogColor),GsonHelper.getAsFloat(o,"water_transparency", getDefault(overrides).waterTransparency)));
                        });
                    bufferedReader.close();
                } catch (IOException exception) {
                    LegacyMinecraft.LOGGER.warn(exception.getMessage());
                }
            }));
            return overrides;
        }

        private Integer optionalJsonColor(JsonObject o, String s, Integer fallback) {
            if (o.get(s) instanceof JsonPrimitive p){
                if (p.isString() && p.getAsString().startsWith("#")) return Integer.parseInt(p.getAsString().substring(1),16);
                return p.getAsInt();
            }
            return fallback;
        }

        @Override
        protected void apply(Map<ResourceLocation,LegacyBiomeOverride> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            map.clear();
            map.putAll(object);;
        }
    }
}
