package wily.legacy.mixin;

import net.minecraft.world.inventory.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.util.LegacySprites;
import wily.legacy.inventory.LegacySlotDisplay;

@Mixin(CrafterMenu.class)
public abstract class CrafterMenuMixin extends AbstractContainerMenu {
    protected CrafterMenuMixin(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    @Shadow public abstract boolean isSlotDisabled(int i);

    @ModifyArg(method = "addSlots",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/CrafterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addSlotFirst(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 150, 39,new LegacySlotDisplay(){
            public IconHolderOverride getIconHolderOverride() {
                return IconHolderOverride.create(LegacySprites.NON_INTERACTIVE_RESULT_SLOT);
            }
            public int getWidth() {return 32;}
        });
    }
    @ModifyArg(method = "addSlots",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/CrafterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addSlotSecond(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 34 + originalSlot.getContainerSlot() % 3 * 21,23 + originalSlot.getContainerSlot() / 3 * 21,new LegacySlotDisplay() {
            public IconHolderOverride getIconHolderOverride() {
                return IconHolderOverride.create(isSlotDisabled(originalSlot.index) ? LegacySprites.DISABLED_CRAFTER_SLOT : LegacySprites.CRAFTER_SLOT);
            }
        });
    }
    @ModifyArg(method = "addSlots",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/CrafterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + (originalSlot.getContainerSlot() - 9) % 9 * 21,102 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "addSlots",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/CrafterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + originalSlot.getContainerSlot() * 21,171);
    }
}
