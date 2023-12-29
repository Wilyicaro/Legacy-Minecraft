package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.inventory.LegacyFurnaceMenu;

@Mixin(BlastFurnaceBlockEntity.class)
public class BlastFurnaceBlockEntityMixin extends AbstractFurnaceBlockEntity {

    public BlastFurnaceBlockEntityMixin(BlockPos blockPos, BlockState blockState) {
        super(BlockEntityType.BLAST_FURNACE, blockPos, blockState, RecipeType.BLASTING);
    }
    @Override
    public Component getDefaultName() {
        return Component.translatable("container.blast_furnace");
    }
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return LegacyFurnaceMenu.blastFurnace(i,inventory, this, this.dataAccess);
    }
}
