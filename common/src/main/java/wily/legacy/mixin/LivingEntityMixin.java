package wily.legacy.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.Legacy4JClient;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onGround()Z", ordinal = 2))
    public boolean travelFlight(LivingEntity instance) {
        return !isLegacyFlying() && onGround();
    }

    private boolean isLegacyFlying(){
        return ((LivingEntity)(Object)this instanceof Player p && p.getAbilities().flying && (!level().isClientSide || Legacy4JClient.isModEnabledOnServer()));
    }
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V", ordinal = 3))
    public void travelFlight(LivingEntity instance, double x, double y, double z) {
        setDeltaMovement((isLegacyFlying() ? 0.6 : 1) * x,(isLegacyFlying() ? 0.546 : 1) * y,(isLegacyFlying() ? 0.6 : 1) * z);
    }
}
