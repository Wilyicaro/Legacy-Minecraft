package wily.legacy.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.client.Offset;
import wily.legacy.inventory.LegacySlotWrapper;

import static wily.legacy.client.LegacySprites.BREWING_FUEL_SLOT_SPRITE;

@Mixin(BrewingStandMenu.class)
public class BrewingStandMenuMixin {

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container, originalSlot.getContainerSlot(), 60, 76){
            public Offset getOffset() {
                return new Offset(0,0.5,0);
            }
            public int getWidth() {return 27;}
            public boolean hasIconHolder() {
                return false;
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(), 94, 87){
            public Offset getOffset() {
                return new Offset(0.5,0,0);
            }

            public int getWidth() {
                return 27;
            }
            public boolean hasIconHolder() {
                return false;
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addThirdSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(), 129, 76){
            public Offset getOffset() {
                return new Offset(0,0.5,0);
            }

            public int getWidth() {
                return 27;
            }
            public boolean hasIconHolder() {
                return false;
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addFourthSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(), 94, 25){
            public Offset getOffset() {
                return new Offset(0.5,0.5,0);
            }
            public int getWidth() {
                return 27;
            }

            public boolean hasIconHolder() {
                return false;
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addFifthSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(), 19, 25){
            public Offset getOffset() {
                return new Offset(0,0.5,0);
            }
            public int getWidth() {
                return 27;
            }
            public ResourceLocation getIconSprite() {
                return getItem().isEmpty() ? BREWING_FUEL_SLOT_SPRITE : super.getIconSprite();
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 5))
    private Slot addInventorySlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 13 + (originalSlot.getContainerSlot() - 9) % 9 * 21,126 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/BrewingStandMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 6))
    private Slot addHotbarSlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 13 + originalSlot.getContainerSlot() * 21,195);
    }
}
