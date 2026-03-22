package wily.legacy.mixin.base;

import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.inventory.RenameItemMenu;
import wily.legacy.init.LegacyGameRules;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow @Final private static Logger LOGGER;
    @Shadow public ServerPlayer player;

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

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void handlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LCE_OFFHAND_LIMITS)) ci.cancel();
    }
}
