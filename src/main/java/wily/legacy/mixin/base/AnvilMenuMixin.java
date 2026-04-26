package wily.legacy.mixin.base;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.inventory.RenameItemMenu;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    //? if neoforge {
    /*@Redirect(method = "createResultInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component createResultInternal(ItemStack itemStack) {
        return Component.literal(RenameItemMenu.getItemName(itemStack));
    }
    *///?} else {
    @Redirect(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component createResult(ItemStack itemStack) {
        return Component.literal(RenameItemMenu.getItemName(itemStack));
    }
    //?}
}
