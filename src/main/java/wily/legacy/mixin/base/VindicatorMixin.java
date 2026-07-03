package wily.legacy.mixin.base;

import net.minecraft.world.entity.monster./*? if >=26.1 {*//*illager.*//*?}*/Vindicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Vindicator.class)
public class VindicatorMixin {
    //? if <1.20.5 {
    @ModifyArg(method = "createAttributes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;add(Lnet/minecraft/world/entity/ai/attributes/Attribute;D)Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;", ordinal = 3), index = 1)
    private static double createAttributes(double damage) {
        return 2.0;
    }
    //?} else {
    /*@ModifyArg(method = "createAttributes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;add(Lnet/minecraft/core/Holder;D)Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;", ordinal = 3), index = 1)
    private static double createAttributes(double damage) {
        return 2.0;
    }
    *///?}
}
