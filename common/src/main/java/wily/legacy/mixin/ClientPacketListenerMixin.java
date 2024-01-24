package wily.legacy.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraftClient;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleMapItemData", at = @At("RETURN"))
    public void handleMapItemData(ClientboundMapItemDataPacket clientboundMapItemDataPacket, CallbackInfo ci) {
        if (clientboundMapItemDataPacket.getMapId() == -1)
            LegacyMinecraftClient.onClientPlayerInfoChange();
    }
    @Inject(method = "handlePlayerInfoUpdate", at = @At("RETURN"))
    public void handleLogin(ClientboundPlayerInfoUpdatePacket clientboundPlayerInfoUpdatePacket, CallbackInfo ci) {
        LegacyMinecraftClient.onClientPlayerInfoChange();
    }
    @Inject(method = "handlePlayerInfoRemove", at = @At("RETURN"))
    public void handleLogin(ClientboundPlayerInfoRemovePacket clientboundPlayerInfoRemovePacket, CallbackInfo ci) {
        LegacyMinecraftClient.onClientPlayerInfoChange();
    }
}