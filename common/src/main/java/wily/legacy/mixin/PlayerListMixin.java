package wily.legacy.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.network.CommonNetwork;
import wily.legacy.player.LegacyPlayer;
import wily.legacy.player.LegacyPlayerInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    private final CommonNetwork.SecureExecutor SECURE_EXECUTOR = new CommonNetwork.SecureExecutor() {
        @Override
        public boolean isSecure() {
            return true;
        }
    };
    @Shadow protected abstract void save(ServerPlayer arg);
    @Inject(method = "saveAll", at = @At("RETURN"))
    public void saveAll(CallbackInfo ci) {
        SECURE_EXECUTOR.executeAll();
    }
    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    public void placeNewPlayer(Connection connection, ServerPlayer serverPlayer, CallbackInfo ci) {
        Legacy4J.onServerPlayerJoin(serverPlayer);
    }
    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 0))
    public void clientPlaceNewPlayer(Connection connection, ServerPlayer serverPlayer, CallbackInfo ci) {
        if (!serverPlayer.server.isDedicatedServer()) Legacy4JClient.serverPlayerJoin(serverPlayer);
    }
    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;load(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/nbt/CompoundTag;"))
    public CompoundTag placeNewPlayer(PlayerList list, ServerPlayer arg2) {
        CompoundTag playerTag = list.load(arg2);
        if (playerTag == null){
            ItemStack map = Items.MAP.getDefaultInstance();
            map.getOrCreateTag().putInt("map_scale_direction", 3);
            map.onCraftedBy(arg2.level(),arg2,1);
            arg2.getInventory().setItem(9, map);
        }

        return playerTag;
    }
    @Inject(method = "respawn", at = @At("RETURN"))
    public void respawn(ServerPlayer serverPlayer, boolean bl, CallbackInfoReturnable<ServerPlayer> cir) {
        ((LegacyPlayerInfo)cir.getReturnValue()).copyFrom(((LegacyPlayerInfo)serverPlayer));
        ((LegacyPlayer)cir.getReturnValue()).copyFrom(((LegacyPlayer)serverPlayer));
    }
    @Redirect(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;save(Lnet/minecraft/server/level/ServerPlayer;)V"))
    public void remove(PlayerList list, ServerPlayer arg2) {
        if (!arg2.level().noSave()) save(arg2);
        else SECURE_EXECUTOR.execute(()-> save(arg2));
    }

}
