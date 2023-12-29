package wily.legacy.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.inventory.LegacyChestMenu;

@Mixin(DispenserBlockEntity.class)
public class DispenserBlockEntityMixin {
    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    protected void createMenu(int i, Inventory inventory, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        cir.setReturnValue(LegacyChestMenu.threeRowsColumns(i, inventory, (Container) this));
    }
}
