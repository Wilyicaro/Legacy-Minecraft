package wily.legacy.inventory;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.stream.Stream;

public class ImpossibleIngredient extends Ingredient {
    private final ItemStack[] stacks;
    public ImpossibleIngredient(ItemStack stack) {
        super(/*? if <1.21.2 {*//*Stream.empty()*//*?} else {*/HolderSet.direct(stack.getItemHolder())/*?}*/);
        stacks = new ItemStack[]{stack};
    }
    //? if <1.21.2 {
    /*@Override
    public boolean isEmpty() {
        return false;
    }
    @Override
    public ItemStack[] getItems() {
        return stacks;
    }
    *///?}
    @Override
    public boolean test(ItemStack itemStack) {
        return false;
    }
}
