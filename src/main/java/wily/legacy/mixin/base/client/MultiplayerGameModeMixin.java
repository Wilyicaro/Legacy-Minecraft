package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
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
import wily.legacy.client.screen.CreativeModeScreen;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.util.LegacyItemUtil;

@Mixin(MultiPlayerGameMode.class)
public class MultiplayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    //? if >=1.21.2 {
    @ModifyVariable(method = "handleCreativeModeItemDrop", at = @At("STORE"))
    public boolean handleCreativeModeItemDrop(boolean original) {
        return original && !(minecraft.screen instanceof CreativeModeScreen);
    }
    //?}

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void useItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS) && player.getItemInHand(hand).getItem() instanceof ShieldItem) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void handleContainerInput(int i, int j, int k, ContainerInput ContainerInput, Player player, CallbackInfo ci) {
        if (!LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_OFFHAND_LIMITS)) return;
        Slot slot = getSlot(player, j);
        if (slot == null) return;
        if (ContainerInput == ContainerInput.PICKUP && isOffhandSlot(slot) && !player.containerMenu.getCarried().isEmpty() && !LegacyItemUtil.canGoInLceOffhand(player.containerMenu.getCarried())) {
            ci.cancel();
            return;
        }
        if (ContainerInput == ContainerInput.SWAP && ((k == 40 && !LegacyItemUtil.canGoInLceOffhand(slot.getItem())) || (k >= 0 && k < 9 && isOffhandSlot(slot) && !LegacyItemUtil.canGoInLceOffhand(player.getInventory().getItem(k))))) {
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
