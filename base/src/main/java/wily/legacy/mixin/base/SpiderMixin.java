package wily.legacy.mixin.base;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Spider.class)
public class SpiderMixin {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!isNetherPlayerSpawn(level, reason)) return;
        Spider spider = (Spider) (Object) this;
        if (!(spider.getFirstPassenger() instanceof Skeleton skeleton)) return;
        Mob witherSkeleton = EntityType.WITHER_SKELETON.create(spider.level(), EntitySpawnReason.JOCKEY);
        if (witherSkeleton == null) return;
        skeleton.stopRiding();
        skeleton.discard();
        witherSkeleton.snapTo(spider.getX(), spider.getY(), spider.getZ(), spider.getYRot(), 0.0f);
        witherSkeleton.finalizeSpawn(level, difficulty, reason, null);
        witherSkeleton.startRiding(spider, false, false);
    }

    private boolean isNetherPlayerSpawn(ServerLevelAccessor level, EntitySpawnReason reason) {
        return (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.COMMAND) && level.getLevel().dimension() == Level.NETHER;
    }
}
