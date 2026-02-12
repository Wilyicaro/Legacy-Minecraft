//? >=1.21.11 {
package wily.legacy.mixin.base;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.network.PlayerInfoSync;

@Mixin(MinecraftServer.class)
public class ServerMinecraftServerMixin {
    
    @Inject(method = "onGameRuleChanged", at = @At("TAIL"))
    private <T> void onLegacyGameRuleChanged(GameRule<T> gameRule, T value, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer)(Object)this;
        if (gameRule == LegacyGameRules.DEFAULT_SHOW_ARMOR_STANDS_ARMS && value instanceof Boolean) {
            PlayerInfoSync.All.syncGamerule(LegacyGameRules.DEFAULT_SHOW_ARMOR_STANDS_ARMS, (Boolean)value, server);
        }
        else if (gameRule == LegacyGameRules.LEGACY_SWIMMING && value instanceof Boolean) {
            PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_SWIMMING, (Boolean)value, server);
        }
        else if (gameRule == LegacyGameRules.LEGACY_FLIGHT && value instanceof Boolean) {
            PlayerInfoSync.All.syncGamerule(LegacyGameRules.LEGACY_FLIGHT, (Boolean)value, server);
        }
    }
}
//?}
