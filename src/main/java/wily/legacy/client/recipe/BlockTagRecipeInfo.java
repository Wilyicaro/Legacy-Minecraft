package wily.legacy.client.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public record BlockTagRecipeInfo(TagKey<Block> tag) implements RecipeInfoFilter {
    public static final Codec<BlockTagRecipeInfo> CODEC = TagKey.codec(Registries.BLOCK).xmap(BlockTagRecipeInfo::new, BlockTagRecipeInfo::tag);

    @Override
    public boolean test(RecipeInfo<?> h) {
        return h.getResultItem().getItem() instanceof BlockItem i && i.getBlock().builtInRegistryHolder().is(tag);
    }

    @Override
    public Codec<BlockTagRecipeInfo> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "#block_tag/" + tag.location();
    }
}
