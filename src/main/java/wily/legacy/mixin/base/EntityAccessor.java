package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    //? if <1.21.1 {
    /*@Accessor
    BlockPos getPortalEntrancePos();

    @Accessor
    void setPortalEntrancePos(BlockPos pos);
    *///?}
}
