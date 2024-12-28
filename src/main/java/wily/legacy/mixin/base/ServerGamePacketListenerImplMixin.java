package wily.legacy.mixin.base;

import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
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

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "handleRenameItem", at = @At("RETURN"))
    public void handleRenameItem(ServerboundRenameItemPacket serverboundRenameItemPacket, CallbackInfo ci) {
        if (this.player.containerMenu instanceof RenameItemMenu renameMenu) {
            if (!player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, renameMenu);
                return;
            }
            renameMenu.setResultItemName(serverboundRenameItemPacket.getName());
        }
    }
}
