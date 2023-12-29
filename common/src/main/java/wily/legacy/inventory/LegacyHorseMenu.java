package wily.legacy.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import wily.legacy.LegacyMinecraft;

public class LegacyHorseMenu extends AbstractContainerMenu {
    public static final ResourceLocation SADDLE_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/saddle_slot");
    public static final ResourceLocation LLAMA_ARMOR_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/llama_armor_slot");
    public static final ResourceLocation ARMOR_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/armor_slot");
    private final Container horseContainer;
    public final AbstractHorse horse;
    public LegacyHorseMenu(int windowId, Inventory inventory, Container container, final AbstractHorse abstractHorse) {
        super(null, windowId);
        this.horseContainer = container;
        this.horse = abstractHorse;
        this.addSlot(new LegacySlotWrapper(container, 0, 14, 21){
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.SADDLE) && !this.hasItem() && abstractHorse.isSaddleable();
            }

            @Override
            public ResourceLocation getIconSprite() {
                return getItem().isEmpty() ? SADDLE_SLOT_SPRITE : null;
            }

            @Override
            public boolean isActive() {
                return abstractHorse.isSaddleable();
            }
        });
        this.addSlot(new LegacySlotWrapper(container, 1, 14, 42){

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return abstractHorse.isArmor(itemStack);
            }

            @Override
            public boolean isActive() {
                return abstractHorse.canWearArmor();
            }
            @Override
            public ResourceLocation getIconSprite() {
                return getItem().isEmpty() ? horse instanceof Llama ? LLAMA_ARMOR_SLOT_SPRITE : ARMOR_SLOT_SPRITE : null;
            }
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        if (abstractHorse instanceof AbstractChestedHorse h && h.hasChest()) {
            for (int l = 0; l < 3; ++l) {
                for (int m = 0; m < h.getInventoryColumns(); ++m) {
                    this.addSlot(new LegacySlotWrapper(container, 2 + m + l * h.getInventoryColumns(), 98 + m * 21, 21 + l * 21));
                }
            }
        }
        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new LegacySlotWrapper(inventory, j + (i + 1) * 9, 14 + j * 21, 104 + i * 21));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new LegacySlotWrapper(inventory, i, 14 + i * 21, 174));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.horse.hasInventoryChanged(this.horseContainer) && this.horseContainer.stillValid(player) && this.horse.isAlive() && this.horse.distanceTo(player) < 8.0f;
    }
    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            int j = this.horseContainer.getContainerSize();
            if (i < j) {
                if (!this.moveItemStackTo(itemStack2, j, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).mayPlace(itemStack2) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).mayPlace(itemStack2)) {
                if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (j <= 2 || !this.moveItemStackTo(itemStack2, 2, j, false)) {
                int l;
                int k = j;
                int m = l = k + 27;
                int n = m + 9;
                if (i >= m && i < n ? !this.moveItemStackTo(itemStack2, k, l, false) : (i >= k && i < l ? !this.moveItemStackTo(itemStack2, m, n, false) : !this.moveItemStackTo(itemStack2, m, l, false))) {
                    return ItemStack.EMPTY;
                }
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.horseContainer.stopOpen(player);
    }
}