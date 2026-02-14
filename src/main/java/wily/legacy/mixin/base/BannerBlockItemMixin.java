package wily.legacy.mixin.base;

import wily.legacy.block.LegacyBlockCaps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BannerBlockItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void mobspawning$legacyBannerCap(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getItemInHand().getItem() instanceof BannerItem)) {
            return;
        }
        if (context.getLevel().isClientSide()) {
            return;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = context.getItemInHand();
        int originalCount = stack.getCount();
        if (LegacyBlockCaps.isBannerCapped(level, player)) {
            player.displayClientMessage(
                    Component.translatable("legacy.banner.cap_reached"),
                    false
            );
            stack.setCount(originalCount);
            player.getInventory().setChanged();
            player.containerMenu.sendAllDataToRemote();
            player.inventoryMenu.sendAllDataToRemote();
            cir.setReturnValue(InteractionResult.FAIL);
            cir.cancel();
        }
    }
}
