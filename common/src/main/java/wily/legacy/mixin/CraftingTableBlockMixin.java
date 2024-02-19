package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.LegacyMinecraft;
import wily.legacy.network.ServerOpenClientMenu;
import wily.legacy.util.ScreenUtil;

@Mixin(CraftingTableBlock.class)
public class CraftingTableBlockMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) LegacyMinecraft.NETWORK.sendToServer(new ServerOpenClientMenu(blockPos,ScreenUtil.hasClassicCrafting() ? 0 : 1));
        cir.setReturnValue(level.isClientSide ? InteractionResult.SUCCESS: InteractionResult.CONSUME);
    }
}
