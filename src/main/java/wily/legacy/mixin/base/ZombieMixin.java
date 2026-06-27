package wily.legacy.mixin.base;

import net.minecraft.nbt.CompoundTag;
//? if >=1.21 {
import net.minecraft.tags.EntityTypeTags;
//?}
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
//? if >=1.21.2 {
/*import net.minecraft.world.entity.EntitySpawnReason;
*///?} else {
import net.minecraft.world.entity.MobSpawnType;
//?}
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Chicken;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.AbstractHorse;
*///?} else {
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//?}
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
//? if >=1.21.6 {
/*import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.util.LegacyTags;

import java.util.List;

@Mixin(Zombie.class)
public class ZombieMixin {
    @Unique
    private boolean legacy$wantsJockeyMount;

    @ModifyConstant(method = "finalizeSpawn", constant = @Constant(doubleValue = 0.05))
    private double legacy$disableVanillaChickenJockey(double chance) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMobInteractions) ? 0.0 : chance;
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void legacy$finalizeJockey(ServerLevelAccessor level, DifficultyInstance difficulty, /*? if >=1.21.2 {*//*EntitySpawnReason*//*?} else {*/MobSpawnType/*?}*/ reason, SpawnGroupData groupData, /*? if <1.20.5 {*//*CompoundTag compoundTag, *//*?}*/CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMobInteractions)) {
            return;
        }
        Zombie zombie = (Zombie) (Object) this;
        if (!(cir.getReturnValue() instanceof Zombie.ZombieGroupData data) || !data.canSpawnJockey || !zombie.isBaby()) return;
        legacy$wantsJockeyMount = level.getRandom().nextFloat() < 0.15f;
        legacy$tryStartRiding(level);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void legacy$tickJockeyMount(CallbackInfo ci) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMobInteractions)) {
            legacy$wantsJockeyMount = false;
            return;
        }
        Zombie zombie = (Zombie) (Object) this;
        if (!legacy$wantsJockeyMount || zombie.level().isClientSide() || zombie.tickCount % 20 != 0) return;
        if (!zombie.isBaby()) {
            legacy$wantsJockeyMount = false;
            return;
        }
        if (zombie.isPassenger() || !(zombie.level() instanceof ServerLevelAccessor level)) return;
        legacy$tryStartRiding(level);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void legacy$addJockeySaveData(/*? if >=1.21.6 {*//*ValueOutput output*//*?} else {*/CompoundTag compoundTag/*?}*/, CallbackInfo ci) {
        if (!legacy$wantsJockeyMount) return;
        //? if >=1.21.6 {
        /*output.putBoolean("LegacyJockeyMount", true);
        *///?} else {
        compoundTag.putBoolean("LegacyJockeyMount", true);
        //?}
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void legacy$readJockeySaveData(/*? if >=1.21.6 {*//*ValueInput input*//*?} else {*/CompoundTag compoundTag/*?}*/, CallbackInfo ci) {
        legacy$wantsJockeyMount = /*? if >=1.21.6 {*//*input.getBooleanOr("LegacyJockeyMount", false)*//*?} else if >=1.21.5 {*//*compoundTag.getBooleanOr("LegacyJockeyMount", false)*//*?} else {*/compoundTag.getBoolean("LegacyJockeyMount")/*?}*/;
    }

    public boolean canControlVehicle() {
        Zombie zombie = (Zombie) (Object) this;
        return (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMobInteractions) || !legacy$wantsJockeyMount) && !legacy$isNonControllingRider(zombie);
    }

    @Unique
    private boolean legacy$isNonControllingRider(Zombie zombie) {
        //? if >=1.21 {
        return zombie.getType().is(EntityTypeTags.NON_CONTROLLING_RIDER);
        //?} else {
        /*return false;
        *///?}
    }

    @Unique
    private void legacy$tryStartRiding(ServerLevelAccessor level) {
        Zombie zombie = (Zombie) (Object) this;
        if (!legacy$wantsJockeyMount || zombie.isPassenger()) return;
        List<Mob> mounts = level.getEntitiesOfClass(Mob.class, zombie.getBoundingBox().inflate(5.0, 3.0, 5.0), this::legacy$canUseMount);
        if (mounts.isEmpty()) return;
        Mob mount = mounts.get(zombie.getRandom().nextInt(mounts.size()));
        if (mount instanceof Chicken chicken) chicken.setChickenJockey(true);
        zombie.startRiding(mount, false/*? if >=1.21.6 {*//*, false*//*?}*/);
    }

    @Unique
    private boolean legacy$canUseMount(Mob mount) {
        if (mount == (Object) this) return false;
        if (!mount.getType().builtInRegistryHolder().is(LegacyTags.BABY_ZOMBIE_JOCKEY_MOUNTS) || !mount.isAlive() || mount.isPassenger() || mount.isVehicle()) return false;
        if (mount instanceof Zombie zombie && zombie.isBaby()) return false;
        if (mount instanceof AgeableMob ageable && ageable.isBaby()) return false;
        if (mount instanceof TamableAnimal tamable && tamable.isTame()) return false;
        if (mount instanceof AbstractHorse horse && horse.isTamed()) return false;
        if (mount instanceof Pig pig && pig.isSaddled()) return false;
        return !(mount instanceof Chicken chicken) || !chicken.isChickenJockey();
    }
}
