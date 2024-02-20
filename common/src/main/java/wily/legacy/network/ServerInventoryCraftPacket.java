package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import wily.legacy.util.PagedList;

import java.util.List;
import java.util.function.Supplier;

public record ServerInventoryCraftPacket(List<Ingredient> ingredients, ItemStack result, int button, int minSlot, int maxSlot) implements CommonPacket{
    public ServerInventoryCraftPacket(FriendlyByteBuf buf){
        this(buf.readList(Ingredient::fromNetwork), buf.readItem(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
    public ServerInventoryCraftPacket(Recipe<?> rcp, int button, int minSlot, int maxSlot){
        this(rcp.getIngredients(),rcp.getResultItem(RegistryAccess.EMPTY),button, minSlot, maxSlot);
    }
    public ServerInventoryCraftPacket(Recipe<?> rcp, int minSlot, int maxSlot){
        this(rcp,-1,minSlot,maxSlot);
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(ingredients,(r,i)->i.toNetwork(r));
        buf.writeItem(result);
        buf.writeVarInt(button);
        buf.writeVarInt(minSlot);
        buf.writeVarInt(maxSlot);
    }
    public static boolean canCraft(List<Ingredient> ingredients, Inventory inventory){
        boolean canCraft = true;
        for (int i1 = 0; i1 < ingredients.size(); i1++) {
            Ingredient ing = ingredients.get(i1);
            if (ing.isEmpty()) continue;
            int itemCount = inventory.items.stream().filter(ing).mapToInt(ItemStack::getCount).sum();
            long ingCount = ingredients.stream().filter(i-> i == ing).count();
            if (itemCount < ingCount && PagedList.occurrenceOf(ingredients,ing,i1) >= itemCount) {
                canCraft = false;
                break;
            }
        }
        return canCraft;
    }
    @Override
    public void apply(Supplier<NetworkManager.PacketContext> ctx) {
        if (ctx.get().getPlayer() instanceof ServerPlayer sp){
            if (canCraft(ingredients, sp.getInventory())) {
                ingredients.forEach(ing -> {
                    for (int i1 = 0; i1 < sp.containerMenu.slots.size(); i1++) {
                        ItemStack s = sp.containerMenu.slots.get(i1).getItem();
                        if (s.isEmpty() || !ing.test(s)) continue;
                        s.shrink(1);
                        sp.containerMenu.slots.get(i1).set(s);
                        break;
                    }
                });
                sp.getInventory().placeItemBackInInventory(result);
                sp.containerMenu.clickMenuButton(sp,button);
            }
        }
    }
}
