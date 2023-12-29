
package wily.legacy.inventory;

import dev.architectury.registry.fuel.FuelRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import wily.legacy.LegacyMinecraft;

import static wily.legacy.init.LegacyMenuTypes.*;

public class LegacyFurnaceMenu extends RecipeBookMenu<Container> {
    private final Container container;
    private final ContainerData data;
    protected final Level level;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    private final RecipeBookType recipeBookType;
    public static LegacyFurnaceMenu furnace(int i, Inventory inventory){
        return new LegacyFurnaceMenu(LEGACY_FURNACE_MENU.get(), RecipeType.SMELTING,RecipeBookType.FURNACE,i,inventory);
    }
    public static LegacyFurnaceMenu furnace(int i, Inventory inventory, Container container, ContainerData containerData){
        return new LegacyFurnaceMenu(LEGACY_FURNACE_MENU.get(), RecipeType.SMELTING,RecipeBookType.FURNACE,i,inventory, container, containerData);
    }
    public static LegacyFurnaceMenu smoker(int i, Inventory inventory){
        return new LegacyFurnaceMenu(LEGACY_SMOKER_MENU.get(), RecipeType.SMOKING,RecipeBookType.SMOKER,i,inventory);
    }
    public static LegacyFurnaceMenu smoker(int i, Inventory inventory, Container container, ContainerData containerData){
        return new LegacyFurnaceMenu(LEGACY_SMOKER_MENU.get(), RecipeType.SMOKING,RecipeBookType.SMOKER,i,inventory, container, containerData);
    }
    public static LegacyFurnaceMenu blastFurnace(int i, Inventory inventory){
        return new LegacyFurnaceMenu(LEGACY_BLAST_FURNACE_MENU.get(), RecipeType.BLASTING,RecipeBookType.BLAST_FURNACE,i,inventory);
    }
    public static LegacyFurnaceMenu blastFurnace(int i, Inventory inventory, Container container, ContainerData containerData){
        return new LegacyFurnaceMenu(LEGACY_BLAST_FURNACE_MENU.get(), RecipeType.BLASTING,RecipeBookType.BLAST_FURNACE,i,inventory, container, containerData);
    }

    protected LegacyFurnaceMenu(MenuType<?> menuType, RecipeType<? extends AbstractCookingRecipe> recipeType, RecipeBookType recipeBookType, int i, Inventory inventory) {
        this(menuType, recipeType, recipeBookType, i, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    protected LegacyFurnaceMenu(MenuType<?> menuType, RecipeType<? extends AbstractCookingRecipe> recipeType, RecipeBookType recipeBookType, int i, Inventory inventory, Container container, ContainerData containerData) {
        super(menuType, i);
        int j;
        this.recipeType = recipeType;
        this.recipeBookType = recipeBookType;
        AbstractFurnaceMenu.checkContainerSize(container, 3);
        AbstractFurnaceMenu.checkContainerDataCount(containerData, 4);
        this.container = container;
        this.data = containerData;
        this.level = inventory.player.level();
        this.addSlot(new LegacySlotWrapper(container, 0, 77, 25));
        this.addSlot(new LegacySlotWrapper(container, 1, 77, 72){
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return isFuel(itemStack) || FurnaceFuelSlot.isBucket(itemStack);
            }
            @Override
            public int getMaxStackSize(ItemStack itemStack) {
                return FurnaceFuelSlot.isBucket(itemStack) ? 1 : super.getMaxStackSize(itemStack);
            }
        });
        this.addSlot(new LegacySlotWrapper(new FurnaceResultSlot(inventory.player, container, 2, 155, 44)){
            public int getWidth() {
                return 32;
            }
            public int getHeight() {
                return 32;
            }
        });
        for (j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new LegacySlotWrapper(inventory, k + j * 9 + 9, 14 + k * 21, 111 + j * 21));
            }
        }
        for (j = 0; j < 9; ++j) {
            this.addSlot(new LegacySlotWrapper(inventory, j, 14 + j * 21, 180));
        }
        this.addDataSlots(containerData);
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents stackedContents) {
        if (this.container instanceof StackedContentsCompatible s) {
            s.fillStackedContents(stackedContents);
        }
    }

    @Override
    public void clearCraftingContent() {
        this.getSlot(0).set(ItemStack.EMPTY);
        this.getSlot(2).set(ItemStack.EMPTY);
    }

    @Override
    public boolean recipeMatches(RecipeHolder<? extends Recipe<Container>> recipeHolder) {
        return recipeHolder.value().matches(this.container, this.level);
    }

    @Override
    public int getResultSlotIndex() {
        return 2;
    }

    @Override
    public int getGridWidth() {
        return 1;
    }

    @Override
    public int getGridHeight() {
        return 1;
    }

    @Override
    public int getSize() {
        return 3;
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
            if (i == 2) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemStack2, itemStack);
            } else if (i == 1 || i == 0 ? !this.moveItemStackTo(itemStack2, 3, 39, false) : (this.canSmelt(itemStack2) ? !this.moveItemStackTo(itemStack2, 0, 1, false) : (this.isFuel(itemStack2) ? !this.moveItemStackTo(itemStack2, 1, 2, false) : (i >= 3 && i < 30 ? !this.moveItemStackTo(itemStack2, 30, 39, false) : i >= 30 && i < 39 && !this.moveItemStackTo(itemStack2, 3, 30, false))))) {
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
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

    protected boolean canSmelt(ItemStack itemStack) {
        return this.level.getRecipeManager().getRecipeFor(this.recipeType, new SimpleContainer(itemStack), this.level).isPresent();
    }

    protected boolean isFuel(ItemStack itemStack) {
        return FuelRegistry.get(itemStack) != 0;
    }

    public float getBurnProgress() {
        int i = this.data.get(2);
        int j = this.data.get(3);
        if (j == 0 || i == 0) {
            return 0.0f;
        }
        return Mth.clamp((float)i / (float)j, 0.0f, 1.0f);
    }

    public float getLitProgress() {
        int i = this.data.get(1);
        if (i == 0) {
            i = 200;
        }
        return Mth.clamp((float)this.data.get(0) / (float)i, 0.0f, 1.0f);
    }

    public boolean isLit() {
        return this.data.get(0) > 0;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return this.recipeBookType;
    }

    @Override
    public boolean shouldMoveToInventory(int i) {
        return i != 1;
    }
}
