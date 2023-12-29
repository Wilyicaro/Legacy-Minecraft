package wily.legacy.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.inventory.LegacyChestMenu;

@Mixin(ChestBoat.class)
public abstract class ChestBoatMixin {
    @Shadow private ResourceLocation lootTable;

    @Shadow public abstract void unpackLootTable(Player arg);

    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    protected void createMenu(int i, Inventory inventory, Player player, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (this.lootTable== null || !player.isSpectator()) {
            this.unpackLootTable(inventory.player);
            cir.setReturnValue(LegacyChestMenu.threeRows(i, inventory, (Container) this));
        }
    }
}
