package wily.legacy.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.inventory.LegacyChestMenu;

@Mixin(MinecartHopper.class)
public abstract class MinecartHopperMixin {
    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    protected void createMenu(int i, Inventory inventory, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        cir.setReturnValue(LegacyChestMenu.fiveSlots(i, inventory, (Container) this));
    }
}
