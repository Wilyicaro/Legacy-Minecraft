package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.world.PlayerTrustAdmin;

@Mixin({BlockAttachedEntity.class, ItemFrame.class, VehicleEntity.class, EndCrystal.class})
public class TrustProtectedEntityMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void hurtServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerTrustPolicy player = PlayerTrustAdmin.getResponsiblePlayer(level, source);
        if (player != null && !player.canDestroyEntity((Entity) (Object) this)) cir.setReturnValue(false);
    }
}
