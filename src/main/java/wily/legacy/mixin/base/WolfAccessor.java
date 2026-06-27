package wily.legacy.mixin.base;

import net.minecraft.world.item.DyeColor;
//? if <1.21.5 {
import net.minecraft.world.entity.animal.Wolf;
//?} else {
/*import net.minecraft.world.entity.animal.wolf.Wolf;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Wolf.class)
public interface WolfAccessor {
    @Invoker("setCollarColor")
    void callSetCollarColor(DyeColor color);
}
