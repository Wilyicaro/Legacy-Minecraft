package wily.legacy.mixin.base.villager_spawn_egg;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
//? if >=1.21.2 {
/*import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
*///?} else {
import net.minecraft.world.entity.MobSpawnType;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public class VillagerEggMixin {
    @Inject(method = /*? if >=1.21.5 {*//*"spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"*//*?} else if >=1.21.2 {*//*"spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"*//*?} else {*/"spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;"/*?}*/, at = @At("RETURN"))
    private void spawn(ServerLevel level, ItemStack itemStack, /*? if <1.21.5 {*/Player/*?} else {*//*LivingEntity*//*?}*/ livingEntity, BlockPos pos, /*? if >=1.21.2 {*//*EntitySpawnReason*//*?} else {*/MobSpawnType/*?}*/ reason, boolean alignPosition, boolean invertY, CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() instanceof Villager villager && (reason == /*? if >=1.21.2 {*//*EntitySpawnReason.SPAWN_ITEM_USE*//*?} else {*/MobSpawnType.SPAWN_EGG/*?}*/ || reason == /*? if >=1.21.2 {*//*EntitySpawnReason.DISPENSER*//*?} else {*/MobSpawnType.DISPENSER/*?}*/) && /*? if <1.21.5 {*/villager.getVillagerData().getProfession() == VillagerProfession.NONE/*?} else {*//*villager.getVillagerData().profession().is(VillagerProfession.NONE)*//*?}*/) {
            /*? if >=1.21.5 {*/
            /*ResourceKey<VillagerProfession>[] professions = new ResourceKey[]{VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.CARTOGRAPHER, VillagerProfession.CLERIC, VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.FLETCHER, VillagerProfession.LEATHERWORKER, VillagerProfession.LIBRARIAN, VillagerProfession.MASON, VillagerProfession.SHEPHERD, VillagerProfession.TOOLSMITH, VillagerProfession.WEAPONSMITH};
            *//*?} else {*/
            VillagerProfession[] professions = new VillagerProfession[]{VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.CARTOGRAPHER, VillagerProfession.CLERIC, VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.FLETCHER, VillagerProfession.LEATHERWORKER, VillagerProfession.LIBRARIAN, VillagerProfession.MASON, VillagerProfession.SHEPHERD, VillagerProfession.TOOLSMITH, VillagerProfession.WEAPONSMITH};
            //?}
            villager.setVillagerData(villager.getVillagerData()./*? if <1.21.5 {*/setProfession/*?} else {*//*withProfession*//*?}*/(/*? if >=1.21.5 {*//*level.registryAccess(), *//*?}*/professions[villager.getRandom().nextInt(professions.length)]));
            villager.addTag("legacy_spawn_egg_profession");
        }
    }
}
