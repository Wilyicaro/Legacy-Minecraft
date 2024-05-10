package wily.legacy.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;load(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/nbt/CompoundTag;"))
    public CompoundTag placeNewPlayer(PlayerList list, ServerPlayer arg2) {
        CompoundTag playerTag = list.load(arg2);
        if (playerTag == null){
            arg2.getInventory().setItem(9, Items.MAP.getDefaultInstance());
        }
        return playerTag;
    }
}
