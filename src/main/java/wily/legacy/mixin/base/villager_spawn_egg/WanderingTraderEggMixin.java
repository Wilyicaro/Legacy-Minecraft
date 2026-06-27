package wily.legacy.mixin.base.villager_spawn_egg;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
//? if >=1.21.2 {
/*import net.minecraft.world.entity.EntitySpawnReason;
*///?} else {
import net.minecraft.world.entity.MobSpawnType;
//?}
//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
*///?} else {
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.npc.WanderingTrader;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public class WanderingTraderEggMixin {
    @Unique
    private static final int LEGACY_TRADER_DESPAWN_DELAY = 48000;
    @Unique
    private static final int[][] LEGACY_TRADER_LLAMA_OFFSETS = {{1, 0}, {-1, 0}};

    @Inject(method = /*? if >=1.21.5 {*//*"spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"*//*?} else if >=1.21.2 {*//*"spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"*//*?} else {*/"spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;"/*?}*/, at = @At("RETURN"))
    private void legacy$spawnTraderLlamas(ServerLevel level, ItemStack itemStack, /*? if <1.21.5 {*/Player/*?} else {*//*LivingEntity*//*?}*/ livingEntity, BlockPos pos, /*? if >=1.21.2 {*//*EntitySpawnReason*//*?} else {*/MobSpawnType/*?}*/ reason, boolean alignPosition, boolean invertY, CallbackInfoReturnable<Entity> cir) {
        if (!(cir.getReturnValue() instanceof WanderingTrader trader) || !(reason == /*? if >=1.21.2 {*//*EntitySpawnReason.SPAWN_ITEM_USE*//*?} else {*/MobSpawnType.SPAWN_EGG/*?}*/ || reason == /*? if >=1.21.2 {*//*EntitySpawnReason.DISPENSER*//*?} else {*/MobSpawnType.DISPENSER/*?}*/)) {
            return;
        }

        BlockPos home = trader.blockPosition();
        trader.setDespawnDelay(LEGACY_TRADER_DESPAWN_DELAY);
        trader.setWanderTarget(home);
        //? if >=1.21.10 {
        /*trader.setHomeTo(home, 16);
        *///?}
        legacy$spawnTraderLlamas(level, trader);
    }

    @Unique
    private static void legacy$spawnTraderLlamas(ServerLevel level, WanderingTrader trader) {
        for (int[] offset : LEGACY_TRADER_LLAMA_OFFSETS) {
            BlockPos pos = trader.blockPosition().offset(offset[0], 0, offset[1]);
            TraderLlama llama = EntityType.TRADER_LLAMA.spawn(level, pos, /*? if >=1.21.2 {*//*EntitySpawnReason.EVENT*//*?} else {*/MobSpawnType.EVENT/*?}*/);
            if (llama != null) {
                llama.setLeashedTo(trader, true);
            }
        }
    }
}
