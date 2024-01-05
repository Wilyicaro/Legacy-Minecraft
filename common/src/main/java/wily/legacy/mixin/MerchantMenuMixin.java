package wily.legacy.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.inventory.LegacySlotWrapper;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin extends AbstractContainerMenu {
    protected MerchantMenuMixin(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container, originalSlot.getContainerSlot(), 17, 114){
            public int getWidth() {
                return 27;
            }

            public int getHeight() {
                return 27;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(),  17, 144){

            public int getWidth() {
                return 27;
            }

            public int getHeight() {
                return 27;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addThirdSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(),  86, 130){

            public int getWidth() {
                return 27;
            }

            public int getHeight() {
                return 27;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 130 + (originalSlot.getContainerSlot() - 9) % 9 * 16,98 + (originalSlot.getContainerSlot() - 9) / 9 * 16){

            public int getWidth() {
                return 16;
            }

            public int getHeight() {
                return 16;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }

        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 130 + originalSlot.getContainerSlot() * 16,154){

            public int getWidth() {
                return 16;
            }

            public int getHeight() {
                return 16;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }

        };
    }
}
