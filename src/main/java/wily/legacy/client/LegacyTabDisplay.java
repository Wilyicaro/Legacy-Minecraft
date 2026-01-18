package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.client.screen.LegacyTabButton;

import java.util.Optional;

public record LegacyTabDisplay(ResourceLocation id, Optional<Component> name,
                               Optional<LegacyTabButton.IconHolder<?>> iconHolder) implements LegacyTabInfo<LegacyTabDisplay> {
    public static final Codec<LegacyTabDisplay> CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(LegacyTabDisplay::id), DynamicUtil.getComponentCodec().optionalFieldOf("name").forGetter(LegacyTabDisplay::name), LegacyTabButton.ICON_HOLDER_CODEC.optionalFieldOf("icon").forGetter(LegacyTabDisplay::iconHolder)).apply(i, LegacyTabDisplay::new));

    @Override
    public boolean isValid() {
        return iconHolder().isPresent();
    }

    @Override
    public LegacyTabDisplay copyFrom(LegacyTabDisplay otherListing) {
        return new LegacyTabDisplay(id, otherListing.name.or(this::name), otherListing.iconHolder.or(this::iconHolder));
    }
}
