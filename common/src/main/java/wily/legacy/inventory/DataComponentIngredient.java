/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package wily.legacy.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import wily.legacy.Legacy4J;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Ingredient that matches the given items, performing either a {@link DataComponentIngredient#isStrict() strict} or a partial NBT test.
 * <p>
 * Strict NBT ingredients will only match items that have <b>exactly</b> the provided tag, while partial ones will
 * match if the item's tags contain all the elements of the provided one, while allowing for additional elements to exist.
 */
public class DataComponentIngredient extends Ingredient implements LegacyIngredient {
    public static final MapCodec<DataComponentIngredient> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                            HolderSetCodec.create(Registries.ITEM, BuiltInRegistries.ITEM.holderByNameCodec(), false).fieldOf("items").forGetter(DataComponentIngredient::items),
                            DataComponentPredicate.CODEC.fieldOf("components").forGetter(DataComponentIngredient::components),
                            Codec.BOOL.optionalFieldOf("strict", false).forGetter(DataComponentIngredient::isStrict),
                            Codec.INT.optionalFieldOf("count",1).forGetter(DataComponentIngredient::getCount))
                    .apply(builder, DataComponentIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf,DataComponentIngredient> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,"data_component_ingredient");
    private final HolderSet<Item> items;
    private final DataComponentPredicate components;
    private final boolean strict;
    private final ItemStack[] stacks;
    private final int count;

    public DataComponentIngredient(HolderSet<Item> items, DataComponentPredicate components, boolean strict, int count) {
        super(Stream.empty());
        this.items = items;
        this.components = components;
        this.strict = strict;
        this.stacks = items.stream().map(i -> new ItemStack(i, count, components.asPatch())).filter(i-> !i.isEmpty()).toArray(ItemStack[]::new);
        this.count = count;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (strict) {
            for (ItemStack stack2 : this.stacks) {
                if (ItemStack.isSameItemSameComponents(stack, stack2)) return true;
            }
            return false;
        } else {
            return this.items.contains(stack.getItemHolder()) && this.components.test(stack);
        }
    }

    @Override
    public boolean isEmpty() {
        return stacks.length == 0;
    }

    @Override
    public ItemStack[] getItems() {
        return stacks;
    }


    public HolderSet<Item> items() {
        return items;
    }

    public DataComponentPredicate components() {
        return components;
    }

    public boolean isStrict() {
        return strict;
    }
    /**
     * Creates a new ingredient matching the given item, containing the given components and the item stack count
     */
    public static DataComponentIngredient of(boolean strict, ItemStack stack) {
        return of(strict, stack.getComponents(), stack.getCount(), stack.getItem());
    }

    /**
     * Creates a new ingredient matching the given item, containing the given components
     */
    public static DataComponentIngredient of(boolean strict, ItemStack stack, int count) {
        return of(strict, stack.getComponents(), count, stack.getItem());
    }

    /**
     * Creates a new ingredient matching any item from the list, containing the given components
     */
    public static <T> DataComponentIngredient of(boolean strict, DataComponentType<? super T> type, T value, int count, ItemLike... items) {
        return of(strict, DataComponentPredicate.builder().expect(type, value).build(), count, items);
    }

    /**
     * Creates a new ingredient matching any item from the list, containing the given components
     */
    public static <T> DataComponentIngredient of(boolean strict, Supplier<? extends DataComponentType<? super T>> type, T value, int count, ItemLike... items) {
        return of(strict, type.get(), value, count, items);
    }

    /**
     * Creates a new ingredient matching any item from the list, containing the given components
     */
    public static DataComponentIngredient of(boolean strict, DataComponentMap map, int count, ItemLike... items) {
        return of(strict, DataComponentPredicate.allOf(map), count,items);
    }

    /**
     * Creates a new ingredient matching any item from the list, containing the given components
     */
    @SafeVarargs
    public static DataComponentIngredient of(boolean strict, DataComponentMap map, int count, Holder<Item>... items) {
        return of(strict, DataComponentPredicate.allOf(map), count,items);
    }

    /**
     * Creates a new ingredient matching any item from the list, containing the given components
     */
    public static DataComponentIngredient of(boolean strict, DataComponentMap map, HolderSet<Item> items, int count) {
        return of(strict, DataComponentPredicate.allOf(map), items, count);
    }

    /**
     * Creates a new ingredient matching any item from the list, containing the given components
     */
    @SafeVarargs
    public static DataComponentIngredient of(boolean strict, DataComponentPredicate predicate, int count, Holder<Item>... items) {
        return of(strict, predicate, HolderSet.direct(items), count);
    }

    /**
     * Creates a new ingredient matching any item from the list, containing the given components
     */
    public static DataComponentIngredient of(boolean strict, DataComponentPredicate predicate, int count, ItemLike... items) {
        return of(strict, predicate, HolderSet.direct(Arrays.stream(items).map(ItemLike::asItem).map(Item::builtInRegistryHolder).toList()), count);
    }

    /**
     * Creates a new ingredient matching any item from the list, containing the given components
     */
    public static DataComponentIngredient of(boolean strict, DataComponentPredicate predicate, HolderSet<Item> items, int count) {
        return new DataComponentIngredient(items, predicate, strict,count);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public int getCount() {
        return count;
    }
}
