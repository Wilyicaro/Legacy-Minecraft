package wily.legacy.client.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.StackIngredient;
import wily.legacy.util.LegacyItemUtil;

import java.util.*;
import java.util.function.Consumer;

public interface CustomRecipeAdder<T> {
    Identifier TIPPED_ARROW = FactoryAPI.createVanillaLocation("tipped_arrow");
    Map<Identifier, CustomRecipeAdder> ID_RECIPE_INFO_OVERRIDES = new HashMap<>(Map.of(TIPPED_ARROW, (validRecipes, recipeAdder) -> {
        BuiltInRegistries.POTION.asHolderIdMap().forEach(p -> {
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
        });
    }));

    void addRecipes(Iterable<RecipeInfo<T>> validRecipes, Consumer<RecipeInfo<T>> recipeAdder);
}
