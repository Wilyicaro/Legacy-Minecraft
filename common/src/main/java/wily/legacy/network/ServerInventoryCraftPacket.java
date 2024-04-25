package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import wily.legacy.inventory.RecipeMenu;
import wily.legacy.util.PagedList;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record ServerInventoryCraftPacket(List<Ingredient> ingredients, ItemStack result, int giveSlot, int button, boolean max) implements CommonPacket{
    public ServerInventoryCraftPacket(FriendlyByteBuf buf){
        this(buf.readList(Ingredient::fromNetwork), buf.readItem(), buf.readVarInt(),buf.readVarInt(), buf.readBoolean());
    }
    public ServerInventoryCraftPacket(Recipe<?> rcp, int giveSlot, int button, boolean max){
        this(rcp.getIngredients(),rcp.getResultItem(RegistryAccess.EMPTY), giveSlot,button, max);
    }
    public ServerInventoryCraftPacket(Recipe<?> rcp, boolean max){
        this(rcp,-1,-1,max);
    }
    public ServerInventoryCraftPacket(Recipe<?> rcp, int giveSlot, boolean max){
        this(rcp,giveSlot,-1,max);
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(ingredients,(r,i)->i.toNetwork(r));
        buf.writeItem(result);
        buf.writeVarInt(giveSlot);
        buf.writeVarInt(button);
        buf.writeBoolean(max);
    }
    public static boolean canCraft(List<Ingredient> ingredients, Inventory inventory, ItemStack carriedItem){
        boolean canCraft = true;
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) continue;
            int itemCount = inventory.items.stream().filter(ing).mapToInt(ItemStack::getCount).sum() + (carriedItem.isEmpty() || !ing.test(carriedItem) ? 0 : carriedItem.getCount());
            long ingCount = ingredients.stream().filter(ingredient-> !ingredient.isEmpty() && ingredient.equals(ing)).count();
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
            while (canCraft(ingredients, sp.getInventory(), sp.containerMenu.getCarried()) && ((max && tries <= result.getMaxStackSize() * 100) || tries == 0)) {
                tries++;
                ingredients.forEach(ing -> {
                    if (!sp.containerMenu.getCarried().isEmpty() && ing.test(sp.containerMenu.getCarried())) {
                        sp.containerMenu.getCarried().shrink(1);
                        return;
                    }
                    for (int i = 0; i < sp.containerMenu.slots.size(); i++) {
                        Slot slot = sp.containerMenu.getSlot(i);
                        if (slot.container != sp.getInventory() || !slot.hasItem() || !ing.test(slot.getItem())) continue;
                        sp.getInventory().getItem(slot.getContainerSlot()).shrink(1);
                        //sp.connection.send(new ClientboundContainerSetSlotPacket(sp.containerMenu.containerId, sp.containerMenu.incrementStateId(), slot.index, s));
                        break;
                    }
                });
                Slot s;
                if (giveSlot >= 0 && (!(s = sp.containerMenu.slots.get(giveSlot)).hasItem() || ItemStack.isSameItemSameTags(s.getItem(),result) && s.getItem().getCount() + result.getCount() <= result.getMaxStackSize())){
                    if (s.hasItem()) s.getItem().grow(result.getCount());
                    else s.set(result);
                }else sp.getInventory().placeItemBackInInventory(result.copy());
                if (sp.containerMenu instanceof RecipeMenu m) m.onCraft(sp,button,ingredients,result);
            }
        }
    }
}
