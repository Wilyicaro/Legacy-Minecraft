package wily.legacy.mixin.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.CreativeModeScreen;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.util.LegacyBlockProtection;

@Mixin(MultiPlayerGameMode.class)
public class MultiplayerGameModeMixin {
    @Shadow @Final private Minecraft minecraft;
    //? if >=1.21.2 {
    /*@ModifyVariable(method = "handleCreativeModeItemDrop", at = @At("STORE"))
    public boolean handleCreativeModeItemDrop(boolean original) {
        return original && !(minecraft.screen instanceof CreativeModeScreen);
    }
    *///?}

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void useItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS) && player.getItemInHand(hand).getItem() instanceof ShieldItem) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.player != null && minecraft.level != null && LegacyBlockProtection.blocksBreak(minecraft.level, pos, minecraft.level.getBlockState(pos), minecraft.player.getAbilities().instabuild)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void handleInventoryMouseClick(int i, int j, int k, ClickType clickType, Player player, CallbackInfo ci) {
        if (!LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_OFFHAND_LIMITS)) return;
        Slot slot = getSlot(player, j);
        if (slot == null) return;
        if (clickType == ClickType.PICKUP && isOffhandSlot(slot) && !player.containerMenu.getCarried().isEmpty() && !Legacy4J.canGoInLceOffhand(player.containerMenu.getCarried())) {
            ci.cancel();
            return;
        }
        if (clickType == ClickType.SWAP && ((k == 40 && !Legacy4J.canGoInLceOffhand(slot.getItem())) || (k >= 0 && k < 9 && isOffhandSlot(slot) && !Legacy4J.canGoInLceOffhand(player.getInventory().getItem(k))))) {
            ci.cancel();
        }
    }

    private static Slot getSlot(Player player, int slotIndex) {
        return slotIndex < 0 || slotIndex >= player.containerMenu.slots.size() ? null : player.containerMenu.slots.get(slotIndex);
    }

    private static boolean isOffhandSlot(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() == 40;
    }
}
