package wily.legacy.client.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.util.ListMap;
import wily.legacy.util.IOUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface RecipeInfoFilter extends Predicate<RecipeInfo<?>> {
    ListMap<Identifier, Codec<? extends RecipeInfoFilter>> map = ListMap.<String, Codec<? extends RecipeInfoFilter>>builder().put("id", IdRecipeFilter.CODEC).put("item_tag", ItemTagRecipeFilter.CODEC).put("block_tag", BlockTagRecipeFilter.CODEC).put("item_id", ItemIdRecipeFilter.CODEC).mapKeys(FactoryAPI::createVanillaLocation).build();
    Codec<RecipeInfoFilter> BY_TYPE_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<T> encode(RecipeInfoFilter input, DynamicOps<T> ops, T prefix) {
            return Identifier.CODEC.encodeStart(ops, map.getKey(input.codec())).map(type -> ops.set(prefix, "type", type)).flatMap(r -> ((Codec<RecipeInfoFilter>) input.codec()).encodeStart(ops, input).map(value -> ops.set(r, "value", value)));
        }

        @Override
        public <T> DataResult<Pair<RecipeInfoFilter, T>> decode(DynamicOps<T> ops, T input) {
            Dynamic<T> dynamic = new Dynamic<>(ops, input);
            return dynamic.get("type").flatMap(Identifier.CODEC::parse).flatMap(r -> dynamic.get("value").flatMap(d -> map.get(r).parse(d)).map(f -> Pair.of(f, input)));
        }
    };
    Codec<RecipeInfoFilter> CODEC = IOUtil.createFallbackCodec(BY_TYPE_CODEC, Codec.STRING.xmap(RecipeInfoFilter::parse, RecipeInfoFilter::toString));
    Codec<List<RecipeInfoFilter>> LIST_CODEC = CODEC.listOf().xmap(ArrayList::new, Function.identity());
    Codec<Map<String, List<RecipeInfoFilter>>> LISTING_CODEC = IOUtil.createListingCodec(IOUtil.createFallbackCodec(LIST_CODEC, CODEC.xmap(f -> new ArrayList<>(Collections.singleton(f)), list -> list.get(0))), "group", "recipes", l -> l.get(0).toString());

    static RecipeInfoFilter parse(String s) {
        if (s.startsWith("#"))
            return new ItemTagRecipeFilter(TagKey.create(Registries.ITEM, FactoryAPI.createLocation(s.replaceFirst("#", ""))));
        else if (s.startsWith("result_item/"))
            return new ItemIdRecipeFilter(FactoryAPI.createLocation(s.replaceFirst("result_item/", "")));
        return new IdRecipeFilter(FactoryAPI.createLocation(s));
    }

    default <T> void addRecipes(Iterable<RecipeInfo<T>> validRecipes, Consumer<RecipeInfo<T>> recipeAdder) {
        if (additionMethod() == AdditionMethod.COLLAPSE) {
            List<RecipeInfo<T>> collapsed = CollapsedRecipeInfo.group(this, validRecipes);
            for (RecipeInfo<T> validRecipe : collapsed) {
                if (test(validRecipe)) {
                    recipeAdder.accept(validRecipe);
                }
            }
        } else {
            for (RecipeInfo<T> validRecipe : validRecipes) {
                if (test(validRecipe)) {
                    recipeAdder.accept(validRecipe);
                    if (additionMethod() == AdditionMethod.FIRST_MATCH) return;
                }
            }
        }
    }

    default AdditionMethod additionMethod() {
        return AdditionMethod.ALL;
    }

    Codec<? extends RecipeInfoFilter> codec();

    enum AdditionMethod implements StringRepresentable {
        FIRST_MATCH("first_match"),
        ALL("all"),
        COLLAPSE("collapse");

        public static final EnumCodec<AdditionMethod> CODEC = StringRepresentable.fromEnum(AdditionMethod::values);
        private final String name;

        AdditionMethod(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
