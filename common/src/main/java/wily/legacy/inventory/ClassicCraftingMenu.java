package wily.legacy.inventory;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import wily.legacy.LegacyMinecraft;

import java.util.Optional;

import static wily.legacy.init.LegacyMenuTypes.CLASSIC_CRAFTING_MENU;

public class ClassicCraftingMenu extends RecipeBookMenu<CraftingContainer> {
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final Player player;
    private final ContainerLevelAccess access;
    public ClassicCraftingMenu(int windowId, Player player) {
        this(windowId,player,ContainerLevelAccess.NULL);
    }
    public ClassicCraftingMenu(int windowId, Player player, ContainerLevelAccess access) {
        super(CLASSIC_CRAFTING_MENU.get(), windowId);
        this.player = player;
        this.access = access;
        int i;

        this.addSlot(new LegacySlotWrapper(new ResultSlot(player, craftSlots, resultSlots, 0, 150, 39)){
            public int getWidth() {return 32;}
            public int getHeight() {return 32;}
        });
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new LegacySlotWrapper(craftSlots, j + i * 3, 34 + j * 21, 23 + i * 21));
            }
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new LegacySlotWrapper(player.getInventory(), j + (i + 1) * 9, 14 + j * 21, 102 + i * 21));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new LegacySlotWrapper(player.getInventory(), i, 14 + i * 21, 171));
        }
    }
    protected static void slotChangedCraftingGrid(AbstractContainerMenu abstractContainerMenu, Level level, Player player, CraftingContainer craftingContainer, ResultContainer resultContainer) {
        if (level.isClientSide) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        ItemStack itemStack = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);
        if (optional.isPresent()) {
            ItemStack itemStack2;
            RecipeHolder<CraftingRecipe> recipeHolder = optional.get();
            CraftingRecipe craftingRecipe = recipeHolder.value();
            if (resultContainer.setRecipeUsed(level, serverPlayer, recipeHolder) && (itemStack2 = craftingRecipe.assemble(craftingContainer, level.registryAccess())).isItemEnabled(level.enabledFeatures())) {
                itemStack = itemStack2;
            }
        }
        resultContainer.setItem(0, itemStack);
        abstractContainerMenu.setRemoteSlot(0, itemStack);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(abstractContainerMenu.containerId, abstractContainerMenu.incrementStateId(), 0, itemStack));
    }

    @Override
    public void slotsChanged(Container container) {
        this.access.execute((level, blockPos) -> slotChangedCraftingGrid(this, level, this.player, this.craftSlots, this.resultSlots));
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents stackedContents) {
        this.craftSlots.fillStackedContents(stackedContents);
    }

    @Override
    public void clearCraftingContent() {
        this.craftSlots.clearContent();
        this.resultSlots.clearContent();
    }

    @Override
    public boolean recipeMatches(RecipeHolder<? extends Recipe<CraftingContainer>> recipeHolder) {
        return recipeHolder.value().matches(this.craftSlots, this.player.level());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, blockPos) -> this.clearContainer(player, this.craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return CraftingMenu.stillValid(this.access, player, Blocks.CRAFTING_TABLE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (i == 0) {
                this.access.execute((level, blockPos) -> itemStack2.getItem().onCraftedBy(itemStack2, (Level)level, player));
                if (!this.moveItemStackTo(itemStack2, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemStack2, itemStack);
            } else if (i >= 10 && i < 46 ? !this.moveItemStackTo(itemStack2, 1, 10, false) && (i < 37 ? !this.moveItemStackTo(itemStack2, 37, 46, false) : !this.moveItemStackTo(itemStack2, 10, 37, false)) : !this.moveItemStackTo(itemStack2, 10, 46, false)) {
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
            if (i == 0) {
                player.drop(itemStack2, false);
            }
        }
        return itemStack;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack itemStack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(itemStack, slot);
    }

    @Override
    public int getResultSlotIndex() {
        return 0;
    }

    @Override
    public int getGridWidth() {
        return this.craftSlots.getWidth();
    }

    @Override
    public int getGridHeight() {
        return this.craftSlots.getHeight();
    }

    @Override
    public int getSize() {
        return 10;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int i) {
        return i != this.getResultSlotIndex();
    }

}