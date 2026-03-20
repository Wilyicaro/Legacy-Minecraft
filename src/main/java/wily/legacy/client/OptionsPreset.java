package wily.legacy.client;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.OptionsScreen;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record OptionsPreset(ResourceLocation id, Optional<Component> name, Optional<Component> tooltip, Map<String, Object> legacyOptions, Map<String, Object> vanillaOptions) implements IdValueInfo<OptionsPreset> {
    public static final Map<String, OptionInstance<?>> VANILLA_OPTIONS_MAP = new HashMap<>();

    public static final Codec<Map<String, Object>> LEGACY_OPTIONS_CODEC = Codec.dispatchedMap(Codec.STRING.validate(s -> LegacyOptions.CLIENT_STORAGE.configMap.containsKey(s) ? DataResult.success(s) : DataResult.error(() -> "Can't find legacy option named as " + s)), s -> LegacyOptions.CLIENT_STORAGE.configMap.get(s).control().codec());
    public static final Codec<Map<String, Object>> VANILLA_OPTIONS_CODEC = Codec.dispatchedMap(Codec.STRING.validate(s -> VANILLA_OPTIONS_MAP.containsKey(s) ? DataResult.success(s) : DataResult.error(() -> "Can't find vanilla option named as " + s)), s -> VANILLA_OPTIONS_MAP.get(s).codec());

    public static final Codec<OptionsPreset> CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(OptionsPreset::id), DynamicUtil.getComponentCodec().optionalFieldOf("name").forGetter(OptionsPreset::name), DynamicUtil.getComponentCodec().optionalFieldOf("tooltip").forGetter(OptionsPreset::tooltip), LEGACY_OPTIONS_CODEC.optionalFieldOf("legacy", Collections.emptyMap()).forGetter(OptionsPreset::legacyOptions), VANILLA_OPTIONS_CODEC.optionalFieldOf("vanilla", Collections.emptyMap()).forGetter(OptionsPreset::vanillaOptions)).apply(i, OptionsPreset::new));
    public static final Codec<OptionHolder<OptionsPreset>> OPTION_CODEC = OptionHolder.createCodec(r -> Legacy4JClient.optionPresetsManager.map().get(r));

    @Override
    public OptionsPreset copyFrom(OptionsPreset other) {
        return new OptionsPreset(id, other.name().or(this::name), other.tooltip().or(this::tooltip), ImmutableMap.<String, Object>builder().putAll(legacyOptions).putAll(other.legacyOptions).build(), ImmutableMap.<String, Object>builder().putAll(vanillaOptions).putAll(other.vanillaOptions).build());
    }

    public void apply() {
        vanillaOptions.forEach((key, value) -> setVanillaOption(VANILLA_OPTIONS_MAP.get(key), value));
        legacyOptions.forEach((key, value) -> setConfig(LegacyOptions.CLIENT_STORAGE.configMap.get(key), value));
        if (Minecraft.getInstance().screen instanceof OptionsScreen screen) {
            screen.updateWidgets(true);
        }
    }

    public boolean isApplied() {
        for (Map.Entry<String, Object> e : vanillaOptions.entrySet()) {
            OptionInstance<?> option = VANILLA_OPTIONS_MAP.get(e.getKey());
            if (option != null && !option.get().equals(e.getValue()))
                return false;
        }

        for (Map.Entry<String, Object> e : legacyOptions.entrySet()) {
            FactoryConfig<?> config = LegacyOptions.CLIENT_STORAGE.configMap.get(e.getKey());
            if (config != null && !config.get().equals(e.getValue()))
                return false;
        }

        return true;
    }

    public void saveIfNeeded() {
        if (!vanillaOptions.isEmpty()) Minecraft.getInstance().options.save();
        if (!legacyOptions.isEmpty()) LegacyOptions.CLIENT_STORAGE.save();
    }

    public void applyAndSave() {
        apply();
        saveIfNeeded();
    }

    public <T> void setVanillaOption(OptionInstance<T> option, Object value) {
        if (option != null) {
            if (option.values() instanceof OptionInstance.CycleableValueSet<T> set) set.valueSetter().set(option, (T) value);
            else option.set((T) value);
        }
    }

    public <T> void setConfig(FactoryConfig<T> config, Object value) {
        if (config != null)
            config.set((T) value);
    }

    @Override
    public boolean isValid() {
        return !legacyOptions.isEmpty() || !vanillaOptions.isEmpty();
    }
}
