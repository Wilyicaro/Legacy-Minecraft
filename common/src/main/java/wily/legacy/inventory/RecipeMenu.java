package wily.legacy.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public interface RecipeMenu {
    void onCraft(Player player, int buttonInfo, List<Ingredient> ingredients, ItemStack result);
}
