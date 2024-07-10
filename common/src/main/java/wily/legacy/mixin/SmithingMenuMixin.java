package wily.legacy.mixin;

import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SmithingMenu.class)
public class SmithingMenuMixin {
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withSlot(IIILjava/util/function/Predicate;)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;"), index = 2)
    private int setSlotsY(int i){
        return 60;
    }
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withSlot(IIILjava/util/function/Predicate;)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;", ordinal = 0), index = 1)
    private int setFirstSlotX(int i){
        return 10;
    }
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withSlot(IIILjava/util/function/Predicate;)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;", ordinal = 1), index = 1)
    private int setSecondSlotX(int i){
        return 31;
    }
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withSlot(IIILjava/util/function/Predicate;)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;", ordinal = 2), index = 1)
    private int setThirdSlotX(int i){
        return 52;
    }
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withResultSlot(III)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;"), index = 1)
    private int setResultSlotX(int i){
        return 127;
    }
    @ModifyArg(method = "createInputSlotDefinitions",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withResultSlot(III)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;"), index = 2)
    private int setResultSlotY(int i){
        return 56;
    }
}
