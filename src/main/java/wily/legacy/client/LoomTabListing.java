package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BannerPattern;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyTabButton;

import java.util.*;
import java.util.function.Function;

public record LoomTabListing(ResourceLocation id, Optional<Component> name,
                             Optional<LegacyTabButton.IconHolder<?>> iconHolder,
                             List<ResourceKey<BannerPattern>> patterns) implements LegacyTabInfo<LoomTabListing> {
    public static final Codec<List<ResourceKey<BannerPattern>>> PATTERNS_CODEC = ResourceKey.codec(Registries.BANNER_PATTERN).listOf().xmap(ArrayList::new, Function.identity());
    public static final Codec<LoomTabListing> CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(LoomTabListing::id), DynamicUtil.getComponentCodec().optionalFieldOf("name").forGetter(LoomTabListing::name), LegacyTabButton.ICON_HOLDER_CODEC.optionalFieldOf("icon").forGetter(LoomTabListing::iconHolder), PATTERNS_CODEC.fieldOf("listing").orElseGet(ArrayList::new).forGetter(LoomTabListing::patterns)).apply(i, LoomTabListing::new));

    public static final ResourceLocation SELECT_BANNER = Legacy4J.createModLocation("select_banner");

    public boolean isValid() {
        return LegacyTabInfo.super.isValid() && !patterns.isEmpty() || is(SELECT_BANNER);
    }

    @Override
    public LoomTabListing copyFrom(LoomTabListing otherListing) {
        patterns.addAll(otherListing.patterns);
        return new LoomTabListing(id, otherListing.name.or(this::name), otherListing.iconHolder.or(this::iconHolder), patterns);
    }
}
