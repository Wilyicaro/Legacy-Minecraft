package wily.legacy.client;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
//? if >1.20.1 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?}
//? if >=1.20.5 {
//?}
import net.minecraft.world.level.block.Block;
//? if <1.21.2 {
/*import net.minecraft.world.level.block.SuspiciousEffectHolder;
 *///?}
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.RegisterListing;
import wily.factoryapi.base.StackIngredient;
import wily.factoryapi.util.ListMap;
import wily.legacy.util.IOUtil;
import wily.legacy.util.LegacyItemUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface RecipeInfo<T> extends RegisterListing.Holder<T> {
    static <T> RecipeInfo<T> create(ResourceKey<Recipe<?>> id, T value, List<Optional<Ingredient>> ings, ItemStack result) {
        return create(id.location(), value, ings, result, () -> null);
    }

    static <T> RecipeInfo<T> create(ResourceLocation id, T value, List<Optional<Ingredient>> ings, ItemStack result) {
        return create(id, value, ings, result, () -> null);
    }

    static <T> RecipeInfo<T> create(ResourceLocation id, T value, List<Optional<Ingredient>> ings, ItemStack result, Supplier<Component> description) {
        return create(id, value, ings, result, result.getHoverName(), description);
    }

    static <T> RecipeInfo<T> create(ResourceLocation id, T value, List<Optional<Ingredient>> ings, ItemStack result, Component name, Supplier<Component> description) {
        return new RecipeInfo<>() {

            @Override
            public T get() {
                return value;
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public List<Optional<Ingredient>> getOptionalIngredients() {
                return ings;
            }

            public ItemStack getResultItem() {
                return result;
            }

            @Override
            public Component getName() {
                return name;
            }

            @Override
            public Component getDescription() {
                return description.get();
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj) || obj instanceof RecipeHolder<?> h && h.id().equals(getId()) || obj instanceof RecipeInfo<?> h1 && h1.getId().equals(getId());
            }
        };
    }

    default boolean isInvalid() {
        return get() instanceof Recipe<?> r && r.isSpecial() || getOptionalIngredients().isEmpty() || getResultItem().isEmpty();
    }

    default boolean isOverride() {
        return get() == null;
    }

    List<Optional<Ingredient>> getOptionalIngredients();

    ItemStack getResultItem();

    Component getName();

    Component getDescription();

    interface Filter extends Predicate<RecipeInfo<?>> {
        ListMap<ResourceLocation, Codec<? extends Filter>> map = ListMap.<String, Codec<? extends Filter>>builder().put("id", Id.CODEC).put("item_tag", ItemTag.CODEC).put("block_tag", BlockTag.CODEC).put("item_id", ItemId.CODEC).mapKeys(FactoryAPI::createVanillaLocation).build();
        Codec<Filter> BY_TYPE_CODEC = new Codec<>() {
            @Override
            public <T> DataResult<T> encode(Filter input, DynamicOps<T> ops, T prefix) {
                return ResourceLocation.CODEC.encodeStart(ops, map.getKey(input.codec())).map(type -> ops.set(prefix, "type", type)).flatMap(r ->  ((Codec<Filter>) input.codec()).encodeStart(ops, input).map(value -> ops.set(r, "value", value)));
            }

            @Override
            public <T> DataResult<Pair<Filter, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> dynamic = new Dynamic<>(ops, input);
                return dynamic.get("type").flatMap(ResourceLocation.CODEC::parse).flatMap(r -> dynamic.get("value").flatMap(d -> map.get(r).parse(d)).map(f -> Pair.of(f, input)));
            }
        };
        Codec<Filter> CODEC = IOUtil.createFallbackCodec(BY_TYPE_CODEC, Codec.STRING.xmap(Filter::parse, Filter::toString));
        Codec<List<Filter>> LIST_CODEC = CODEC.listOf().xmap(ArrayList::new, Function.identity());
        Codec<Map<String, List<Filter>>> LISTING_CODEC = IOUtil.createListingCodec(IOUtil.createFallbackCodec(LIST_CODEC, CODEC.xmap(f -> new ArrayList<>(Collections.singleton(f)), list -> list.get(0))), "group", "recipes", l -> l.get(0).toString());

        ResourceLocation TIPPED_ARROW = FactoryAPI.createVanillaLocation("tipped_arrow");

        Map<ResourceLocation, IdOverride> ID_RECIPE_INFO_OVERRIDES = new HashMap<>(Map.of(TIPPED_ARROW, (validRecipes, recipeAdder) -> BuiltInRegistries.POTION.asHolderIdMap().forEach(p -> {
            if (p.value().getEffects().isEmpty() && !p.equals(Potions.WATER)) return;
            ItemStack potion = LegacyItemUtil.setItemStackPotion(Items.LINGERING_POTION.getDefaultInstance(), p);
            ItemStack result = LegacyItemUtil.setItemStackPotion(new ItemStack(Items.TIPPED_ARROW, 8), p);
            List<Optional<Ingredient>> ings = new ArrayList<>();
            Optional<Ingredient> arrowOptional = Optional.of(Ingredient.of(Items.ARROW));
            for (int i = 0; i < 8; i++) ings.add(arrowOptional);
            ings.add(4, Optional.of(StackIngredient.of(true, potion)));
            List<Component> description = new ArrayList<>();
            recipeAdder.accept(RecipeInfo.create(TIPPED_ARROW, null, ings, result, () -> {
                description.clear();
                LegacyItemUtil.addPotionTooltip(p, description, 0.125F/*? if >=1.20.3 {*/, Minecraft.getInstance().level.tickRateManager().tickrate()/*?}*/);
                return description.get(0);
            }));
        })));

        static Filter parse(String s) {
            if (s.startsWith("#"))
                return new ItemTag(TagKey.create(Registries.ITEM, FactoryAPI.createLocation(s.replaceFirst("#", ""))));
            else if (s.startsWith("result_item/"))
                return new ItemId(FactoryAPI.createLocation(s.replaceFirst("result_item/", "")));
            return new Id(FactoryAPI.createLocation(s));
        }

        default <T> void addRecipes(Iterable<RecipeInfo<T>> validRecipes, Consumer<RecipeInfo<T>> recipeAdder) {
            for (RecipeInfo<T> validRecipe : validRecipes) {
                if (test(validRecipe)) {
                    recipeAdder.accept(validRecipe);
                    if (onlyFirstMatch()) return;
                }
            }
        }

        default boolean onlyFirstMatch() {
            return false;
        }

        Codec<? extends Filter> codec();

        interface IdOverride<T> {
            void addRecipes(Iterable<RecipeInfo<T>> validRecipes, Consumer<RecipeInfo<T>> recipeAdder);
        }

        record BlockTag(TagKey<Block> tag) implements Filter {
            public static final Codec<BlockTag> CODEC = TagKey.codec(Registries.BLOCK).xmap(BlockTag::new, BlockTag::tag);

            @Override
            public boolean test(RecipeInfo<?> h) {
                return h.getResultItem().getItem() instanceof BlockItem i && i.getBlock().builtInRegistryHolder().is(tag);
            }

            @Override
            public Codec<BlockTag> codec() {
                return CODEC;
            }

            @Override
            public String toString() {
                return "#block_tag/" + tag.location();
            }
        }

        record ItemTag(TagKey<Item> tag) implements Filter {
            public static final Codec<ItemTag> CODEC = TagKey.codec(Registries.ITEM).xmap(ItemTag::new, ItemTag::tag);

            @Override
            public boolean test(RecipeInfo<?> h) {
                return h.getResultItem().is(tag);
            }

            @Override
            public Codec<? extends Filter> codec() {
                return CODEC;
            }

            @Override
            public String toString() {
                return "#" + tag.location();
            }
        }

        record Id(ResourceLocation id) implements Filter {
            public static final Codec<Id> CODEC = ResourceLocation.CODEC.xmap(Id::new, Id::id);

            @Override
            public <T> void addRecipes(Iterable<RecipeInfo<T>> validRecipes, Consumer<RecipeInfo<T>> recipeAdder) {
                Filter.super.addRecipes(validRecipes, recipeAdder);
                IdOverride<T> value = ID_RECIPE_INFO_OVERRIDES.get(id);
                if (value != null) value.addRecipes(validRecipes, recipeAdder);
            }

            @Override
            public boolean onlyFirstMatch() {
                return true;
            }

            @Override
            public Codec<? extends Filter> codec() {
                return CODEC;
            }

            @Override
            public boolean test(RecipeInfo<?> h) {
                return h.getId().equals(id);
            }

            @Override
            public String toString() {
                return id.toString();
            }
        }

        record ItemId(ResourceLocation id, boolean onlyFirstMatch) implements Filter {
            public static final Codec<ItemId> EXTENDED_CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(ItemId::id), Codec.BOOL.fieldOf("onlyFirstMatch").forGetter(ItemId::onlyFirstMatch)).apply(i, ItemId::new));
            public static final Codec<ItemId> CODEC = IOUtil.createFallbackCodec(EXTENDED_CODEC, ResourceLocation.CODEC.xmap(ItemId::new, ItemId::id));

            public ItemId(ResourceLocation id) {
                this(id, true);
            }

            @Override
            public boolean test(RecipeInfo<?> h) {
                return BuiltInRegistries.ITEM.getKey(h.getResultItem().getItem()).equals(id);
            }

            @Override
            public Codec<? extends Filter> codec() {
                return CODEC;
            }

            @Override
            public String toString() {
                return "result_item/" + id;
            }
        }

    }
}
