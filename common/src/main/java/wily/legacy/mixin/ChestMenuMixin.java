package wily.legacy.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.inventory.LegacySlotDisplay;

@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin extends AbstractContainerMenu {

    protected ChestMenuMixin(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }
    @ModifyArg(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ChestMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addSlotFirst(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + originalSlot.getContainerSlot() % 9 * 21,26 + originalSlot.getContainerSlot() / 9 * 21);
    }
    @Redirect(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ChestMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addInventorySlots(ChestMenu instance, Slot originalSlot, MenuType<?> menuType, int i, Inventory inventory, Container container, int j){
        int k = (j - 3) * 21;
        return addSlot(LegacySlotDisplay.override(originalSlot, 14 + (originalSlot.getContainerSlot() - 9) % 9 * 21,107 + (originalSlot.getContainerSlot() - 9) / 9 * 21 + k));
    }
    @Redirect(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/ChestMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addHotbarSlots(ChestMenu instance, Slot originalSlot, MenuType<?> menuType, int i, Inventory inventory, Container container, int j){
        int k = (j - 3) * 21;
        return addSlot(LegacySlotDisplay.override(originalSlot, 14 + originalSlot.getContainerSlot() * 21,177 + k));
    }
}
