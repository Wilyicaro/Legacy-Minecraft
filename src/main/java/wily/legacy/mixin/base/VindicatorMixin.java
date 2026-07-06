package wily.legacy.mixin.base;

import net.minecraft.world.entity.monster./*? if >=26.1 {*//*illager.*//*?}*/Vindicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Vindicator.class)
public class VindicatorMixin {
    //? if <1.20.5 {
    @ModifyConstant(method = "createAttributes", constant = @Constant(doubleValue = 5.0))
    private static double createAttributes(double damage) {
        return 2.0;
    }
}
