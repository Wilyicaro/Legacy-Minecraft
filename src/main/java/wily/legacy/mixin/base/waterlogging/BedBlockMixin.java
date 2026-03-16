package wily.legacy.mixin.base.waterlogging;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.LegacySimpleWaterloggedBlock;
import wily.legacy.util.LegacyWaterlogging;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin implements LegacySimpleWaterloggedBlock {
    @Redirect(
        method = "setPlacedBy",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
        )
    )
    private boolean legacy$syncHeadFluid(Level level, BlockPos targetPos, BlockState headState, int flags, Level originalLevel, BlockPos pos, BlockState placedState, LivingEntity placer, ItemStack stack) {
        return level.setBlock(targetPos, LegacyWaterlogging.syncPlacementFluid(level, targetPos, headState), flags);
    }
}
