package wily.legacy.mixin.base.client.bosshealth;

import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.client.LegacyOptions;

@Mixin(Raid.class)
public class RaidMixin {
    @ModifyArg(method = "<init>*", at = @At(value = "INVOKE", target = /*? if >=26.1 {*/"Lnet/minecraft/server/level/ServerBossEvent;<init>(Ljava/util/UUID;Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/BossEvent$BossBarColor;Lnet/minecraft/world/BossEvent$BossBarOverlay;)V"/*?} else {*//*"Lnet/minecraft/server/level/ServerBossEvent;<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/BossEvent$BossBarColor;Lnet/minecraft/world/BossEvent$BossBarOverlay;)V"*//*?}*/))
    private BossEvent.BossBarColor initColor(BossEvent.BossBarColor bossBarColor) {
        return LegacyOptions.legacyBossBars.get() ? BossEvent.BossBarColor.PINK : bossBarColor;
    }

    @ModifyArg(method = "<init>*", at = @At(value = "INVOKE", target = /*? if >=26.1 {*/"Lnet/minecraft/server/level/ServerBossEvent;<init>(Ljava/util/UUID;Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/BossEvent$BossBarColor;Lnet/minecraft/world/BossEvent$BossBarOverlay;)V"/*?} else {*//*"Lnet/minecraft/server/level/ServerBossEvent;<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/BossEvent$BossBarColor;Lnet/minecraft/world/BossEvent$BossBarOverlay;)V"*//*?}*/))
    private BossEvent.BossBarOverlay initOverlay(BossEvent.BossBarOverlay bossBarOverlay) {
        return LegacyOptions.legacyBossBars.get() ? BossEvent.BossBarOverlay.PROGRESS : bossBarOverlay;
    }
}
