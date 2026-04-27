package wily.legacy.mixin.base;

import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
//? if >=1.20.5 {
import net.minecraft.world.item.component.FireworkExplosion;
//?}
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Map;

public interface FireworkStarRecipeAccessor {
    static Ingredient getTwinkleIngredient() {
        return Ingredient.of(Items.GLOWSTONE_DUST);
    }

    static Ingredient getTrailIngredient() {
        return Ingredient.of(Items.DIAMOND);
    }

    static Ingredient getGunpowderIngredient() {
        return Ingredient.of(Items.GUNPOWDER);
    }

    static Map<Item, /*? if >=1.20.5 {*/FireworkExplosion.Shape/*?} else {*//*FireworkRocketItem.Shape*//*?}*/> getShapeByItem() {
        return Map.of(
                Items.FIRE_CHARGE, FireworkExplosion.Shape.LARGE_BALL,
                Items.GOLD_NUGGET, FireworkExplosion.Shape.STAR,
                Items.FEATHER, FireworkExplosion.Shape.BURST,
                Items.CREEPER_HEAD, FireworkExplosion.Shape.CREEPER
        );
    }
}
