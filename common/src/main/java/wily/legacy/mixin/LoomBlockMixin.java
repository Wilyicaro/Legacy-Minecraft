package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.LegacyMinecraft;
import wily.legacy.init.LegacyMenuTypes;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.player.LegacyPlayer;
import wily.legacy.util.ScreenUtil;

@Mixin(LoomBlock.class)
public class LoomBlockMixin {
    @Inject(method = "use", at = @At("HEAD"))
    public void use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) LegacyMinecraft.NETWORK.sendToServer(new PlayerInfoSync(ScreenUtil.hasClassicCrafting() ? 1 : 2,player));
    }
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getMenuProvider(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/MenuProvider;"))
    public MenuProvider use(BlockState instance, Level level, BlockPos blockPos, BlockState blockState, Level level1, BlockPos blockPos1, Player player) {
        if (player instanceof LegacyPlayer p && !p.hasClassicCrafting()) return LegacyCraftingMenu.getLoomMenuProvider(blockPos);
        return instance.getMenuProvider(level,blockPos);
    }
}
