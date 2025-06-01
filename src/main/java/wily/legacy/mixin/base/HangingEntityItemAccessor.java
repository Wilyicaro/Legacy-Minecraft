package wily.legacy.mixin.base;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.HangingEntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HangingEntityItem.class)
public interface HangingEntityItemAccessor {
    @Accessor
    EntityType<?> getType();
}
