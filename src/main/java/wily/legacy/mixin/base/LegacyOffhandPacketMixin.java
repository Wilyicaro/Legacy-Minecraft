package wily.legacy.mixin.base;

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.init.LegacyGameRules;

@Mixin(ServerGamePacketListenerImpl.class)
public class LegacyOffhandPacketMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    public void handlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LCE_OFFHAND_LIMITS)) ci.cancel();
    }
}
