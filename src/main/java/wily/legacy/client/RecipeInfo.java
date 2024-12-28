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
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
//? if >1.20.1 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?}
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.world.level.block.Block;
//? if <1.21.2 {
/*import net.minecraft.world.level.block.SuspiciousEffectHolder;
*///?}
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.RegisterListing;
import wily.factoryapi.base.StackIngredient;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface RecipeInfo<T> extends RegisterListing.Holder<T> {
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

    static <T> RecipeInfo<T> create(ResourceKey<Recipe<?>> id, T value, List<Optional<Ingredient>> ings, ItemStack result) {
        return create(id.location(), value, ings, result, ()-> null);
    }

    static <T> RecipeInfo<T> create(ResourceLocation id, T value, List<Optional<Ingredient>> ings, ItemStack result) {
        return create(id, value, ings, result, ()-> null);
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
                return super.equals(obj) || obj instanceof /*? if >1.20.1 {*/RecipeHolder<?>/*?} else {*//*Recipe<?>*//*?}*/ h && h./*? if >1.20.1 {*/id()/*?} else {*//*getId()*//*?}*/.equals(getId()) || obj instanceof RecipeInfo<?> h1 && h1.getId().equals(getId());
            }
        };
    }

    interface Filter extends Predicate<RecipeInfo<?>> {
        ListMap<ResourceLocation, Codec<? extends Filter>> map = ListMap.<String, Codec<? extends Filter>>builder().put("id",Id.CODEC).put("item_tag",Id.CODEC).put("block_tag",BlockTag.CODEC).put("item_id",ItemId.CODEC).mapKeys(FactoryAPI::createVanillaLocation).build();
        Codec<Filter> BY_TYPE_CODEC = new Codec<>() {
            @Override
            public <T> DataResult<T> encode(Filter input, DynamicOps<T> ops, T prefix) {
                ResourceLocation.CODEC.fieldOf("type").codec().encode(map.getKey(input.codec()),ops,prefix);
                return ((Codec<Filter>)input.codec()).fieldOf("value").codec().encode(input, ops, prefix);
            }

            @Override
            public <T> DataResult<Pair<Filter, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> dynamic = new Dynamic<>(ops,input);
                return dynamic.get("type").flatMap(ResourceLocation.CODEC::parse).flatMap(r-> dynamic.get("value").flatMap(d-> map.get(r).parse(d)).map(f->Pair.of(f, input)));
            }
        };
        Codec<Filter> CODEC = Codec.either(BY_TYPE_CODEC,Codec.STRING.xmap(Filter::parse,Filter::toString)).xmap(e->e.right().orElseGet(e.left()::get),Either::right);
        Codec<List<Filter>> LIST_CODEC = CODEC.listOf().xmap(ArrayList::new, Function.identity());
        Codec<Map<String,List<Filter>>> LISTING_CODEC = createListingCodec(Codec.either(CODEC,LIST_CODEC).xmap(e->e.right().orElseGet(()->new ArrayList<>(Collections.singleton(e.left().get()))),Either::right),"group","recipes", l->l.get(0).toString());

        static <E> Codec<Map<String,E>> createListingCodec(Codec<E> codec, String keyField, String valueField, Function<E,String> fallBackKey){
            return new Codec<>() {
                @Override
                public <T> DataResult<Pair<Map<String, E>, T>> decode(DynamicOps<T> ops, T input) {
                    Dynamic<T> dynamic = new Dynamic<>(ops,input);
                    DataResult<Map<String, E>> defaultMap = dynamic.asMapOpt().map(s->s.collect(Collectors.toMap(p->p.getFirst().asString().result().orElseThrow(), p->codec.parse(p.getSecond()).result().orElseThrow(),(a,b)->b,LinkedHashMap::new)));
                    return (defaultMap.result().isPresent() ? defaultMap : dynamic.asListOpt(d-> {
                        E element = codec.parse(d).result().orElseGet(()->d.get(valueField).flatMap(codec::parse).resultOrPartial(Legacy4J.LOGGER::error).get());
                        return Pair.of(d.get(keyField).asString().result().orElse(fallBackKey.apply(element)),element);
                    }).map(l->l.stream().collect(Collectors.toMap(Pair::getFirst,Pair::getSecond,(a,b)->b,LinkedHashMap::new)))).map(m->Pair.of(m,input));
                }

                @Override
                public <T> DataResult<T> encode(Map<String, E> input, DynamicOps<T> ops, T prefix) {
                    Dynamic<T> dynamic = new Dynamic<>(ops,prefix);
                    input.forEach((s,e)-> codec.encodeStart(ops,e).result().ifPresent(r->dynamic.set(s,new Dynamic<>(ops,r))));
                    return DataResult.success(prefix);
                }
            };
        }

        ResourceLocation TIPPED_ARROW = FactoryAPI.createVanillaLocation("tipped_arrow");
        //? if <1.21.2
        /*ResourceLocation SUSPICIOUS_STEW = FactoryAPI.createVanillaLocation("suspicious_stew");*/
        Map<ResourceLocation, IdOverride> ID_RECIPE_INFO_OVERRIDES = new HashMap<>(Map.of(TIPPED_ARROW, (validRecipes, recipeAdder) -> BuiltInRegistries.POTION.asHolderIdMap().forEach(p -> {
            if (p.value().getEffects().isEmpty() && !p./*? if <1.20.5 {*//*value().*//*?}*/equals(Potions.WATER)) return;
            ItemStack potion = Legacy4J.setItemStackPotion(Items.LINGERING_POTION.getDefaultInstance(),p);
            ItemStack result = Legacy4J.setItemStackPotion(new ItemStack(Items.TIPPED_ARROW,8),p);
            List<Optional<Ingredient>> ings = new ArrayList<>();
            Optional<Ingredient> arrowOptional = Optional.of(Ingredient.of(Items.ARROW));
            for (int i = 0; i < 8; i++) ings.add(arrowOptional);
            ings.add(4, Optional.of(StackIngredient.of(true, potion)));
            List<Component> description = new ArrayList<>();
            recipeAdder.accept(RecipeInfo.create(TIPPED_ARROW, null, ings, result, ()-> {
                description.clear();
                Legacy4J.addPotionTooltip(p, description, 1.0F/*? if >=1.20.3 {*/, Minecraft.getInstance().level.tickRateManager().tickrate()/*?}*/);
                return description.get(0);
            }));
        })/*? if <1.21.2 {*//*,SUSPICIOUS_STEW,(validRecipes, recipeAdder)-> BuiltInRegistries.ITEM.getTag(ItemTags.SMALL_FLOWERS).ifPresent(s->s.forEach(h->{
            ItemStack result = Items.SUSPICIOUS_STEW.getDefaultInstance();
            SuspiciousEffectHolder suspiciousEffectHolder = SuspiciousEffectHolder.tryGet(h.value());
            if (suspiciousEffectHolder != null) {
                //? if <=1.20.1 {
                /^SuspiciousStewItem.saveMobEffect(result, suspiciousEffectHolder.getSuspiciousEffect(), suspiciousEffectHolder.getEffectDuration());
                ^///?} else if <1.20.5 {
                /^SuspiciousStewItem.saveMobEffects(result, suspiciousEffectHolder.getSuspiciousEffects());
                ^///?} else {
                result.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, suspiciousEffectHolder.getSuspiciousEffects());
                //?}
            }
            recipeAdder.accept(RecipeInfo.create(SUSPICIOUS_STEW,null,List.of(Optional.of(Ingredient.of(Items.BROWN_MUSHROOM)),Optional.of(Ingredient.of(Items.RED_MUSHROOM)),Optional.of(Ingredient.of(h.value())),Optional.of(Ingredient.of(Items.BOWL))),result));
        }))*//*?}*/));

        default <T> void addRecipes(Iterable<RecipeInfo<T>> validRecipes, Consumer<RecipeInfo<T>> recipeAdder) {
            for (RecipeInfo<T> validRecipe : validRecipes) {
                if (test(validRecipe)) {
                    recipeAdder.accept(validRecipe);
                    if (onlyFirstMatch()) return;
                }
            }
        }

        default boolean onlyFirstMatch(){
            return false;
        }

        Codec<? extends Filter> codec();

        static Filter parse(String s){
            if (s.startsWith("#")) return new ItemTag(TagKey.create(Registries.ITEM, FactoryAPI.createLocation(s.replaceFirst("#", ""))));
            return new Id(FactoryAPI.createLocation(s));
        }

        @Deprecated
        static Filter create(String s){
            if (s.startsWith("#blocks/")) return new BlockTag(TagKey.create(Registries.BLOCK, FactoryAPI.createLocation(s.replaceFirst("#blocks/", ""))));
            return parse(s);
        }

        interface IdOverride<T> {
            void addRecipes(Iterable<RecipeInfo<T>> validRecipes, Consumer<RecipeInfo<T>> recipeAdder);
        }

        record BlockTag(TagKey<Block> tag) implements Filter {
            public static final Codec<BlockTag> CODEC = TagKey.codec(Registries.BLOCK).xmap(BlockTag::new,BlockTag::tag);
            @Override
            public boolean test(RecipeInfo<?> h) {
                return h.getResultItem().getItem() instanceof BlockItem i && i.getBlock().builtInRegistryHolder().is(tag);
            }

            @Override
            public Codec<Filter> codec() {
                return null;
            }
            @Override
            public String toString() {
                return "#block_tag"+tag.location();
            }
        }
        record ItemTag(TagKey<Item> tag) implements Filter {
            public static final Codec<ItemTag> CODEC = TagKey.codec(Registries.ITEM).xmap(ItemTag::new,ItemTag::tag);
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
                return "#"+tag.location();
            }
        }
        record Id(ResourceLocation id) implements Filter {
            public static final Codec<Id> CODEC = ResourceLocation.CODEC.xmap(Id::new,Id::id);
            @Override
            public <T> void addRecipes(Iterable<RecipeInfo<T>> validRecipes, Consumer<RecipeInfo<T>> recipeAdder) {
                Filter.super.addRecipes(validRecipes,recipeAdder);
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
            public static final Codec<ItemId> EXTENDED_CODEC = RecordCodecBuilder.create(i->i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(ItemId::id),Codec.BOOL.fieldOf("onlyFirstMatch").forGetter(ItemId::onlyFirstMatch)).apply(i,ItemId::new));
            public static final Codec<ItemId> CODEC = Codec.either(ResourceLocation.CODEC.xmap(ItemId::new,ItemId::id),EXTENDED_CODEC).xmap(e->e.right().orElseGet(e.left()::get), Either::right);

            public ItemId(ResourceLocation id){
                this(id,true);
            }
            @Override
            public boolean test(RecipeInfo<?> h) {
                return BuiltInRegistries.ITEM.getKey(h.getResultItem().getItem()).equals(id);
            }

            @Override
            public Codec<? extends Filter> codec() {
                return EXTENDED_CODEC;
            }

            @Override
            public String toString() {
                return "result_item/"+id;
            }
        }

    }
}
