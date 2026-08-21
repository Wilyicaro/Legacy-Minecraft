package wily.legacy.mixin.base.client.bosshealth;

import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EnderDragonFight.class)
public class EndDragonFightMixin {
    @ModifyArg(method = "<init>(Lnet/minecraft/server/level/ServerLevel;JLnet/minecraft/world/level/dimension/end/EndDragonFight$Data;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BossEvent;setCreateWorldFog(Z)Lnet/minecraft/world/BossEvent;"))
    private boolean initCreateWorldFog(boolean bl) {
        return false;
    }
}
