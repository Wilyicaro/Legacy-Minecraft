package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
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
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.util.LegacyBlockProtection;
import wily.legacy.world.PlayerTrustAdmin;

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

    @WrapOperation(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean mayStartDestroyBlock(ServerLevel level, Entity entity, BlockPos pos, Operation<Boolean> original) {
        return PlayerTrustPolicy.of(player).canBuildAndMine() && original.call(level, entity, pos);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    protected void destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerTrustPolicy.of(player).canBuildAndMine()) {
            cir.setReturnValue(false);
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (decreaseCreativeTurtleEggs(pos, state)) {
            cir.setReturnValue(true);
        } else if (LegacyBlockProtection.blocksBreak(level, pos, state, isCreative())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void useItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!PlayerTrustPolicy.of(player).canUseItem(stack)) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void useItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isSpectator() && !PlayerTrustPolicy.of(player).canInteractWithBlock(level.getBlockState(hitResult.getBlockPos()))) cir.setReturnValue(InteractionResult.FAIL);
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult useItemOnBlock(BlockState state, ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hitResult, Operation<InteractionResult> original) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (!PlayerTrustPolicy.of(serverPlayer).canInteractWithBlock(state)) return InteractionResult.FAIL;
        return original.call(state, stack, level, player, hand, hitResult);
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;onItemUseFirst(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"), require = 0)
    private InteractionResult useItemFirst(ItemStack stack, UseOnContext context, Operation<InteractionResult> original) {
        PlayerTrustPolicy trust = PlayerTrustPolicy.of(player);
        if (!trust.canInteractWithBlock(level.getBlockState(context.getClickedPos()))) return InteractionResult.FAIL;
        if (!trust.canBuildAndMine()) return InteractionResult.PASS;
        return original.call(stack, context);
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useWithoutItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult useBlock(BlockState state, Level level, Player player, BlockHitResult hitResult, Operation<InteractionResult> original) {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        PlayerTrustPolicy trust = PlayerTrustPolicy.of(serverPlayer);
        if (!trust.canInteractWithBlock(state)) return InteractionResult.FAIL;
        return PlayerTrustAdmin.withResponsiblePlayer(trust, () -> original.call(state, level, player, hitResult));
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult useItemOnBlock(ItemStack stack, UseOnContext context, Operation<InteractionResult> original) {
        if (!PlayerTrustPolicy.of(player).canBuildAndMine()) return InteractionResult.FAIL;
        return original.call(stack, context);
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
