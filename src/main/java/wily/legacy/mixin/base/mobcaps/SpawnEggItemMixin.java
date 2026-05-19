package wily.legacy.mixin.base.mobcaps;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(SpawnEggItem.class)
public class SpawnEggItemMixin {
    private static final int TRADER_DESPAWN_DELAY = 48000;
    private static final int[][] TRADER_LLAMA_OFFSETS = {{1, 0}, {-1, 0}};

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void gateSpawnEggUseFromHand(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        EntityType<?> type = SpawnEggItem.getType(player.getItemInHand(hand));
        if (type == null) {
            return;
        }

        String failure = ConsoleMobCaps.spawnEggFailure(serverLevel, type);
        if (failure == null) {
            return;
        }

        ConsoleMobCaps.sendFailure(player, failure);
        cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "spawnMob", at = @At("HEAD"), cancellable = true)
    private static void gateSpawnEggUse(LivingEntity user, ItemStack stack, Level level, BlockPos pos, boolean alignPosition, boolean invertY, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        EntityType<?> type = SpawnEggItem.getType(stack);
        if (type == null) {
            return;
        }

        String failure = ConsoleMobCaps.spawnEggFailure(serverLevel, type);
        if (failure == null) {
            return;
        }

        if (user instanceof Player player) {
            ConsoleMobCaps.sendFailure(player, failure);
        }
        cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "spawnOffspringFromSpawnEgg", at = @At("HEAD"), cancellable = true)
    private static void gateSpawnEggOffspring(Player player, Mob parent, EntityType<? extends Mob> type, ServerLevel level, Vec3 pos, ItemStack stack, CallbackInfoReturnable<Optional<Mob>> cir) {
        String failure = ConsoleMobCaps.spawnEggFailure(level, type);
        if (failure == null) {
            return;
        }

        ConsoleMobCaps.sendFailure(player, failure);
        cir.setReturnValue(Optional.empty());
    }

    @WrapOperation(method = "spawnMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"))
    private static Entity spawnTraderWithLlamas(EntityType<?> type, ServerLevel level, ItemStack stack, LivingEntity user, BlockPos pos, EntitySpawnReason reason, boolean alignPosition, boolean invertY, Operation<Entity> original) {
        Entity entity = original.call(type, level, stack, user, pos, reason, alignPosition, invertY);
        if (entity instanceof WanderingTrader trader) {
            BlockPos home = trader.blockPosition();
            trader.setDespawnDelay(TRADER_DESPAWN_DELAY);
            trader.setWanderTarget(home);
            trader.setHomeTo(home, 16);
            spawnTraderLlamas(level, trader);
        }
        return entity;
    }

    private static void spawnTraderLlamas(ServerLevel level, WanderingTrader trader) {
        for (int[] offset : TRADER_LLAMA_OFFSETS) {
            BlockPos pos = trader.blockPosition().offset(offset[0], 0, offset[1]);
            TraderLlama llama = EntityType.TRADER_LLAMA.spawn(level, pos, EntitySpawnReason.EVENT);
            if (llama != null) {
                llama.setLeashedTo(trader, true);
            }
        }
    }
}
