package wily.legacy.mixin.base.waterlogging;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.LegacyWaterlogging;

@Mixin({BannerBlock.class, WallBannerBlock.class})
public class BannerBlockMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void legacy$applyPlacementFluid(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null) {
            cir.setReturnValue(LegacyWaterlogging.applyPlacementFluid(context.getLevel(), context.getClickedPos(), state));
        }
    }
}
