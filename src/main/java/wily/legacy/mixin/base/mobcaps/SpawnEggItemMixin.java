package wily.legacy.mixin.base.mobcaps;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
}
