package wily.legacy.mixin.base;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
//? if >1.20.2 {
//?}
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.config.LegacyWorldOptions;
import wily.legacy.entity.LegacyPlayer;
import wily.legacy.entity.LegacyPlayerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {


    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;load(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/util/ProblemReporter;)Ljava/util/Optional;"))
    public Optional<ValueInput> placeNewPlayer(PlayerList instance, ServerPlayer optional, ProblemReporter problemReporter) {
        var playerTag = instance.load(optional, problemReporter);
        if (playerTag.isEmpty()){
            List<ItemStack> itemsToAdd = new ArrayList<>(LegacyWorldOptions.initialItems.get().stream().filter(i-> i.isEnabled(optional.getServer())).map(LegacyWorldOptions.InitialItem::item).toList());

            for (int j = 0; j < 27; j++) {
                if (itemsToAdd.isEmpty()) break;
                for (int i = 0; i < itemsToAdd.size(); i++) {
                    if (optional.getInventory().add(9 + j, itemsToAdd.get(i).copy())) {
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
        CriteriaTriggers.CHANGED_DIMENSION.trigger(cir.getReturnValue(), serverPlayer.level().dimension(), cir.getReturnValue().level().dimension());
    }
}
