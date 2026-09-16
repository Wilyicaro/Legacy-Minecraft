package wily.legacy.client.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public record ItemTagRecipeFilter(TagKey<Item> tag) implements RecipeInfoFilter {
    public static final Codec<ItemTagRecipeFilter> CODEC = TagKey.codec(Registries.ITEM).xmap(ItemTagRecipeFilter::new, ItemTagRecipeFilter::tag);

    @Override
    public boolean test(RecipeInfo<?> h) {
        return h.getResultItem().is(tag);
    }

    @Override
    public Codec<? extends RecipeInfoFilter> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "#" + tag.location();
    }
}
