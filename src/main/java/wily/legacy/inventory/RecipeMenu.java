package wily.legacy.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import wily.factoryapi.base.FactoryIngredient;
import wily.factoryapi.util.FactoryItemUtil;
import wily.legacy.network.ServerMenuCraftPayload;

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
        compactList.stream().filter(i -> FactoryItemUtil.equalItems(i, item)).findFirst().ifPresentOrElse(i -> i.grow(item.getCount()), () -> compactList.add(item.copy()));
    }

    static boolean canCraft(List<Optional<Ingredient>> ingredients, Inventory inventory, ItemStack carriedItem){
        boolean canCraft = true;
        List<ItemStack> compactList = new ArrayList<>();
        handleCompactInventoryList(compactList,inventory,carriedItem);
        main: for (Optional<Ingredient> ing : ingredients) {
            if (ing.isEmpty()) continue;
            for (int i = 0; i < FactoryIngredient.of(ing.get()).getCount(); i++) {
                Optional<ItemStack> match = compactList.stream().filter(item -> !item.isEmpty() && ing.get().test(item.copyWithCount(1))).findFirst();
                if (match.isEmpty()) {
                    canCraft = false;
                    break main;
                } else match.get().shrink(1);
            }
        }
        return canCraft;
    }
    default List<ItemStack> getRemainingItems(Player player, ServerMenuCraftPayload packet){
        return Collections.emptyList();
    }

    default void tryCraft(Player player, ServerMenuCraftPayload packet){
        int tries = 0;
        List<Optional<Ingredient>> ingredients;
        if ((ingredients = getIngredients(player,packet)).isEmpty()) return;
        while (canCraft(ingredients,player.getInventory(),player.containerMenu.getCarried()) && ((packet.max() && tries <= 64 * 36) || tries == 0)) {
            tries++;
            setupActualItems(player,packet,null,-1);
            for (int index = 0; index < ingredients.size(); index++) {
                Optional<Ingredient> ing = ingredients.get(index);
                if (ing.isEmpty()) continue;
                int count = FactoryIngredient.of(ing.get()).getCount();;
                for (int c = 0; c < count; c++) {
                    ItemStack copy;
                    if (!player.containerMenu.getCarried().isEmpty() &&  ing.get().test((copy = player.containerMenu.getCarried().copyWithCount(1)))) {
                        if (c == count - 1) setupActualItems(player,packet,copy,index);
                        player.containerMenu.getCarried().shrink(1);
                        continue;
                    }
                    for (int i = 0; i < player.containerMenu.slots.size(); i++) {
                        Slot slot = player.containerMenu.getSlot(i);
                        if (slot.container != player.getInventory() || !slot.hasItem() || !ing.get().test((copy = slot.getItem().copyWithCount(1)))) continue;
                        if (c == count - 1) setupActualItems(player,packet,copy,index);
                        slot.getItem().shrink(1);
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
    void onCraft(Player player, ServerMenuCraftPayload packet, ItemStack result);
    ItemStack getResult(Player player, ServerMenuCraftPayload packet);
    default List<Optional<Ingredient>> getIngredients(Player player, ServerMenuCraftPayload packet){
        return packet.customIngredients();
    }
    default void setupActualItems(Player player, ServerMenuCraftPayload packet, ItemStack setItem, int index){
    }
}
