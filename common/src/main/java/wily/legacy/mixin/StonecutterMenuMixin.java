package wily.legacy.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.Offset;
import wily.legacy.inventory.LegacySlotWrapper;

@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin extends AbstractContainerMenu {
    @Shadow @Final private Slot resultSlot;

    private static final Offset INVENTORY_OFFSET = new Offset(0.5,0.5,0);
    private static final Offset CONTAINER_OFFSET = new Offset(0.5,0,0);

    protected StonecutterMenuMixin(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/StonecutterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container, originalSlot.getContainerSlot(), 38, 149){
            public Offset getOffset() {
                return CONTAINER_OFFSET;
            }
            public int getWidth() {
                return 36;
            }


            public int getHeight() {
                return 36;
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/StonecutterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(), 110, 149){
            public Offset getOffset() {
                return CONTAINER_OFFSET;
            }
            public int getWidth() {
                return 36;
            }

            public int getHeight() {
                return 36;
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/StonecutterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addInventorySlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 186 + (originalSlot.getContainerSlot() - 9) % 9 * 16,125 + (originalSlot.getContainerSlot() - 9) / 9 * 16){
            @Override
            public Offset getOffset() {
                return INVENTORY_OFFSET;
            }
            @Override
            public int getWidth() {
                return 16;
            }
            @Override
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
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/StonecutterMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addHotbarSlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 186 + originalSlot.getContainerSlot() * 16,181){
            @Override
            public Offset getOffset() {
                return INVENTORY_OFFSET;
            }

            @Override
            public int getWidth() {
                return 16;
            }
            @Override
            public int getHeight() {
                return 16;
            }
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }
        };
    }
    @Inject(method = "clickMenuButton",at = @At("RETURN"))
    private void clickMenuButton(Player player, int i, CallbackInfoReturnable<Boolean> cir){
        if (i < 0) resultSlot.onTake(player, BuiltInRegistries.ITEM.byId(-i).getDefaultInstance());
    }
    @Inject(method = "slotsChanged",at = @At("RETURN"))
    private void slotsChanged(Container container, CallbackInfo ci) {
        super.slotsChanged(container);
    }
}
