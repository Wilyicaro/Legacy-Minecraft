package wily.legacy.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.inventory.LegacySlotDisplay;

import static wily.legacy.util.LegacySprites.*;

@Mixin(LoomMenu.class)
public class LoomMenuMixin {
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/LoomMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 19, 41,new LegacySlotDisplay(){
            public int getWidth() {
                return 23;
            }

            @Override
            public ResourceLocation getIconSprite() {
                return originalSlot.hasItem() ? null : BANNER_SLOT;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/LoomMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 45, 41,new LegacySlotDisplay(){
            public int getWidth() {
                return 23;
            }
            @Override
            public ResourceLocation getIconSprite() {
                return originalSlot.hasItem() ? null : DYE_SLOT;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/LoomMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addThirdSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 32, 66,new LegacySlotDisplay(){
            public int getWidth() {
                return 23;
            }
            @Override
            public ResourceLocation getIconSprite() {
                return originalSlot.hasItem() ? null : PATTERN_SLOT;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/LoomMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addFourthSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,166, 75,new LegacySlotDisplay(){
            public int getWidth() {
                return 32;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/LoomMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + (originalSlot.getContainerSlot() - 9) % 9 * 21,115 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/LoomMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 5))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + originalSlot.getContainerSlot() * 21,185);
    }
}
