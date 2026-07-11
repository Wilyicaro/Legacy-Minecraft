package wily.legacy.mixin.base.mobcaps;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.mobcaps.ConsoleMobCaps;
import wily.legacy.mobcaps.LegacyMobCaps;

import java.util.List;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
    @Inject(method = "spawnCategoryForChunk", at = @At("HEAD"), cancellable = true)
    private static void gateNaturalCategorySpawn(MobCategory category, ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnPredicate spawnPredicate, NaturalSpawner.AfterSpawnCallback afterSpawnCallback, CallbackInfo ci) {
        if (!ConsoleMobCaps.canNaturalCategorySpawn(level, category)) ci.cancel();
    }

    //? if <1.21.3 {
    @WrapOperation(
        method = "spawnForChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner$SpawnState;canSpawnForCategory(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/ChunkPos;)Z"
        )
    )
    private static boolean gateCategorySpawn(
        NaturalSpawner.SpawnState state,
        MobCategory category,
        ChunkPos chunkPos,
        Operation<Boolean> original,
        ServerLevel level,
        LevelChunk chunk,
        NaturalSpawner.SpawnState spawnState,
        boolean spawnFriendlies,
        boolean spawnEnemies,
        boolean spawnPersistent
    ) {
        return original.call(state, category, chunkPos) || canRefillPassive(level, category);
    }
    //?} else {
    /*
    @WrapOperation(
        method = "spawnForChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner$SpawnState;canSpawnForCategoryLocal(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/ChunkPos;)Z"
        )
    )
    private static boolean gateLocalCategorySpawn(
        NaturalSpawner.SpawnState state,
        MobCategory category,
        ChunkPos chunkPos,
        Operation<Boolean> original,
        ServerLevel level,
        LevelChunk chunk,
        NaturalSpawner.SpawnState spawnState,
        List<MobCategory> categories
    ) {
        return original.call(state, category, chunkPos) || canRefillPassive(level, category);
    }
    *///?}

    private static boolean canRefillPassive(ServerLevel level, MobCategory category) {
        return category == MobCategory.CREATURE
            && LegacyMobCaps.isEnabled(level)
            && Level.OVERWORLD.equals(level.dimension())
            && ConsoleMobCaps.canNaturalCategorySpawn(level, category);
    }

    @WrapOperation(
        method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner;getMobForSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/Mob;"
        )
    )
    private static Mob gateNaturalSpawn(
        ServerLevel level,
        EntityType<?> type,
        Operation<Mob> original,
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

        return original.call(level, type);
    }
}
