package wily.legacy.mixin;

import com.google.common.collect.Lists;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

import java.util.List;
import java.util.Optional;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow protected abstract void save(ServerPlayer arg);

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    public void placeNewPlayer(Connection connection, ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
        Legacy4J.onServerPlayerJoin(serverPlayer);
    }
    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;load(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/Optional;"))
    public Optional<CompoundTag> placeNewPlayer(PlayerList list, ServerPlayer arg2) {
        Optional<CompoundTag> playerTag = list.load(arg2);
        if (playerTag.isEmpty()){
            ItemStack map = Items.MAP.getDefaultInstance();
            CompoundTag scaleTag = new CompoundTag();
            scaleTag.putByte("map_scale", (byte) 3);
            map.set(DataComponents.CUSTOM_DATA, CustomData.of(scaleTag));
            arg2.getInventory().setItem(9, map);
        }

        return playerTag;
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    public void respawn(ServerPlayer serverPlayer, boolean bl, CallbackInfoReturnable<ServerPlayer> cir) {
        ((LegacyPlayerInfo)cir.getReturnValue()).copyFrom(((LegacyPlayerInfo)serverPlayer));
        ((LegacyPlayer)cir.getReturnValue()).copyFrom(((LegacyPlayer)serverPlayer));
    }

}
