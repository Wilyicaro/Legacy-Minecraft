package wily.legacy.mixin.base;

import net.minecraft.world.entity.animal./*? if <1.21.11 {*//**//*?} else {*/pig./*?}*/Pig;
import net.minecraft.world.entity.ItemBasedSteering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Pig.class)
public interface PigAccessor {
    @Accessor("steering")
    ItemBasedSteering getSteering();
}
