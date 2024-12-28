package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
//? if >=1.21.2 {
import net.minecraft.world.entity.EntitySpawnReason;
//?} else {
/*import net.minecraft.world.entity.MobSpawnType;
*///?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
    @Unique
    boolean wasLastEnemySpawnFailed = false;
    @Inject(method = /*? if >=1.21.2 {*/ "create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"/*?} else {*//*"create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"*//*?}*/, at = @At("RETURN"), cancellable = true)
    public void create(Level level,/*? if >=1.21.2 {*/ EntitySpawnReason entitySpawnReason,/*?}*/ CallbackInfoReturnable<Entity> cir) {
        Entity entity = cir.getReturnValue();
        if (entity instanceof Mob m && level.getDifficulty() == Difficulty.PEACEFUL && m.shouldDespawnInPeaceful()) {
            cir.setReturnValue(null);
            wasLastEnemySpawnFailed = true;
        }else wasLastEnemySpawnFailed = false;
    }
    @Inject(method = /*? if >=1.21.2 {*/ "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"/*?} else {*//*"spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;"*//*?}*/, at = @At("RETURN"))
    public void spawn(ServerLevel arg, ItemStack arg2, Player arg3, BlockPos arg4, /*? if >=1.21.2 {*/ EntitySpawnReason/*?} else {*//*MobSpawnType*//*?}*/ arg5, boolean bl, boolean bl2, CallbackInfoReturnable<Entity> cir) {
        if (arg5 == /*? if >=1.21.2 {*/ EntitySpawnReason.SPAWN_ITEM_USE/*?} else {*//*MobSpawnType.SPAWN_EGG*//*?}*/ && wasLastEnemySpawnFailed && cir.getReturnValue() == null && arg3 != null)
            arg3.displayClientMessage(Component.translatable("item.minecraft.spawn_egg.peaceful.tip"), true);
    }
}
