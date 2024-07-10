package wily.legacy.mixin;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.inventory.LegacySlotDisplay;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addSlotFirst(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 165, 44,new LegacySlotDisplay(){
            public int getWidth() {
                return 23;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSlotSecond(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 195, 44,new LegacySlotDisplay(){
            public int getWidth() {
                return 23;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addSlotThird(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 258, 39,new LegacySlotDisplay(){
            public int getWidth() {
                return 32;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 132 + (originalSlot.getContainerSlot() - 9) % 9 * 21,98 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 132 + originalSlot.getContainerSlot() * 21,166);
    }

}
