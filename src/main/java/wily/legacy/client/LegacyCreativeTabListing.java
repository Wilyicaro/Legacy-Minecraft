package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyTabButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record LegacyCreativeTabListing(ResourceLocation id, Optional<Component> name,
                                       Optional<LegacyTabButton.IconHolder<?>> iconHolder,
                                       List<ArbitrarySupplier<ItemStack>> displayItems) implements LegacyTabInfo<LegacyCreativeTabListing> {
    public static final Codec<LegacyCreativeTabListing> CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(LegacyCreativeTabListing::id), DynamicUtil.getComponentCodec().optionalFieldOf("name").forGetter(LegacyCreativeTabListing::name), LegacyTabButton.ICON_HOLDER_CODEC.optionalFieldOf("icon").forGetter(LegacyCreativeTabListing::iconHolder), DynamicUtil.ITEM_SUPPLIER_CODEC.listOf().fieldOf("listing").orElseGet(ArrayList::new).forGetter(LegacyCreativeTabListing::displayItems)).apply(i, LegacyCreativeTabListing::new));
    public static final ResourceLocation SEARCH = Legacy4J.createModLocation("search");

    public static void rebuildVanillaCreativeTabsItems(Minecraft minecraft) {
        if (minecraft.getConnection() != null && CreativeModeTabs.tryRebuildTabContents(minecraft.getConnection().enabledFeatures(), minecraft.options.operatorItemsTab().get(), minecraft.getConnection().registryAccess())) {
            List<ItemStack> list = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
            minecraft.getConnection().searchTrees().updateCreativeTooltips(minecraft.getConnection().registryAccess(), list);
            minecraft.getConnection().searchTrees().updateCreativeTags(list);
        }
    }

    @Override
    public boolean isValid() {
        return LegacyTabInfo.super.isValid() && !is(SEARCH);
    }

    @Override
    public LegacyCreativeTabListing copyFrom(LegacyCreativeTabListing otherListing) {
        displayItems.addAll(otherListing.displayItems);
        return new LegacyCreativeTabListing(id, otherListing.name.or(this::name), otherListing.iconHolder.or(this::iconHolder), displayItems);
    }
}