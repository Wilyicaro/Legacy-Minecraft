package wily.legacy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.inventory.ClassicCraftingMenu;

@Mixin(CraftingTableBlock.class)
public class CraftingTableBlockMixin {
    @Shadow @Final private static Component CONTAINER_TITLE;
    @Inject(method = "getMenuProvider",at = @At("HEAD"), cancellable = true)
    private void getMenuProvider(BlockState blockState, Level level, BlockPos blockPos, CallbackInfoReturnable<MenuProvider> cir){
        cir.setReturnValue(new SimpleMenuProvider((i, inventory, player) -> new ClassicCraftingMenu(i, player, ContainerLevelAccess.create(level, blockPos)), CONTAINER_TITLE));
    }
}
