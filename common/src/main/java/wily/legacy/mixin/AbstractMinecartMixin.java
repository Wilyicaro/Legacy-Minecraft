package wily.legacy.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends VehicleEntity {
    @Shadow public abstract void tick();

    @Unique
    boolean canRepeatTick = true;

    public AbstractMinecartMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    protected void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(8d / 20d);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void tick(CallbackInfo ci) {
        if (canRepeatTick){
            canRepeatTick = false;
            tick();
            canRepeatTick = true;
        }
    }

}
