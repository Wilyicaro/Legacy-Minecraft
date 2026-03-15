package wily.legacy.mixin.base.mobcaps;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
    @Inject(method = "spawnCategoryForChunk", at = @At("HEAD"), cancellable = true)
    private static void gateNaturalCategorySpawn(MobCategory category, ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnPredicate spawnPredicate, NaturalSpawner.AfterSpawnCallback afterSpawnCallback, CallbackInfo ci) {
        if (!ConsoleMobCaps.canNaturalCategorySpawn(level, category)) {
            ci.cancel();
        }
    }

    @Redirect(
        method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner;getMobForSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/Mob;"
        )
    )
    private static Mob gateNaturalSpawn(
        ServerLevel level,
        EntityType<?> type,
        MobCategory category,
        ServerLevel serverLevel,
        ChunkAccess chunkAccess,
        BlockPos blockPos,
        NaturalSpawner.SpawnPredicate spawnPredicate,
        NaturalSpawner.AfterSpawnCallback afterSpawnCallback
    ) {
        if (!ConsoleMobCaps.canNaturalMobSpawn(serverLevel, category, type)) {
            return null;
        }

        try {
            Entity entity = type.create(level, EntitySpawnReason.NATURAL);
            return entity instanceof Mob mob ? mob : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
