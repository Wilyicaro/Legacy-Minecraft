package wily.legacy.mixin;

import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.inventory.LegacySlotDisplay;

@Mixin(ItemCombinerMenu.class)
public class ItemCombinerMenuMixin {

    @ModifyArg(method = "createInputSlots",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;"))
    private Slot createInputSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,new LegacySlotDisplay(){
            public int getWidth() {
                return originalSlot.container.getContainerSize() > 2 ? LegacySlotDisplay.super.getWidth() : 30;
            }
        });
    }
    @ModifyArg(method = "createResultSlot",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;"))
    private Slot addSecondSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, new LegacySlotDisplay(){
            public int getWidth() {
                return 30;
            }
        });
    }

    @ModifyArg(method = "createInventorySlots",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 10 + (originalSlot.getContainerSlot() - 9) % 9 * 21,116 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "createInventorySlots",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ItemCombinerMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,10 + originalSlot.getContainerSlot() * 21,185);
    }
}
