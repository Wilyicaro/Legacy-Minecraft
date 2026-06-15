package wily.legacy.mixin.base.mobcaps;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <1.21.2 {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.ConsoleMobCaps;

import java.util.Optional;

@Mixin(SpawnEggItem.class)
public class SpawnEggItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void gateSpawnEggUseFromHand(Level level, Player player, InteractionHand hand, CallbackInfoReturnable</*? if <1.21.2 {*/InteractionResultHolder<ItemStack>/*?} else {*//*InteractionResult*//*?}*/> cir) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ItemStack stack = player.getItemInHand(hand);
        EntityType<?> type = legacy$getType(level, stack);
        if (type == null) return;
        String failure = ConsoleMobCaps.spawnEggFailure(serverLevel, type);
        if (failure == null) return;
        ConsoleMobCaps.sendFailure(player, failure);
        cir.setReturnValue(/*? if <1.21.2 {*/InteractionResultHolder.fail(stack)/*?} else {*//*InteractionResult.FAIL*//*?}*/);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void gateSpawnEggUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) return;
        Player player = context.getPlayer();
        EntityType<?> type = legacy$getType(context.getLevel(), context.getItemInHand());
        if (type == null) return;
        String failure = ConsoleMobCaps.spawnEggFailure(serverLevel, type);
        if (failure == null) return;
        if (player != null) ConsoleMobCaps.sendFailure(player, failure);
        cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "spawnOffspringFromSpawnEgg", at = @At("HEAD"), cancellable = true)
    private static void gateSpawnEggOffspring(Player player, Mob parent, EntityType<? extends Mob> type, ServerLevel level, Vec3 pos, ItemStack stack, CallbackInfoReturnable<Optional<Mob>> cir) {
        String failure = ConsoleMobCaps.spawnEggFailure(level, type);
        if (failure == null) return;
        ConsoleMobCaps.sendFailure(player, failure);
        cir.setReturnValue(Optional.empty());
    }

    @Unique
    private EntityType<?> legacy$getType(Level level, ItemStack stack) {
        return ((SpawnEggItem)(Object)this).getType(/*? if >=1.21.4 {*//*level.registryAccess(), *//*?}*/stack/*? if <1.20.5 {*//*.getTag()*//*?}*/);
    }
}
