package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.util.IOUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record TypeCraftingTab(ResourceLocation id, Optional<Component> name,
                              Optional<LegacyTabButton.IconHolder<?>> iconHolder,
                              List<CustomTab> tabs) implements LegacyTabInfo<TypeCraftingTab> {
    public static final Codec<TypeCraftingTab> CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(TypeCraftingTab::id), DynamicUtil.getComponentCodec().optionalFieldOf("name").forGetter(TypeCraftingTab::name), LegacyTabButton.ICON_HOLDER_CODEC.optionalFieldOf("icon").forGetter(TypeCraftingTab::iconHolder), CustomTab.LIST_CODEC.fieldOf("tabs").orElseGet(ArrayList::new).forGetter(TypeCraftingTab::tabs)).apply(i, TypeCraftingTab::new));

    public static final ResourceLocation CRAFTING = Legacy4J.createModLocation("crafting");
    public static final ResourceLocation BANNER = Legacy4J.createModLocation("banner");
    public static final ResourceLocation FIREWORK = Legacy4J.createModLocation("firework");
    public static final ResourceLocation DYING = Legacy4J.createModLocation("dying");

    @Override
    public boolean isValid() {
        return iconHolder().isPresent();
    }

    @Override
    public TypeCraftingTab copyFrom(TypeCraftingTab otherListing) {
        tabs.addAll(otherListing.tabs);
        return new TypeCraftingTab(id, otherListing.name.or(this::name), otherListing.iconHolder.or(this::iconHolder), tabs);
    }

    public record CustomTab(LegacyTabButton.IconHolder<?> iconHolder, boolean allows2x2Grid) {
        public static final Codec<CustomTab> EXTENDED_CODEC = RecordCodecBuilder.create(i -> i.group(LegacyTabButton.ICON_HOLDER_CODEC.fieldOf("icon").forGetter(CustomTab::iconHolder), Codec.BOOL.fieldOf("allows2x2Grid").forGetter(CustomTab::allows2x2Grid)).apply(i, CustomTab::new));
        public static final Codec<CustomTab> CODEC = IOUtil.createFallbackCodec(LegacyTabButton.ICON_HOLDER_CODEC.xmap(h -> new CustomTab(h, true), CustomTab::iconHolder), EXTENDED_CODEC);
        public static final Codec<List<CustomTab>> LIST_CODEC = CODEC.listOf().xmap(ArrayList::new, Function.identity());
    }
}
