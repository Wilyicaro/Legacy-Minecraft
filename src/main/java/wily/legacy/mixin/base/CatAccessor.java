package wily.legacy.mixin.base;

import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Cat.class)
public interface CatAccessor {
    @Invoker("setCollarColor")
    void callSetCollarColor(DyeColor color);
}
