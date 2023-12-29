package wily.legacy.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import wily.legacy.LegacyMinecraft;

import static wily.legacy.init.LegacyMenuTypes.LEGACY_INVENTORY_MENU;
import static wily.legacy.init.LegacyMenuTypes.LEGACY_INVENTORY_MENU_CRAFTING;

public class LegacyInventoryMenu extends AbstractContainerMenu {
    public static final ResourceLocation SHIELD_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/shield_slot");
    public final boolean hasCrafting;

    public LegacyInventoryMenu(int windowId, Player player, boolean hasCrafting) {
        super(hasCrafting ? LEGACY_INVENTORY_MENU_CRAFTING.get() : LEGACY_INVENTORY_MENU.get(), windowId);
        this.hasCrafting = hasCrafting;
        int xDiff = hasCrafting ? 0 : 50;
        int i;

        for (i = 1; i < 5; ++i) {
            final EquipmentSlot equipmentSlot = EquipmentSlot.values()[EquipmentSlot.values().length - i];
            this.addSlot(new LegacySlotWrapper(player.getInventory(), 40 - i, xDiff + 14, -7 + i * 21){

                @Override
                public void setByPlayer(ItemStack itemStack, ItemStack itemStack2) {
                    player.onEquipItem(equipmentSlot, itemStack, itemStack2);
                    super.setByPlayer(itemStack, itemStack2);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return equipmentSlot == Mob.getEquipmentSlotForItem(itemStack);
                }

                public ResourceLocation getIconSprite() {
                    return getItem().isEmpty() ? new ResourceLocation(LegacyMinecraft.MOD_ID,"container/"+ equipmentSlot.getName()+ "_slot") : null;
                }

                @Override
                public boolean mayPickup(Player player2) {
                    ItemStack itemStack = this.getItem();
                    if (!itemStack.isEmpty() && !player2.isCreative() && EnchantmentHelper.hasBindingCurse(itemStack)) {
                        return false;
                    }
                    return super.mayPickup(player2);
                }
            });
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new LegacySlotWrapper(player.getInventory(), j + (i + 1) * 9, 14 + j * 21, 116 + i * 21));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new LegacySlotWrapper(player.getInventory(), i, 14 + i * 21, 186));
        }
        this.addSlot(new LegacySlotWrapper(player.getInventory(), 40, xDiff + 111, 77){
            @Override
            public void setByPlayer(ItemStack itemStack, ItemStack itemStack2) {
                player.onEquipItem(EquipmentSlot.OFFHAND, itemStack, itemStack2);
                super.setByPlayer(itemStack, itemStack2);
            }
            @Override
            public ResourceLocation getIconSprite() {
                return getItem().isEmpty() ? SHIELD_SLOT_SPRITE : null;
            }
        });
        if (hasCrafting) {
            this.addSlot(new LegacySlotWrapper(new ResultSlot(player, player.inventoryMenu.getCraftSlots(), player.inventoryMenu.resultSlots, 0, 180, 40)));
            for (i = 0; i < 2; ++i) {
                for (int j = 0; j < 2; ++j) {
                    this.addSlot(new LegacySlotWrapper(player.inventoryMenu.getCraftSlots(), j + i * 2, 111 + j * 21, 30 + i * 21));
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot.hasItem()) {
            int j;
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            EquipmentSlot equipmentSlot = Mob.getEquipmentSlotForItem(itemStack);

            if (i == slots.size() - 1) {
                if (!this.moveItemStackTo(itemStack2, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemStack2, itemStack);
            } else if (i >= 41 && i < 45 ? !this.moveItemStackTo(itemStack2, 4, 40, false) : (i >= 0 && i < 4 ? !this.moveItemStackTo(itemStack2, 4, 40, false) : (equipmentSlot.getType() == EquipmentSlot.Type.ARMOR && !this.slots.get(3 - equipmentSlot.getIndex()).hasItem() ? !this.moveItemStackTo(itemStack2, j = 3 - equipmentSlot.getIndex(), j + 1, false) : (equipmentSlot == EquipmentSlot.OFFHAND && !this.slots.get(40).hasItem() ? !this.moveItemStackTo(itemStack2, 40, 41, false) : (i >= 4 && i < 31 ? !this.moveItemStackTo(itemStack2, 31, 40, false) : (i >= 31 && i < 40 ? !this.moveItemStackTo(itemStack2, 4, 31, false) : !this.moveItemStackTo(itemStack2, 4, 40, false))))))) {
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, itemStack);
            } else {
                slot.setChanged();
            }
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemStack2);
            if (i == slots.size() - 1) {
                player.drop(itemStack2, false);
            }
        }
        return itemStack;
    }


    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public static MenuProvider getMenuProvider(boolean hasCrafting){
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.crafting");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                return new LegacyInventoryMenu(i,player,hasCrafting);
            }
        };
    }
}