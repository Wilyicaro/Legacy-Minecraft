package wily.legacy.mixin.base.villager_spawn_egg;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public class VillagerEggMixin {
    @Unique
    private static final ResourceKey<VillagerProfession>[] LEGACY_SPAWN_EGG_PROFESSIONS = new ResourceKey[]{VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.CARTOGRAPHER, VillagerProfession.CLERIC, VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.FLETCHER, VillagerProfession.LEATHERWORKER, VillagerProfession.LIBRARIAN, VillagerProfession.MASON, VillagerProfession.SHEPHERD, VillagerProfession.TOOLSMITH, VillagerProfession.WEAPONSMITH};

    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"))
    private void spawn(ServerLevel level, ItemStack itemStack, LivingEntity livingEntity, BlockPos pos, EntitySpawnReason reason, boolean alignPosition, boolean invertY, CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() instanceof Villager villager && (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.DISPENSER) && villager.getVillagerData().profession().is(VillagerProfession.NONE)) {
            villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), LEGACY_SPAWN_EGG_PROFESSIONS[villager.getRandom().nextInt(LEGACY_SPAWN_EGG_PROFESSIONS.length)]));
            villager.addTag("legacy_spawn_egg_profession");
        }
    }
}
