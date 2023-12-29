package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.inventory.LegacyFurnaceMenu;

@Mixin(SmokerBlockEntity.class)
public class SmokerBlockEntityMixin extends AbstractFurnaceBlockEntity {

    public SmokerBlockEntityMixin(BlockPos blockPos, BlockState blockState) {
        super(BlockEntityType.SMOKER, blockPos, blockState, RecipeType.SMOKING);
    }
    @Override
    public Component getDefaultName() {
        return Component.translatable("container.smoker");
    }
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return LegacyFurnaceMenu.smoker(i,inventory, this, this.dataAccess);
    }
}
