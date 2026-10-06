package wily.legacy.client.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import wily.legacy.util.IOUtil;

public record ItemIdRecipeFilter(Identifier id, AdditionMethod additionMethod) implements RecipeInfoFilter {
    public static final Codec<ItemIdRecipeFilter> EXTENDED_CODEC = RecordCodecBuilder.create(i -> i.group(Identifier.CODEC.fieldOf("id").forGetter(ItemIdRecipeFilter::id), AdditionMethod.CODEC.fieldOf("additionMethod").forGetter(ItemIdRecipeFilter::additionMethod)).apply(i, ItemIdRecipeFilter::new));
    public static final Codec<ItemIdRecipeFilter> CODEC = IOUtil.createFallbackCodec(EXTENDED_CODEC, Identifier.CODEC.xmap(ItemIdRecipeFilter::new, ItemIdRecipeFilter::id));

    public ItemIdRecipeFilter(Identifier id) {
        this(id, AdditionMethod.COLLAPSE);
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
