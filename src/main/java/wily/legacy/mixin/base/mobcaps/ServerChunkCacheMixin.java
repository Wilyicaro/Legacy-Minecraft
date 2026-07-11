package wily.legacy.mixin.base.mobcaps;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.mobcaps.ConsoleMobCaps;
import wily.legacy.mobcaps.LegacyMobCaps;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
    @Shadow
    @Final
    private ServerLevel level;

    //? if >=1.21.3 && <1.21.5 {
    /*@ModifyExpressionValue(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;JLjava/util/List;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;getFilteredSpawningCategories(Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)Ljava/util/List;"))
    *///?} else if >=1.21.5 {
    /*@ModifyExpressionValue(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;getFilteredSpawningCategories(Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)Ljava/util/List;"))
    *///?}
    private List<MobCategory> getFilteredSpawningCategories(List<MobCategory> categories) {
        if (!LegacyMobCaps.isEnabled(level) || !Level.OVERWORLD.equals(level.dimension()) || categories.contains(MobCategory.CREATURE)) {
            return categories;
        }
        if (!ConsoleMobCaps.canNaturalCategorySpawn(level, MobCategory.CREATURE)) {
            return categories;
        }

        List<MobCategory> result = new ArrayList<>(categories);
        result.add(MobCategory.CREATURE);
        return result;
    }
}
