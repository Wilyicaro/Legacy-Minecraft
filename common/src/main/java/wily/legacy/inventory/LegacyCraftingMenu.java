package wily.legacy.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import wily.legacy.client.Offset;
import wily.legacy.init.LegacyMenuTypes;

import java.util.List;

public class LegacyCraftingMenu extends AbstractContainerMenu implements RecipeMenu {
    public static final Component CRAFTING_TITLE = Component.translatable("container.crafting");
    public static final Component STONECUTTER_TITLE = Component.translatable("container.stonecutter");
    public static final Component LOOM_TITLE = Component.translatable("container.stonecutter");
    final BlockPos blockPos;
    public boolean is2x2 = false;
    public boolean inventoryActive = true;
    public static LegacyCraftingMenu playerCraftingMenu(int window, Inventory inventory){
        return new LegacyCraftingMenu(inventory, LegacyMenuTypes.PLAYER_CRAFTING_PANEL_MENU.get(),window,null);
    }
    public static LegacyCraftingMenu craftingMenu(int window, Inventory inventory,BlockPos pos){
        return new LegacyCraftingMenu(inventory, LegacyMenuTypes.CRAFTING_PANEL_MENU.get(),window,pos);
    }
    public static LegacyCraftingMenu craftingMenu(int window, Inventory inventory){
        return craftingMenu(window, inventory,null);
    }
    public static LegacyCraftingMenu loomMenu(int window, Inventory inventory,BlockPos blockPos){
        return new LegacyCraftingMenu(inventory, LegacyMenuTypes.LOOM_PANEL_MENU.get(),window,blockPos);
    }
    public static LegacyCraftingMenu loomMenu(int window, Inventory inventory){
        return loomMenu(window,inventory,null);
    }
    public static LegacyCraftingMenu stoneCutterMenu(int window, Inventory inventory,BlockPos blockPos){
        return new LegacyCraftingMenu(inventory, LegacyMenuTypes.STONECUTTER_PANEL_MENU.get(),window,blockPos){
            long lastSoundTime;
            @Override
            public void onCraft(Player player, int buttonInfo, List<Ingredient> ingredients, ItemStack result) {
                super.onCraft(player, buttonInfo, ingredients, result);
                long l = player.level().getGameTime();
                if (lastSoundTime != l) {
                    player.level().playSound(null, this.blockPos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
                    lastSoundTime = l;
                }
            }
        };
    }
    public static LegacyCraftingMenu stoneCutterMenu(int window, Inventory inventory){
        return stoneCutterMenu(window, inventory,null);
    }

    public LegacyCraftingMenu(Inventory inventory, @Nullable MenuType<?> menuType, int i, BlockPos pos) {
        super(menuType, i);
        this.blockPos =pos;
        addInventorySlotGrid(inventory, 9,186, 133,3);
        addInventorySlotGrid(inventory, 0,186, 186,1);
    }
    private static final Offset INVENTORY_OFFSET = new Offset(0.5,0.5,0);
    private static final Offset INVENTORY_2x2_OFFSET = new Offset(-40.5,0.5,0);

    public void addInventorySlotGrid(Container container, int startIndex, int x, int y, int rows){
        for (int j = 0; j < rows; j++) {
            for (int k = 0; k < 9; k++) {
                addSlot(new LegacySlotWrapper(container,startIndex +  j * 9 + k,x + k * 16,y + j * 16){
                    @Override
                    public void setChanged() {
                        super.setChanged();
                        slotsChanged(container);
                    }
                    @Override
                    public boolean isActive() {
                        return inventoryActive;
                    }
                    public Offset getOffset() {
                        return is2x2 ? INVENTORY_2x2_OFFSET : INVENTORY_OFFSET;
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
        return blockPos == null || !player.level().getBlockState(blockPos).isAir() && player.distanceToSqr((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5) <= 64.0;
    }

    public static boolean isValidBlock(Player player, BlockPos pos, Block wantedBlock){
        return player.level().getBlockState(pos).is(wantedBlock) && player.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5) <= 64.0;
    }

    public static MenuProvider getMenuProvider(BlockPos pos, boolean is2x2) {
        return new SimpleMenuProvider((i, inventory, player) ->  is2x2 ? playerCraftingMenu(i,inventory) : craftingMenu(i, inventory, pos), CRAFTING_TITLE);
    }
    public static MenuProvider getMenuProvider(MenuConstructor constructor, Component component) {
        return new SimpleMenuProvider(constructor, component);
    }
    public static MenuProvider getLoomMenuProvider(BlockPos pos) {
        return getMenuProvider((i,inv,p)-> loomMenu(i,inv,pos), LOOM_TITLE);
    }
    public static MenuProvider getStonecutterMenuProvider(BlockPos pos) {
        return getMenuProvider((i,inv,p)-> stoneCutterMenu(i,inv,pos), LOOM_TITLE);
    }
    @Override
    public void onCraft(Player player, int buttonInfo, List<Ingredient> ingredients, ItemStack result) {
        result.onCraftedBy(player.level(),player,result.getCount());
    }
}
