package wily.legacy.mixin.base.mobcaps;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
    private Entity gatePhantomSpawn(EntityType<?> type, Level level, EntitySpawnReason reason) {
        if (level instanceof ServerLevel serverLevel && !ConsoleMobCaps.canNaturalMobSpawn(serverLevel, MobCategory.MONSTER, type)) {
            return null;
        }
        return type.create(level, reason);
    }
}
