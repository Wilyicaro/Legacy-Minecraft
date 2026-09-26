package wily.legacy.mixin.base;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    //? if <1.21.1 {
    /*@Accessor
    BlockPos getPortalEntrancePos();

    @Accessor
    void setPortalEntrancePos(BlockPos pos);
    *///?}

    @Invoker("canAddPassenger")
    boolean canVehicleAddPassenger(Entity entity);

    @Invoker("canRide")
    boolean canRideVehicle(Entity entity);
}
