package wily.legacy.Skins.mixin;


/**
 * Mixin: console skins / CPM rendering glue.
 */

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.SkinsBootstrap;

@Mixin(PlayerList.class)
public class PlayerListSkinsJoinMixin {

    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At("TAIL"), require = 0)
    private void legacy4j$skins$afterPlaceNewPlayer(Connection connection, ServerPlayer player,
                                                    CommonListenerCookie cookie, CallbackInfo ci) {
        if (player != null) {
            SkinsBootstrap.handleServerPlayerJoin(player);
        }
    }
}
