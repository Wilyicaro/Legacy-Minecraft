package wily.legacy.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.EnderChestBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.inventory.LegacyChestMenu;

import java.util.OptionalInt;

@Mixin(EnderChestBlock.class)
public abstract class EnderChestBlockMixin {
    @Redirect(method = "use", at = @At(value = "INVOKE",target = "Lnet/minecraft/world/entity/player/Player;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;"))
    protected OptionalInt createMenu(Player instance, MenuProvider arg) {
        return instance.openMenu(new SimpleMenuProvider((i, inventory, player) -> LegacyChestMenu.threeRows(i, inventory, player.getEnderChestInventory()), Component.translatable("container.enderchest")));
    }

}
