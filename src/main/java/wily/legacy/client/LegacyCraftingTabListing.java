package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.client.screen.LegacyTabButton;

import java.util.*;

public record LegacyCraftingTabListing(ResourceLocation id, Optional<Component> name,
                                       Optional<LegacyTabButton.IconHolder<?>> iconHolder,
                                       Map<String, List<RecipeInfo.Filter>> craftings) implements LegacyTabInfo<LegacyCraftingTabListing> {
    public static final Codec<LegacyCraftingTabListing> CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(LegacyCraftingTabListing::id), DynamicUtil.getComponentCodec().optionalFieldOf("name").forGetter(LegacyCraftingTabListing::name), LegacyTabButton.ICON_HOLDER_CODEC.optionalFieldOf("icon").forGetter(LegacyCraftingTabListing::iconHolder), RecipeInfo.Filter.LISTING_CODEC.fieldOf("listing").orElseGet(LinkedHashMap::new).forGetter(LegacyCraftingTabListing::craftings)).apply(i, LegacyCraftingTabListing::new));

    @Override
    public boolean isValid() {
        return LegacyTabInfo.super.isValid() && !craftings.isEmpty();
    }

    @Override
    public LegacyCraftingTabListing copyFrom(LegacyCraftingTabListing otherListing) {
        otherListing.craftings.forEach((s, f) -> {
            if (craftings.containsKey(s)) craftings.get(s).addAll(f);
            else craftings.put(s, f);
        });
        return new LegacyCraftingTabListing(id, otherListing.name.or(this::name), otherListing.iconHolder.or(this::iconHolder), otherListing.craftings);
    }
}
