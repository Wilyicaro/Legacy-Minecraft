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
        main: for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) continue;
            for (int i = 0; i < LegacyIngredient.of(ing).getCount(); i++) {
                Optional<ItemStack> match = compactList.stream().filter(item -> !item.isEmpty() && ing.test(item.copyWithCount(1))).findFirst();
                if (match.isEmpty()) {
                    canCraft = false;
                    break main;
                } else match.get().shrink(1);
            }
        }
        return canCraft;
    }
    default List<ItemStack> getRemainingItems(Player player, ServerMenuCraftPacket packet){
        return Collections.emptyList();
    }

    default void tryCraft(Player player, ServerMenuCraftPacket packet){
        int tries = 0;
        List<Ingredient> ingredients;
        if ((ingredients = getIngredients(player,packet)).isEmpty()) return;
        while (canCraft(ingredients,player.getInventory(),player.containerMenu.getCarried()) && ((packet.max() && tries <= 64 * 36) || tries == 0)) {
            tries++;
            setupActualItems(player,packet,null,-1);
            for (int index = 0; index < ingredients.size(); index++) {
                Ingredient ing = ingredients.get(index);
                if (ing.isEmpty()) continue;
                int count = LegacyIngredient.of(ing).getCount();;
                for (int c = 0; c < count; c++) {
                    if (!player.containerMenu.getCarried().isEmpty() && ing.test(player.containerMenu.getCarried().copyWithCount(1))) {
                        if (c == count - 1) setupActualItems(player,packet,player.containerMenu.getCarried().copyWithCount(count),index);
                        player.containerMenu.getCarried().split(1);
                        continue;
                    }
                    for (int i = 0; i < player.containerMenu.slots.size(); i++) {
                        Slot slot = player.containerMenu.getSlot(i);
                        if (slot.container != player.getInventory() || !slot.hasItem() || !ing.test(slot.getItem().copyWithCount(1))) continue;
                        ItemStack item = player.getInventory().getItem(slot.getContainerSlot());
                        if (c == count - 1) setupActualItems(player,packet,item.copyWithCount(count),index);
                        item.shrink(1);
                        break;
                    }
                }
            };
            ItemStack result = getResult(player,packet);
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
    default void setupActualItems(Player player, ServerMenuCraftPacket packet, ItemStack setItem, int index){
    }
}
