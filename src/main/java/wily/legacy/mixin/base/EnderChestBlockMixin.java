package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(level.getBlockEntity(blockPos) instanceof EnderChestBlockEntity be)) {
            return;
        }
        Component customName = be.components().get(DataComponents.CUSTOM_NAME);
        if (customName == null) {
            return;
        }
        PlayerEnderChestContainer container = player.getEnderChestInventory();
        if (container == null) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        BlockPos above = blockPos.above();
        if (level.getBlockState(above).isRedstoneConductor(level, above)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            container.setActiveChest(be);
            player.openMenu(new SimpleMenuProvider((i, inventory, p) -> ChestMenu.threeRows(i, inventory, container), customName));
            player.awardStat(Stats.OPEN_ENDERCHEST);
            PiglinAi.angerNearbyPiglins(serverLevel, player, true);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
