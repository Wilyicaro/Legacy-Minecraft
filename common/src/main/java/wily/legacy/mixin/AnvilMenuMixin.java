package wily.legacy.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {

    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withSlot(IIILjava/util/function/Predicate;)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;", ordinal = 0), index = 1)
    private int setFirstSlotX(int i){
        return 15;
    }
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withSlot(IIILjava/util/function/Predicate;)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;"), index = 2)
    private int setSlotsY(int i){
        return 56;
    }
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withSlot(IIILjava/util/function/Predicate;)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;", ordinal = 1), index = 1)
    private int setSecondSlotX(int i){
        return 84;
    }
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withResultSlot(III)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;"), index = 1)
    private int setResultSlotX(int i){
        return 167;
    }
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withResultSlot(III)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;"), index = 2)
    private int setResultSlotY(int i){
        return 56;
    }
}
