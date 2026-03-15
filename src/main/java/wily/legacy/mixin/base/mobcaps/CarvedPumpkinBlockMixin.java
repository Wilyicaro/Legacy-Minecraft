package wily.legacy.mixin.base.mobcaps;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.mobcaps.ConsoleMobCaps;

@Mixin(CarvedPumpkinBlock.class)
public class CarvedPumpkinBlockMixin {
    @Redirect(
        method = "trySpawnGolem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"
        )
    )
    private Entity gateTriggeredSummons(EntityType<?> type, Level level, EntitySpawnReason reason, Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverLevel && !ConsoleMobCaps.canTriggerSummon(serverLevel, type)) {
            return null;
        }

        return type.create(level, reason);
    }
}
