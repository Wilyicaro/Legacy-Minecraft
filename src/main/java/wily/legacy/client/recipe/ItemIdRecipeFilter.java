package wily.legacy.client.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import wily.legacy.util.IOUtil;

public record ItemIdRecipeFilter(Identifier id, boolean onlyFirstMatch) implements RecipeInfoFilter {
    public static final Codec<ItemIdRecipeFilter> EXTENDED_CODEC = RecordCodecBuilder.create(i -> i.group(Identifier.CODEC.fieldOf("id").forGetter(ItemIdRecipeFilter::id), Codec.BOOL.fieldOf("onlyFirstMatch").forGetter(ItemIdRecipeFilter::onlyFirstMatch)).apply(i, ItemIdRecipeFilter::new));
    public static final Codec<ItemIdRecipeFilter> CODEC = IOUtil.createFallbackCodec(EXTENDED_CODEC, Identifier.CODEC.xmap(ItemIdRecipeFilter::new, ItemIdRecipeFilter::id));

    public ItemIdRecipeFilter(Identifier id) {
        this(id, true);
    }

    @Override
    public boolean test(RecipeInfo<?> h) {
        return BuiltInRegistries.ITEM.getKey(h.getResultItem().getItem()).equals(id);
    }

    @Override
    public Codec<? extends RecipeInfoFilter> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "result_item/" + id;
    }
}
