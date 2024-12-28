package wily.legacy.mixin.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
//? if >1.20.2 {
import net.minecraft.server.network.CommonListenerCookie;
//?}
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.entity.LegacyPlayer;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.init.LegacyGameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {


    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    public void placeNewPlayer(Connection connection, ServerPlayer serverPlayer, /*? if >1.20.2 {*/CommonListenerCookie commonListenerCookie, /*?}*/CallbackInfo ci) {
        Legacy4J.onServerPlayerJoin(serverPlayer);
    }
    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = /*? if <1.20.5 {*//*"Lnet/minecraft/server/players/PlayerList;load(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/nbt/CompoundTag;"*//*?} else {*/"Lnet/minecraft/server/players/PlayerList;load(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/Optional;"/*?}*/))
    public /*? if <1.20.5 {*//*CompoundTag*//*?} else {*/Optional<CompoundTag>/*?}*/ placeNewPlayer(PlayerList list, ServerPlayer arg2) {
        var playerTag = list.load(arg2);
        if (/*? if <1.20.5 {*//*playerTag == null*//*?} else {*/playerTag.isEmpty()/*?}*/){
            List<ItemStack> itemsToAdd = new ArrayList<>();
            if (arg2.getServer().getGameRules().getBoolean(LegacyGameRules.PLAYER_STARTING_MAP)) itemsToAdd.add(Items.MAP.getDefaultInstance());
            if (arg2.getServer().getGameRules().getBoolean(LegacyGameRules.PLAYER_STARTING_BUNDLE) && Items.BUNDLE.isEnabled(arg2.level().enabledFeatures())) itemsToAdd.add(Items.BUNDLE.getDefaultInstance());


            for (int j = 0; j < 27; j++) {
                if (itemsToAdd.isEmpty()) break;
                for (int i = 0; i < itemsToAdd.size(); i++) {
                    if (arg2.getInventory().add(9 + j, itemsToAdd.get(i))) {
                        itemsToAdd.remove(i);
                        break;
                    }
                }
            }
        }
        return playerTag;
    }
    @Inject(method = "respawn", at = @At("RETURN"))
    public void respawn(ServerPlayer serverPlayer, boolean bl, /*? if >=1.20.5 {*/Entity.RemovalReason removalReason,/*?}*/ CallbackInfoReturnable<ServerPlayer> cir) {
        ((LegacyPlayerInfo)cir.getReturnValue()).copyFrom(((LegacyPlayerInfo)serverPlayer));
        ((LegacyPlayer)cir.getReturnValue()).copyFrom(((LegacyPlayer)serverPlayer));
    }
}
