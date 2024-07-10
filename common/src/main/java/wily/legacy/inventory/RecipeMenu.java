package wily.legacy.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import wily.legacy.network.ServerMenuCraftPacket;

import java.util.*;

public interface RecipeMenu {
    static void handleCompactInventoryList(Collection<ItemStack> compactList, Inventory inventory, ItemStack carriedItem){
        handleCompactItemStackList(compactList,inventory.items);
        if (!carriedItem.isEmpty()) handleCompactItemStackListAdd(compactList,carriedItem);
    }
    static void handleCompactItemStackList(Collection<ItemStack> compactList, Iterable<ItemStack> items){
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            handleCompactItemStackListAdd(compactList,item);
        }
    }

    static void handleCompactItemStackListAdd(Collection<ItemStack> compactList, ItemStack item){
        compactList.stream().filter(i -> ItemStack.isSameItemSameComponents(i, item)).findFirst().ifPresentOrElse(i -> i.grow(item.getCount()), () -> compactList.add(item.copy()));
    }

    static boolean canCraft(List<Ingredient> ingredients, Inventory inventory, ItemStack carriedItem){
        boolean canCraft = true;
        List<ItemStack> compactList = new ArrayList<>();
        handleCompactInventoryList(compactList,inventory,carriedItem);
        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;
            Optional<ItemStack> match = compactList.stream().filter(item -> !item.isEmpty() && ing.test(item.copyWithCount(1))).findFirst();
            if (match.isEmpty()) {
                canCraft = false;
                break;
            } else match.get().shrink(1);
        }
        return canCraft;
    }
    default List<ItemStack> getRemainingItems(Player player, ServerMenuCraftPacket packet){
        return Collections.emptyList();
    }

    default void tryCraft(Player player, ServerMenuCraftPacket packet){
        int tries = 0;
        ItemStack result;
        List<Ingredient> ingredients;
        if ((ingredients = getIngredients(player,packet)).isEmpty() || (result = getResult(player,packet)).isEmpty()) return;
        while (canCraft(ingredients,player.getInventory(),player.containerMenu.getCarried()) && ((packet.max() && tries <= result.getMaxStackSize() * 36) || tries == 0)) {
            tries++;
            ingredients.forEach(ing -> {
                if (!player.containerMenu.getCarried().isEmpty() && ing.test(player.containerMenu.getCarried().copyWithCount(1))) {
                    player.containerMenu.getCarried().shrink(1);
                    return;
                }
                for (int i = 0; i < player.containerMenu.slots.size(); i++) {
                    Slot slot = player.containerMenu.getSlot(i);
                    if (slot.container != player.getInventory() || !slot.hasItem() || !ing.test(slot.getItem().copyWithCount(1))) continue;
                    ItemStack item = player.getInventory().getItem(slot.getContainerSlot());
                    item.shrink(1);
                    break;
                }
            });
            onCraft(player, packet, result);
            getRemainingItems(player,packet).forEach(player.getInventory()::placeItemBackInInventory);
            player.getInventory().placeItemBackInInventory(result.copy());
        }
    }
    void onCraft(Player player, ServerMenuCraftPacket packet, ItemStack result);
    ItemStack getResult(Player player, ServerMenuCraftPacket packet);
    default List<Ingredient> getIngredients(Player player, ServerMenuCraftPacket packet){
        return packet.customIngredients();
    }
}
