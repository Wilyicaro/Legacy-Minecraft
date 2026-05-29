package wily.legacy.mixin.base;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.globalleaderboards.GlobalDifficultyStatsStore;
import wily.legacy.init.LegacyGameRules;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "onGameRuleChanged", at = @At("RETURN"))
    private <T> void onGameRuleChanged(GameRule<T> gameRule, T object, CallbackInfo ci) {
        LegacyGameRules.onGameRuleChanged((MinecraftServer) (Object)this, gameRule, object);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void legacy$stopServer(CallbackInfo ci) {
        GlobalDifficultyStatsStore.saveAll();
    }
}
