package wily.legacy.mixin.base;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EndDragonFight.class)
public class EndDragonFightMixin {
    @ModifyArg(method = "<init>(Lnet/minecraft/server/level/ServerLevel;JLnet/minecraft/world/level/dimension/end/EndDragonFight$Data;Lnet/minecraft/core/BlockPos;)V",at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerBossEvent;<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/BossEvent$BossBarColor;Lnet/minecraft/world/BossEvent$BossBarOverlay;)V"))
    private Component init(Component component){
        return component.copy().withStyle(ChatFormatting.DARK_PURPLE);
    }
    @ModifyArg(method = "<init>(Lnet/minecraft/server/level/ServerLevel;JLnet/minecraft/world/level/dimension/end/EndDragonFight$Data;Lnet/minecraft/core/BlockPos;)V",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BossEvent;setCreateWorldFog(Z)Lnet/minecraft/world/BossEvent;"))
    private boolean initCreateWorldFog(boolean bl){
        return false;
    }
}
