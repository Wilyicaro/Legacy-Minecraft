package wily.legacy.mixin.base;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.init.LegacyGameRules;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract GameRules getGameRules();

    @Shadow public abstract ServerLevel overworld();

    @Inject(method = "isPvpAllowed", at = @At("HEAD"), cancellable = true)
    public void isPvpAllowed(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getGameRules().getBoolean(LegacyGameRules.PLAYER_VS_PLAYER));
    }
}
