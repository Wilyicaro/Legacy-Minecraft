package wily.legacy.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import wily.legacy.Legacy4JPlatform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public interface RecipeValue<C extends RecipeInput, T extends Recipe<C>> extends Predicate<RecipeHolder<T>> {
    ResourceLocation TIPPED_ARROW = ResourceLocation.withDefaultNamespace("tipped_arrow");
    Map<ResourceLocation, IdOverride> ID_RECIPE_VALUES_OVERRIDES = new HashMap<>(Map.of(TIPPED_ARROW, (type, manager, rcps, filter) -> {
        manager.byKey(TIPPED_ARROW).ifPresent(h->{
            if (h.value() instanceof TippedArrowRecipe r) {
                BuiltInRegistries.POTION.holders().forEach(p -> {
                    if (p.value().getEffects().isEmpty() && p != Potions.WATER) return;
                    ItemStack potion = Items.LINGERING_POTION.getDefaultInstance();
                    ItemStack result = new ItemStack(Items.TIPPED_ARROW,8);
                    PotionContents contents = new PotionContents(p);
                    potion.set(DataComponents.POTION_CONTENTS,contents);
                    result.set(DataComponents.POTION_CONTENTS,contents);
                    NonNullList<Ingredient> ings = NonNullList.create();
                    for (int i = 0; i < 8; i++) ings.add(Ingredient.of(Items.ARROW.getDefaultInstance()));
                    ings.add(4, Legacy4JPlatform.getStrictComponentsIngredient(potion));
                    RecipeHolder<?> rcp = new RecipeHolder<>(h.id(),overrideCustomRecipe(r,ings,result));
                    if (filter.test(rcp)) rcps.add(rcp);
                });
            }
        });
    }));
    static CustomRecipe overrideCustomRecipe(CustomRecipe recipe, NonNullList<Ingredient> ings, ItemStack result){
        return new CustomRecipe(recipe.category()) {
            @Override
            public boolean matches(CraftingInput RecipeInput, Level level) {
                return recipe.matches(RecipeInput,level);
            }

            @Override
            public NonNullList<Ingredient> getIngredients() {
                return ings;
            }
            @Override
            public ItemStack getResultItem(HolderLookup.Provider provider) {
                return result;
            }

            @Override
            public ItemStack assemble(CraftingInput RecipeInput, HolderLookup.Provider provider) {
                return recipe.assemble(RecipeInput,provider);
            }

            @Override
            public boolean canCraftInDimensions(int i, int j) {
                return recipe.canCraftInDimensions(i,j);
            }

            @Override
            public RecipeSerializer<?> getSerializer() {
                return recipe.getSerializer();
            }
        };
    }

    void addRecipes(RecipeType<T> type, RecipeManager manager, List<RecipeHolder<T>> rcps, Predicate<RecipeHolder<T>> filter);
    static <I extends RecipeInput, T extends Recipe<I>> RecipeValue<I,T> create(String s){
        if (s.startsWith("#")) {
            String tag = s.replaceFirst("#", "");
            if (tag.startsWith("blocks/")) return new BlockTag<>(TagKey.create(Registries.BLOCK, ResourceLocation.parse(tag.replaceFirst("blocks/", ""))));
            return new ItemTag<>(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag.replaceFirst("items/", ""))));
        }else return new Id<>(ResourceLocation.parse(s));
    }
    interface IdOverride extends RecipeValue{
        @Override
        default boolean test(Object o){
            return true;
        }
    }

    interface AnyMatch<C extends RecipeInput, T extends Recipe<C>> extends RecipeValue<C,T>{
        @Override
        default void addRecipes(RecipeType<T> type, RecipeManager manager, List<RecipeHolder<T>> rcps, Predicate<RecipeHolder<T>> filter) {
            manager.getAllRecipesFor(type).stream().filter(h->h.value().getType() == type && filter.test(h) && test(h)).forEach(rcps::add);
        }
    }
    record BlockTag<C extends RecipeInput, T extends Recipe<C>>(TagKey<Block> tag) implements AnyMatch<C,T>{
        @Override
        public boolean test(RecipeHolder<T> h) {
            return h.value().getResultItem(RegistryAccess.EMPTY).getItem() instanceof BlockItem i && i.getBlock().builtInRegistryHolder().is(tag);
        }
    }
    record ItemTag<C extends RecipeInput, T extends Recipe<C>>(TagKey<Item> tag) implements AnyMatch<C,T>{
        @Override
        public boolean test(RecipeHolder<T> h) {
            return h.value().getResultItem(RegistryAccess.EMPTY).is(tag);
        }
    }
    record Id<C extends RecipeInput, T extends Recipe<C>>(ResourceLocation id) implements RecipeValue<C,T>{
        @Override
        public void addRecipes(RecipeType<T> type, RecipeManager manager, List<RecipeHolder<T>> rcps, Predicate<RecipeHolder<T>> filter) {
            manager.byKey(id).ifPresent(h->{ if (h.value().getType() == type && filter.test((RecipeHolder<T>) h)) rcps.add((RecipeHolder<T>) h);});
            RecipeValue<C,T> value = (RecipeValue<C, T>) ID_RECIPE_VALUES_OVERRIDES.get(id);
            if (value != null) value.addRecipes(type,manager,rcps,filter);
        }
        @Override
        public boolean test(RecipeHolder<T> h) {
            return h.id().equals(id);
        }
    }

}
