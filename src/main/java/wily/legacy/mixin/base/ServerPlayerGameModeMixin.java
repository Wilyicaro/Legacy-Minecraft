package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.util.LegacyBlockProtection;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;

    @Shadow
    public abstract boolean isCreative();

    @Inject(method = "setGameModeForPlayer", at = @At("RETURN"))
    protected void setGameModeForPlayer(GameType gameType, GameType gameType2, CallbackInfo ci) {
        LegacyPlayerInfo.updateMayFlySurvival(player, LegacyPlayerInfo.of(player).mayFlySurvival(), false);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    protected void destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        if (decreaseCreativeTurtleEggs(pos, state)) {
            cir.setReturnValue(true);
        } else if (LegacyBlockProtection.blocksBreak(level, pos, state, isCreative())) {
            cir.setReturnValue(false);
        }
    }

    private boolean decreaseCreativeTurtleEggs(BlockPos pos, BlockState state) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyWorldInteractions) || !isCreative() || !state.is(Blocks.TURTLE_EGG)) return false;
        int eggs = state.getValue(TurtleEggBlock.EGGS);
        if (eggs <= 1) return false;
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        if (player.getMainHandItem().isEmpty()) {
            SoundType soundType = SoundType.METAL;
            level.playSound(null, pos, soundType.getBreakSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f);
        }
        level.setBlock(pos, state.setValue(TurtleEggBlock.EGGS, eggs - 1), 2);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));
        level.levelEvent(2001, pos, Block.getId(state));
        return true;
    }
}
