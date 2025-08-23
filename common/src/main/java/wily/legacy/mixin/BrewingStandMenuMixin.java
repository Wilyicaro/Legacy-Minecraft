package wily.legacy.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.util.Offset;
import wily.legacy.inventory.LegacySlotDisplay;

import static wily.legacy.util.LegacySprites.BREWING_FUEL_SLOT;

@Mixin(BrewingStandMenu.class)
public class BrewingStandMenuMixin {

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,60, 76,new LegacySlotDisplay(){
            public Offset getOffset() {
                return new Offset(0,0.5,0);
            }
            public int getWidth() {return 27;}
            public IconHolderOverride getIconHolderOverride() {
                return IconHolderOverride.EMPTY;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,94, 87,new LegacySlotDisplay(){
            public Offset getOffset() {
                return new Offset(0.5,0,0);
            }

            public int getWidth() {
                return 27;
            }
            public IconHolderOverride getIconHolderOverride() {
                return IconHolderOverride.EMPTY;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addThirdSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 129, 76,new LegacySlotDisplay(){
            public Offset getOffset() {
                return new Offset(0,0.5,0);
            }

            public int getWidth() {
                return 27;
            }
            public IconHolderOverride getIconHolderOverride() {
                return IconHolderOverride.EMPTY;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addFourthSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 94, 25,new LegacySlotDisplay(){
            public Offset getOffset() {
                return new Offset(0.5,0.5,0);
            }
            public int getWidth() {
                return 27;
            }

            public IconHolderOverride getIconHolderOverride() {
                return IconHolderOverride.EMPTY;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addFifthSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,19,25, new LegacySlotDisplay(){
            public Offset getOffset() {
                return new Offset(0,0.5,0);
            }
            public int getWidth() {
                return 27;
            }
            public ResourceLocation getIconSprite() {
                return originalSlot.getItem().isEmpty() ? BREWING_FUEL_SLOT : null;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 5))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 13 + (originalSlot.getContainerSlot() - 9) % 9 * 21,126 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 6))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 13 + originalSlot.getContainerSlot() * 21,195);
    }
}
