package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerPlayer;
//? if >1.20.2 {
import net.minecraft.server.players.NameAndId;
//?}
import net.minecraft.server.players.PlayerList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.skins.skin.SkinSync;
import wily.legacy.entity.LegacyPlayer;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.network.PlayerInfoSync;
import wily.legacy.world.PlayerTrustAdmin;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void legacy4j$denySessionKickedPlayer(SocketAddress address, NameAndId player, CallbackInfoReturnable<Component> cir) {
        PlayerList playerList = (PlayerList) (Object) this;
        if (PlayerTrustAdmin.isSessionKicked(playerList.getServer(), player.id()))
            cir.setReturnValue(Component.translatable("multiplayer.disconnect.kicked"));
    }

    @Inject(method = "sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("RETURN"))
    private void legacy4j$syncTrustAuthority(ServerPlayer player, CallbackInfo ci) {
        LegacyPlayerInfo info = LegacyPlayerInfo.of(player);
        boolean previousFullAuthority = info.hasFullTrustAuthority();
        PlayerInfoSync.All authoritative = PlayerInfoSync.All.fromPlayer(player);
        boolean fullAuthority = info.hasFullTrustAuthority();
        boolean changed = previousFullAuthority != fullAuthority;
        if (!changed) return;
        if (!fullAuthority) PlayerTrustAdmin.enforceCurrentRestrictions(player);
        CommonNetwork.sendToPlayers(player.level().getServer().getPlayerList().getPlayers(), authoritative);
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    public void respawn(ServerPlayer serverPlayer, boolean bl, /*? if >=1.20.5 {*/Entity.RemovalReason removalReason,/*?}*/ CallbackInfoReturnable<ServerPlayer> cir) {
        ((LegacyPlayerInfo) cir.getReturnValue()).copyFrom(((LegacyPlayerInfo) serverPlayer));
        LegacyPlayerInfo.updateMayFlySurvival(cir.getReturnValue(), ((LegacyPlayerInfo) serverPlayer).mayFlySurvival(), true);
        ((LegacyPlayer) cir.getReturnValue()).copyFrom(((LegacyPlayer) serverPlayer));
        PlayerTrustAdmin.rememberPlayer(cir.getReturnValue());
    }

    @Inject(method = "remove", at = @At("HEAD"), require = 0)
    public void legacy4j$clearSkinSyncState(ServerPlayer serverPlayer, CallbackInfo ci) {
        if (serverPlayer != null) {
            PlayerTrustAdmin.rememberPlayer(serverPlayer);
            SkinSync.clearPlayer(serverPlayer.getUUID());
        }
    }
}
