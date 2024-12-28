package wily.legacy.mixin.base;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WitherBoss.class)
public class WitherBossMixin {
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerBossEvent;<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/BossEvent$BossBarColor;Lnet/minecraft/world/BossEvent$BossBarOverlay;)V"))
    private Component init(Component component){
        return component.copy().withStyle(ChatFormatting.DARK_PURPLE);
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerBossEvent;<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/BossEvent$BossBarColor;Lnet/minecraft/world/BossEvent$BossBarOverlay;)V"), index = 1)
    private BossEvent.BossBarColor initColor(BossEvent.BossBarColor bossBarColor){return BossEvent.BossBarColor.PINK;}
}
