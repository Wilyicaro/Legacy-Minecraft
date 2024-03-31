package wily.legacy.client;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import wily.legacy.LegacyMinecraftPlatform;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface RecipeValue<C extends Container, T extends Recipe<C>> extends Predicate<RecipeHolder<T>> {
    ResourceLocation TIPPED_ARROW = new ResourceLocation("tipped_arrow");
    ResourceLocation SHULKER_BOX_COLORING = new ResourceLocation("shulker_box_coloring");
    Map<ResourceLocation, IdOverride> ID_RECIPE_VALUES_OVERRIDES = new HashMap<>(Map.of(TIPPED_ARROW, (type, manager, rcps, filter) -> {
        if (rcps.get(rcps.size() -1) instanceof TippedArrowRecipe r) {
            BuiltInRegistries.POTION.forEach(p -> {
                if (p == Potions.EMPTY || (p.getEffects().isEmpty() && p != Potions.WATER)) return;
                ItemStack potion = Items.LINGERING_POTION.getDefaultInstance();
                ItemStack result = new ItemStack(Items.TIPPED_ARROW,8);
                PotionUtils.setPotion(potion, p);
                PotionUtils.setPotion(result, p);
                NonNullList<Ingredient> ings = NonNullList.create();
                for (int i = 0; i < 8; i++) ings.add(Ingredient.of(Items.ARROW.getDefaultInstance()));
                ings.add(4, LegacyMinecraftPlatform.getNBTIngredient(potion));
                Recipe<?> rcp = new ShapelessRecipe(r.getGroup(), r.category(), result, ings);
                if (filter.test(rcp)) rcps.add(rcp);
            });
            rcps.remove(r);
        }
    },SHULKER_BOX_COLORING, (type, manager, rcps, filter) -> {
        if (rcps.get(rcps.size() -1) instanceof ShulkerBoxColoring r) {
            Ingredient shulkerBoxes = Ingredient.of(Stream.concat(Stream.of(Items.SHULKER_BOX.getDefaultInstance()),Arrays.stream(DyeColor.values()).map(c->ShulkerBoxBlock.getBlockByColor(c).asItem().getDefaultInstance())));
            for (DyeColor color : DyeColor.values()) {
                DyeItem d = DyeItem.byColor(color);
                Recipe<?> rcp = new ShapelessRecipe(r.getGroup(), r.category(), ShulkerBoxBlock.getColoredItemStack(d.getDyeColor()), NonNullList.of(Ingredient.EMPTY,shulkerBoxes,Ingredient.of(d)));
                if (filter.test(rcp)) rcps.add(rcp);
            }
            rcps.remove(r);
        }
    }));
    void addRecipes(RecipeType<T> type, RecipeManager manager, List<T> rcps, Predicate<T> filter);
    static <C extends Container, T extends Recipe<C>> RecipeValue<C,T> create(String s){
        if (s.startsWith("#")) {
            String tag = s.replaceFirst("#", "");
            if (tag.startsWith("blocks/")) return new BlockTag<>(TagKey.create(Registries.BLOCK, new ResourceLocation(tag.replaceFirst("blocks/", ""))));
            return new ItemTag<>(TagKey.create(Registries.ITEM, new ResourceLocation(tag.replaceFirst("items/", ""))));
        }else return new Id<>(new ResourceLocation(s));
    }
    interface IdOverride extends RecipeValue{
        @Override
        default boolean test(Object o){
            return true;
        }
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
            RecipeValue<C,T> value = (RecipeValue<C, T>) ID_RECIPE_VALUES_OVERRIDES.get(id);
            if (value != null) value.addRecipes(type,manager,rcps,filter);
        }
        @Override
        public boolean test(RecipeHolder<T> h) {
            return h.id().equals(id);
        }
    }

}
