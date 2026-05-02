package wily.legacy.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Map;

public class FireworkCrafting {
    public static Ingredient twinkleIngredient() {
        return Ingredient.of(Items.GLOWSTONE_DUST);
    }

    public static Ingredient trailIngredient() {
        return Ingredient.of(Items.DIAMOND);
    }

    public static Ingredient gunpowderIngredient() {
        return Ingredient.of(Items.GUNPOWDER);
    }

    public static Ingredient paperIngredient() {
        return Ingredient.of(Items.PAPER);
    }

    public static Map<Item, FireworkExplosion.Shape> shapesByItem() {
        return Map.of(
                Items.FIRE_CHARGE, FireworkExplosion.Shape.LARGE_BALL,
                Items.GOLD_NUGGET, FireworkExplosion.Shape.STAR,
                Items.FEATHER, FireworkExplosion.Shape.BURST,
                Items.CREEPER_HEAD, FireworkExplosion.Shape.CREEPER
        );
    }
}
