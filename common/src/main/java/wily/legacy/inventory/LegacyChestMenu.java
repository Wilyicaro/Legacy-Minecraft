package wily.legacy.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.Nullable;
import wily.legacy.LegacyMinecraft;

import java.util.Optional;

import static wily.legacy.init.LegacyMenuTypes.*;

public class LegacyChestMenu extends AbstractContainerMenu {
    private final Container container;
    private final int containerRows;

    public static LegacyChestMenu fiveSlots(int i, Inventory inventory) {
        return new LegacyChestMenu(STORAGE_5X1.get(), i, inventory, new SimpleContainer(5), 1);
    }
    public static LegacyChestMenu fiveSlots(int i, Inventory inventory, Container container) {
        return new LegacyChestMenu(STORAGE_5X1.get(), i, inventory, container,1);
    }
    public static LegacyChestMenu threeRows(int i, Inventory inventory) {
        return new LegacyChestMenu(CHEST_MENU.get(), i, inventory, 3);
    }
    public static LegacyChestMenu threeRowsColumns(int i, Inventory inventory) {
        return new LegacyChestMenu(STORAGE_3X3.get(), i, inventory, new SimpleContainer(9), 3);
    }
    public static LegacyChestMenu sixRows(int i, Inventory inventory) {
        return new LegacyChestMenu(LARGE_CHEST_MENU.get(), i, inventory, 6);
    }
    public static LegacyChestMenu threeRows(int i, Inventory inventory, Container container) {
        return new LegacyChestMenu(CHEST_MENU.get(), i, inventory, container, 3);
    }
    public static LegacyChestMenu threeRowsColumns(int i, Inventory inventory, Container container) {
        return new LegacyChestMenu(STORAGE_3X3.get(), i, inventory, container, 3);
    }

    public static LegacyChestMenu sixRows(int i, Inventory inventory, Container container) {
        return new LegacyChestMenu(LARGE_CHEST_MENU.get(), i, inventory, container, 6);
    }
    public static LegacyChestMenu threeRowsBag(int i, Inventory inventory) {
        return new LegacyChestMenu(BAG_MENU.get(), i, inventory, 3,true);
    }

    public static LegacyChestMenu threeRowsBag(int i, Inventory inventory, Container container) {
        return new LegacyChestMenu(BAG_MENU.get(), i, inventory, container, 3,true);
    }
    private LegacyChestMenu(MenuType<?> menuType, int i, Inventory inventory, int j) {
        this(menuType, i, inventory, new SimpleContainer(9 * j), j);
    }
    private LegacyChestMenu(MenuType<?> menuType, int i, Inventory inventory, int j, boolean isBag) {
        this(menuType, i, inventory, new SimpleContainer(9 * j), j, isBag);
    }
    public LegacyChestMenu(MenuType<?> menuType, int i, Inventory inventory, Container container, int j) {
        this(menuType,i,inventory,container,j,false);
    }

    public LegacyChestMenu(MenuType<?> menuType, int i, Inventory inventory, Container container, int rows, boolean isBag) {
        super(menuType, i);
        int m;
        int l;
        this.container = container;
        this.containerRows = rows;
        int columns = container.getContainerSize() / rows;
        int hDiff = getHorizontalDiff();
        container.startOpen(inventory.player);
        for (l = 0; l < this.containerRows; ++l) {
            for (m = 0; m < columns; ++m) {
                this.addSlot(new LegacySlotWrapper(container,m + l * columns,hDiff + 14 + m * 21, 26 + l * 21){
                    @Override
                    public boolean mayPlace(ItemStack itemStack) {
                        return !isBag || itemStack.getItem().canFitInsideContainerItems();
                    }
                });
            }
        }
        int k = getVerticalDiff();
        for (l = 0; l < 3; ++l) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new LegacySlotWrapper(inventory, m + l * 9 + 9, 14 + m * 21, 107 + l * 21 + k));
            }
        }
        for (l = 0; l < 9; ++l) {
            this.addSlot(new LegacySlotWrapper(inventory, l, 14 + l * 21, 177 + k));
        }
    }
    public int getVerticalDiff(){
        return (this.containerRows - 3) * 21;
    }
    public int getHorizontalDiff(){
        return ((9 - container.getContainerSize() / containerRows) * 21) / 2;
    }
    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (i < this.containerRows * 9 ? !this.moveItemStackTo(itemStack2, this.containerRows * 9, this.slots.size(), true) : !this.moveItemStackTo(itemStack2, 0, this.containerRows * 9, false)) {
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
        this.container.stopOpen(player);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getRowCount() {
        return this.containerRows;
    }
    public static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>> MENU_PROVIDER_COMBINER = new DoubleBlockCombiner.Combiner<>() {

        @Override
        public Optional<MenuProvider> acceptDouble(final ChestBlockEntity chestBlockEntity, final ChestBlockEntity chestBlockEntity2) {
            final CompoundContainer container = new CompoundContainer(chestBlockEntity, chestBlockEntity2);
            return Optional.of(new MenuProvider() {
                @Override
                @Nullable
                public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                    if (chestBlockEntity.canOpen(player) && chestBlockEntity2.canOpen(player)) {
                        chestBlockEntity.unpackLootTable(inventory.player);
                        chestBlockEntity2.unpackLootTable(inventory.player);
                        return LegacyChestMenu.sixRows(i, inventory, container);
                    }
                    return null;
                }

                @Override
                public Component getDisplayName() {
                    if (chestBlockEntity.hasCustomName()) {
                        return chestBlockEntity.getDisplayName();
                    }
                    if (chestBlockEntity2.hasCustomName()) {
                        return chestBlockEntity2.getDisplayName();
                    }
                    return Component.translatable("container.chestDouble");
                }
            });
        }

        @Override
        public Optional<MenuProvider> acceptSingle(ChestBlockEntity chestBlockEntity) {
            return Optional.of(chestBlockEntity);
        }

        @Override
        public Optional<MenuProvider> acceptNone() {
            return Optional.empty();
        }

    };
}
