package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.entity.LegacyPlayerInfo;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "setGameModeForPlayer", at = @At(value = "RETURN"))
    protected void setGameModeForPlayer(GameType gameType, GameType gameType2, CallbackInfo ci) {
        if (player.getAbilities().mayfly != LegacyPlayerInfo.of(player).mayFlySurvival() && gameType.isSurvival()){
            player.getAbilities().mayfly = LegacyPlayerInfo.of(player).mayFlySurvival();
            if (!player.getAbilities().mayfly && player.getAbilities().flying) player.getAbilities().flying = false;
        }
    }
}
