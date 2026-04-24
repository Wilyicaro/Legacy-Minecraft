package wily.legacy.mixin.base;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
//? if >1.20.2 {
//?}
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.skins.skin.SkinSync;
import wily.legacy.entity.LegacyPlayer;
import wily.legacy.entity.LegacyPlayerInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "respawn", at = @At("RETURN"))
    public void respawn(ServerPlayer serverPlayer, boolean bl, /*? if >=1.20.5 {*/Entity.RemovalReason removalReason,/*?}*/ CallbackInfoReturnable<ServerPlayer> cir) {
        ((LegacyPlayerInfo) cir.getReturnValue()).copyFrom(((LegacyPlayerInfo) serverPlayer));
        LegacyPlayerInfo.updateMayFlySurvival(cir.getReturnValue(), ((LegacyPlayerInfo) serverPlayer).mayFlySurvival(), true);
        ((LegacyPlayer) cir.getReturnValue()).copyFrom(((LegacyPlayer) serverPlayer));
        CriteriaTriggers.CHANGED_DIMENSION.trigger(cir.getReturnValue(), serverPlayer.level().dimension(), cir.getReturnValue().level().dimension());
    }

    @Inject(method = "remove", at = @At("HEAD"), require = 0)
    public void legacy4j$clearSkinSyncState(ServerPlayer serverPlayer, CallbackInfo ci) {
        if (serverPlayer != null) SkinSync.clearPlayer(serverPlayer.getUUID());
    }
}
