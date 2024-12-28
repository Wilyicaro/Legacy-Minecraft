//? if >=1.21.2 {
package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartBehavior;
import net.minecraft.world.entity.vehicle.OldMinecartBehavior;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;

@Mixin(OldMinecartBehavior.class)
public abstract class OldMinecartBehaviourMixin extends MinecartBehavior {
    protected OldMinecartBehaviourMixin(AbstractMinecart arg) {
        super(arg);
    }

    @Shadow public abstract void tick();

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    protected void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(8d / 20d);
    }
    boolean doubleTick = true;

    @Inject(method = "tick", at = @At("RETURN"))
    public void tick(CallbackInfo ci){
        if (doubleTick){
            doubleTick = false;
            tick();
            doubleTick = true;
        }
    }
    @Redirect(method = "moveAlongTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 4))
    public Vec3 movePlayerAlongTrack(Vec3 instance, double d, double e, double f) {
        ServerPlayer p = (ServerPlayer) minecart.getFirstPassenger();
        if (!p.getLastClientInput().forward() || (this.getDeltaMovement().horizontalDistanceSqr()) >= 0.01D) return instance;
        Vec3 movement = Legacy4J.getRelativeMovement(p,1.0f,new Vec3(0,0,1),0);
        return instance.add(movement.x*0.1f,e,movement.z*0.1f);
    }
    @Redirect(method = "moveAlongTrack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;isInWater()Z"))
    public boolean moveAlongTrack(AbstractMinecart instance) {
        return false;
    }
}
//?}
