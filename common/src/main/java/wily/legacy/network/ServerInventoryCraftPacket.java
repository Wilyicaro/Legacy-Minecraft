package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import wily.legacy.util.PagedList;

import java.util.List;
import java.util.function.Supplier;

public record ServerInventoryCraftPacket(List<Ingredient> ingredients, ItemStack result, int button, boolean max) implements CommonPacket{
    public ServerInventoryCraftPacket(FriendlyByteBuf buf){
        this(buf.readList(Ingredient::fromNetwork), buf.readItem(), buf.readVarInt(), buf.readBoolean());
    }
    public ServerInventoryCraftPacket(Recipe<?> rcp, int button, boolean max){
        this(rcp.getIngredients(),rcp.getResultItem(RegistryAccess.EMPTY),button, max);
    }
    public ServerInventoryCraftPacket(Recipe<?> rcp, boolean max){
        this(rcp,-1,max);
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(ingredients,(r,i)->i.toNetwork(r));
        buf.writeItem(result);
        buf.writeVarInt(button);
        buf.writeBoolean(max);
    }
    public static boolean canCraft(List<Ingredient> ingredients, Inventory inventory){
        boolean canCraft = true;
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) continue;
            int itemCount = inventory.items.stream().filter(ing).mapToInt(ItemStack::getCount).sum();
            long ingCount = ingredients.stream().filter(ingredient-> ingredient == ing).count();
            if (itemCount < ingCount && PagedList.occurrenceOf(ingredients,ing,i) >= itemCount) {
                canCraft = false;
                break;
            }
        }
        return canCraft;
    }
    @Override
    public void apply(Supplier<NetworkManager.PacketContext> ctx) {
        if (ctx.get().getPlayer() instanceof ServerPlayer sp){
            int tries = 0;
            if (ingredients.isEmpty()) return;
            while (canCraft(ingredients, sp.getInventory()) && ((max && tries <= result.getMaxStackSize() * 100) || tries == 0)) {
                tries++;
                ingredients.forEach(ing -> {
                    for (int i = 0; i < sp.containerMenu.slots.size(); i++) {
                        Slot slot = sp.containerMenu.slots.get(i);
                        if (slot.container != sp.getInventory() || !slot.hasItem() || !ing.test(slot.getItem())) continue;
                        sp.getInventory().getItem(slot.getContainerSlot()).shrink(1);
                        //sp.connection.send(new ClientboundContainerSetSlotPacket(sp.containerMenu.containerId, sp.containerMenu.incrementStateId(), slot.index, s));
                        break;
                    }
                });
                sp.getInventory().placeItemBackInInventory(result.copy());
                sp.containerMenu.clickMenuButton(sp,button);
            }
        }
    }
}
