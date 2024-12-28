package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartFurnace.class)
public abstract class MinecartFurnaceMixin extends AbstractMinecart {
    protected MinecartFurnaceMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    protected void getMaxSpeed(/*? if >=1.21.2 {*/ ServerLevel level,/*?}*/ CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(super.getMaxSpeed(/*? if >=1.21.2 {*/level/*?}*/) /  2);
    }
}
