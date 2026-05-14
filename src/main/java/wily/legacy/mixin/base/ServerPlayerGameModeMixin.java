package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.LegacyBlockProtection;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    protected ServerLevel level;

    @Shadow
    public abstract boolean isCreative();

    @Inject(method = "setGameModeForPlayer", at = @At("RETURN"))
    protected void setGameModeForPlayer(GameType gameType, GameType gameType2, CallbackInfo ci) {
        LegacyPlayerInfo.setAndUpdateMayFlySurvival(player, LegacyPlayerInfo.of(player).mayFlySurvival(), false);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    protected void destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyBlockProtection.blocksBreak(level, pos, level.getBlockState(pos), isCreative())) {
            cir.setReturnValue(false);
        }
    }
}
