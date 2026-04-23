package wily.legacy.mixin.base;

import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ShieldItem;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.entity.LegacyShieldPlayer;
import wily.legacy.inventory.RenameItemMenu;
import wily.legacy.init.LegacyGameRules;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow @Final private static Logger LOGGER;
    @Shadow public ServerPlayer player;

    private void legacy$lowerShield() {
        if (LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS) && (player.isPassenger() || player.isShiftKeyDown()) && player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem) {
            player.stopUsingItem();
        }
    }

    @Inject(method = "handleRenameItem", at = @At("RETURN"))
    public void handleRenameItem(ServerboundRenameItemPacket packet, CallbackInfo ci) {
        if (player.containerMenu instanceof RenameItemMenu renameMenu) {
            if (!player.containerMenu.stillValid(player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", player, renameMenu);
                return;
            }
            renameMenu.setResultItemName(packet.getName());
        }
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"))
    private void handleUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        legacy$lowerShield();
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"))
    private void handleUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (!(player.getItemInHand(packet.getHand()).getItem() instanceof ShieldItem)) legacy$lowerShield();
    }

    @Inject(method = "handleInteract", at = @At("HEAD"))
    private void handleInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        legacy$lowerShield();
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"))
    private void handlePlayerActionHead(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        ServerboundPlayerActionPacket.Action action = packet.getAction();
        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK || action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK || action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            legacy$lowerShield();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;releaseUsingItem()V"), cancellable = true)
    private void handlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM && !((LegacyShieldPlayer) player).isShieldPaused() && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS) && (player.isPassenger() || player.isShiftKeyDown()) && player.isUsingItem() && player.getUseItem().getItem() instanceof ShieldItem) {
            ci.cancel();
        }
    }
}
