package wily.legacy.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import wily.legacy.client.Offset;
import wily.legacy.init.LegacyMenuTypes;

import java.util.function.Predicate;

public class LegacyCraftingMenu extends AbstractContainerMenu {
    public static final Component CONTAINER_TITLE = Component.translatable("container.crafting");
    private final Predicate<Player> stillValid;
    public static LegacyCraftingMenu playerCraftingMenu(int window, Inventory inventory){
        return new LegacyCraftingMenu(inventory, LegacyMenuTypes.PLAYER_CRAFTING_PANEL_MENU.get(),window,p->true);
    }
    public static LegacyCraftingMenu craftingMenu(int window, Inventory inventory,Predicate<Player> stillValid){
        return new LegacyCraftingMenu(inventory, LegacyMenuTypes.CRAFTING_PANEL_MENU.get(),window,stillValid);
    }
    public static LegacyCraftingMenu craftingMenu(int window, Inventory inventory){
        return craftingMenu(window, inventory, p-> true);
    }

    public LegacyCraftingMenu(Inventory inventory, @Nullable MenuType<?> menuType, int i, Predicate<Player> stillValid) {
        super(menuType, i);
        this.stillValid = stillValid;
        addInventorySlotGrid(inventory, 9,186, 133,3);
        addInventorySlotGrid(inventory, 0,186, 186,1);
    }


    public void addInventorySlotGrid(Container container, int startIndex, int x, int y, int rows){
        for (int j = 0; j < rows; j++) {
            for (int k = 0; k < 9; k++) {
                addSlot(new LegacySlotWrapper(container,startIndex +  j * 9 + k,x + k * 16,y + j * 16){
                    public Offset getOffset() {
                        return new Offset(0.5,0.5,0);
                    }
                    public int getWidth() {
                        return 16;
                    }
                    public int getHeight() {
                        return 16;
                    }
                });
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (i >= 0 && i < 27) {
                if (!this.moveItemStackTo(itemStack2, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, 27, false)) {
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
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid.test(player);
    }

    public static boolean isValidBlock(Player player, BlockPos pos, Block wantedBlock){
        return player.level().getBlockState(pos).is(wantedBlock) && player.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5) <= 64.0;
    }

    public static MenuProvider getMenuProvider(BlockPos pos, boolean is2x2) {
        return new SimpleMenuProvider((i, inventory, player) ->  is2x2 ? playerCraftingMenu(i,inventory) : craftingMenu(i, inventory, p-> isValidBlock(p,pos, Blocks.CRAFTING_TABLE)), CONTAINER_TITLE);
    }
}
