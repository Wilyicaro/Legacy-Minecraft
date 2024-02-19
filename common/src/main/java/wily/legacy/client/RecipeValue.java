package wily.legacy.client;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Predicate;

public interface RecipeValue<C extends Container, T extends Recipe<C>> extends Predicate<RecipeHolder<T>> {
    void addRecipes(RecipeType<T> type, RecipeManager manager, List<T> rcps, Predicate<T> filter);
    static <C extends Container, T extends Recipe<C>> RecipeValue<C,T> create(String s){
        if (s.startsWith("#")) {
            String tag = s.replaceFirst("#", "");
            if (tag.startsWith("blocks/")) return new BlockTag<>(TagKey.create(Registries.BLOCK, new ResourceLocation(tag.replaceFirst("blocks/", ""))));
            return new ItemTag<>(TagKey.create(Registries.ITEM, new ResourceLocation(tag.replaceFirst("items/", ""))));
        }else return new Id<>(new ResourceLocation(s));
    }

    interface AnyMatch<C extends Container, T extends Recipe<C>> extends RecipeValue<C,T>{
        @Override
        default void addRecipes(RecipeType<T> type, RecipeManager manager, List<T> rcps, Predicate<T> filter) {
            manager.getAllRecipesFor(type).stream().filter(h->h.value().getType() == type && filter.test(h.value()) && test(h)).forEach(h-> rcps.add(h.value()));
        }
    }
    record BlockTag<C extends Container, T extends Recipe<C>>(TagKey<Block> tag) implements AnyMatch<C,T>{
        @Override
        public boolean test(RecipeHolder<T> h) {
            return h.value().getResultItem(RegistryAccess.EMPTY).getItem() instanceof BlockItem i && i.getBlock().builtInRegistryHolder().is(tag);
        }
    }
    record ItemTag<C extends Container, T extends Recipe<C>>(TagKey<Item> tag) implements AnyMatch<C,T>{
        @Override
        public boolean test(RecipeHolder<T> h) {
            return h.value().getResultItem(RegistryAccess.EMPTY).is(tag);
        }
    }
    record Id<C extends Container, T extends Recipe<C>>(ResourceLocation id) implements RecipeValue<C,T>{
        @Override
        public void addRecipes(RecipeType<T> type, RecipeManager manager, List<T> rcps, Predicate<T> filter) {
            manager.byKey(id).ifPresent(h->{ if (h.value().getType() == type && filter.test((T)h.value())) rcps.add((T)h.value());});
        }
        @Override
        public boolean test(RecipeHolder<T> h) {
            return h.id().equals(id);
        }
    }

}
