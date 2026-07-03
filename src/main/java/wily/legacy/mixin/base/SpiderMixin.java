package wily.legacy.mixin.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
//? if >=1.21.2 {
/*import net.minecraft.world.entity.EntitySpawnReason;
*///?} else {
import net.minecraft.world.entity.MobSpawnType;
//?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
*///?} else {
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;

@Mixin(Spider.class)
public class SpiderMixin {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, /*? if >=1.21.2 {*//*EntitySpawnReason*//*?} else {*/MobSpawnType/*?}*/ reason, SpawnGroupData spawnGroupData, /*? if <1.20.5 {*//*CompoundTag compoundTag, *//*?}*/CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMobInteractions) || !isNetherPlayerSpawn(level, reason)) return;
        Spider spider = (Spider) (Object) this;
        if (!(spider.getFirstPassenger() instanceof Skeleton skeleton)) return;
        Mob witherSkeleton = EntityType.WITHER_SKELETON.create(spider.level()/*? if >=1.21.2 {*//*, EntitySpawnReason.JOCKEY*//*?}*/);
        if (witherSkeleton == null) return;
        skeleton.stopRiding();
        skeleton.discard();
        //? if >=1.21.5 {
        /*witherSkeleton.snapTo(spider.getX(), spider.getY(), spider.getZ(), spider.getYRot(), 0.0f);
        *///?} else {
        witherSkeleton.moveTo(spider.getX(), spider.getY(), spider.getZ(), spider.getYRot(), 0.0f);
        //?}
        witherSkeleton.finalizeSpawn(level, difficulty, reason, null/*? if <1.20.5 {*//*, null*//*?}*/);
        witherSkeleton.startRiding(spider, false/*? if >=1.21.6 {*//*, false*//*?}*/);
    }

    private boolean isNetherPlayerSpawn(ServerLevelAccessor level, /*? if >=1.21.2 {*//*EntitySpawnReason*//*?} else {*/MobSpawnType/*?}*/ reason) {
        return (reason == /*? if >=1.21.2 {*//*EntitySpawnReason.SPAWN_ITEM_USE*//*?} else {*/MobSpawnType.SPAWN_EGG/*?}*/ || reason == /*? if >=1.21.2 {*//*EntitySpawnReason.COMMAND*//*?} else {*/MobSpawnType.COMMAND/*?}*/) && level.getLevel().dimension() == Level.NETHER;
    }
}
