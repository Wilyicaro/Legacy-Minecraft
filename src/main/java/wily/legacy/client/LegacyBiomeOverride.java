package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.util.IOUtil;

import java.util.List;
import java.util.Optional;

public record LegacyBiomeOverride(ResourceLocation id, Optional<Component> name, Optional<ItemStack> item,
                                  Optional<Integer> waterColor, Optional<Integer> waterFogColor,
                                  Optional<Integer> fogColor, Optional<Integer> skyColor,
                                  Optional<Float> waterTransparency,
                                  Optional<Float> waterFogDistance) implements IdValueInfo<LegacyBiomeOverride> {
    public static final ResourceLocation DEFAULT_LOCATION = FactoryAPI.createVanillaLocation("default");
    public static final Codec<LegacyBiomeOverride> CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(LegacyBiomeOverride::id), DynamicUtil.getComponentCodec().optionalFieldOf("name").forGetter(LegacyBiomeOverride::name), DynamicUtil.ITEM_CODEC.optionalFieldOf("item").forGetter(LegacyBiomeOverride::item), CommonColor.INT_COLOR_CODEC.optionalFieldOf("water_color").forGetter(LegacyBiomeOverride::waterColor), CommonColor.INT_COLOR_CODEC.optionalFieldOf("water_fog_color").forGetter(LegacyBiomeOverride::waterFogColor), CommonColor.INT_COLOR_CODEC.optionalFieldOf("fog_color").forGetter(LegacyBiomeOverride::fogColor), CommonColor.INT_COLOR_CODEC.optionalFieldOf("sky_color").forGetter(LegacyBiomeOverride::skyColor), Codec.FLOAT.optionalFieldOf("water_transparency").forGetter(LegacyBiomeOverride::waterTransparency), Codec.FLOAT.optionalFieldOf("water_fog_distance").forGetter(LegacyBiomeOverride::waterFogDistance)).apply(i, LegacyBiomeOverride::new));
    public static final Codec<List<LegacyBiomeOverride>> LIST_MAP_CODEC = IOUtil.createListIdMapCodec(CODEC, "id").fieldOf("overrides").codec();

    public LegacyBiomeOverride(ResourceLocation id) {
        this(id, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static LegacyBiomeOverride getDefault() {
        return Legacy4JClient.legacyBiomeOverrides.map().computeIfAbsent(DEFAULT_LOCATION, LegacyBiomeOverride::new);
    }

    public static LegacyBiomeOverride getOrDefault(Optional<ResourceKey<Biome>> optionalKey) {
        return optionalKey.isEmpty() ? getDefault() : getOrDefault(optionalKey.get().location());
    }

    public static LegacyBiomeOverride getOrDefault(ResourceLocation location) {
        return Legacy4JClient.legacyBiomeOverrides.map().getOrDefault(location, getDefault());
    }

    public ItemStack icon() {
        return item.orElse(ItemStack.EMPTY);
    }

    @Override
    public Optional<Float> waterTransparency() {
        return waterTransparency.or(() -> getDefault().waterTransparency);
    }

    public float getWaterTransparency() {
        return waterTransparency().orElse(1.0f);
    }

    public Optional<Integer> waterColor() {
        return waterColor.or(() -> getDefault().waterColor);
    }

    public int getWaterARGBOrDefault(int defaultColor) {
        return (int) (getWaterTransparency() * 255) << 24 | (waterColor().orElse(defaultColor)) & 16777215;
    }

    public Optional<Integer> waterFogColor() {
        return waterFogColor.or(() -> getDefault().waterFogColor);
    }

    public Optional<Integer> fogColor() {
        return fogColor.or(() -> getDefault().fogColor);
    }

    public Optional<Integer> skyColor() {
        return skyColor.or(() -> getDefault().skyColor);
    }

    public Optional<Float> waterFogDistance() {
        return waterFogDistance.or(() -> getDefault().waterFogDistance);
    }

    @Override
    public LegacyBiomeOverride copyFrom(LegacyBiomeOverride other) {
        return new LegacyBiomeOverride(id, other.name.or(this::name), other.item.or(this::item), other.waterColor.or(this::waterColor), other.waterFogColor.or(this::waterFogColor), other.fogColor.or(this::fogColor), other.skyColor.or(this::skyColor), other.waterTransparency.or(this::waterTransparency), other.waterFogDistance.or(this::waterFogDistance));
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
