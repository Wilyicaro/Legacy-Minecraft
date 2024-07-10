package wily.legacy.mixin;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.inventory.LegacySlotDisplay;

@Mixin(StonecutterMenu.class)
public class StonecutterMenuMixin {
    
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/StonecutterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 31, 45,new LegacySlotDisplay(){
            public int getWidth() {
                return 23;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/StonecutterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 166, 41,new LegacySlotDisplay(){
            public int getWidth() {
                return 32;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/StonecutterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + (originalSlot.getContainerSlot() - 9) % 9 * 21,108 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/StonecutterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + originalSlot.getContainerSlot() * 21,178);
    }
}
