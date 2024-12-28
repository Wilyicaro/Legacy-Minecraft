package wily.legacy.mixin.base;

import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
//? if >=1.20.5 {
import net.minecraft.world.item.component.FireworkExplosion;
//?}
import net.minecraft.world.item.crafting.FireworkStarRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(FireworkStarRecipe.class)
public interface FireworkStarRecipeAccessor {
    @Accessor(/*? if <1.20.5 {*//*"FLICKER_INGREDIENT"*//*?} else {*/"TWINKLE_INGREDIENT"/*?}*/)
    static Ingredient getTwinkleIngredient() {
        return null;
    }
    @Accessor("TRAIL_INGREDIENT")
    static Ingredient getTrailIngredient() {
        return null;
    }
    @Accessor("GUNPOWDER_INGREDIENT")
    static Ingredient getGunpowderIngredient() {
        return null;
    }
    @Accessor("SHAPE_BY_ITEM")
    static Map<Item, /*? if >=1.20.5 {*/FireworkExplosion.Shape/*?} else {*//*FireworkRocketItem.Shape*//*?}*/> getShapeByItem() {
        return null;
    }
}
