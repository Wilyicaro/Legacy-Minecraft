package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static wily.legacy.inventory.LegacyChestMenu.MENU_PROVIDER_COMBINER;

@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {
    @Shadow public abstract DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(BlockState arg, Level arg2, BlockPos arg3, boolean bl);

    @Inject(method = "getMenuProvider",at = @At("HEAD"), cancellable = true)
    public void getMenuProvider(BlockState blockState, Level level, BlockPos blockPos, CallbackInfoReturnable<MenuProvider> cir) {
        cir.setReturnValue(this.combine(blockState, level, blockPos, false).apply(MENU_PROVIDER_COMBINER).orElse(null));
    }
}
