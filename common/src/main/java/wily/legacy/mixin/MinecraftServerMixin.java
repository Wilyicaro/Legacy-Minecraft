package wily.legacy.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.init.LegacyGameRules;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract GameRules getGameRules();

    @Shadow public abstract ServerLevel overworld();

    @Inject(method = "isPvpAllowed", at = @At("HEAD"), cancellable = true)
    public void isPvpAllowed(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getGameRules().getBoolean(LegacyGameRules.PLAYER_VS_PLAYER));
    }
    @Inject(method = "stopServer", at = @At("RETURN"))
    public void stopServer(CallbackInfo ci) {
        Legacy4J.SECURE_EXECUTOR.clear();
    }

    @Inject(method = "saveEverything", at = @At("RETURN"))
    private void saveEverything(boolean bl, boolean bl2, boolean bl3, CallbackInfoReturnable<Boolean> cir){
        Legacy4J.serverSave((MinecraftServer)(Object)this);
    }
}
