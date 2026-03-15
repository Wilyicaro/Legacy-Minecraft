package wily.legacy.mixin.base.mobcaps;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(ThrownEgg.class)
public class ThrownEggMixin {
    @Redirect(
        method = "onHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"
        )
    )
    private Entity gateChickenHatching(EntityType<?> type, Level level, EntitySpawnReason reason, HitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && type == EntityType.CHICKEN && !ConsoleMobCaps.canHatchChicken(serverLevel)) {
            return null;
        }

        return type.create(level, reason);
    }
}
