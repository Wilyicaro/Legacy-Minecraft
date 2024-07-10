package wily.legacy.mixin;

import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.inventory.LegacySlotDisplay;

@Mixin(GrindstoneMenu.class)
public class GrindstoneMenuMixin {

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/GrindstoneMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 41, 30, new LegacySlotDisplay(){
            public int getWidth() {
                return 30;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/GrindstoneMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,41, 65,new LegacySlotDisplay(){

            public int getWidth() {
                return 30;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/GrindstoneMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addThirdSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,138, 46,new LegacySlotDisplay(){

            public int getWidth() {
                return 30;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/GrindstoneMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 10 + (originalSlot.getContainerSlot() - 9) % 9 * 21,116 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/GrindstoneMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 10 + originalSlot.getContainerSlot() * 21,185);
    }
}
