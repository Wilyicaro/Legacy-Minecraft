package wily.legacy.mixin.base;

import net.minecraft.world.entity.ItemBasedSteering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemBasedSteering.class)
public interface ItemBasedSteeringAccessor {
    @Accessor("boosting")
    boolean getBoosting();
}

