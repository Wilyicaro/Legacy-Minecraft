package wily.legacy.mixin.base;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.util.ScreenUtil;

@Mixin(Inventory.class)
public class ClientInventoryMixin {
    @Unique
    private Inventory self(){
        return (Inventory) (Object)this;
    }
    @Inject(method = "setItem", at = @At("HEAD"))
    public void setItem(int i, ItemStack itemStack, CallbackInfo ci) {
        ItemStack actualItem = self().getItem(i);
        if (!ItemStack.matches(itemStack, actualItem) && !actualItem.isEmpty() && !itemStack.isEmpty() && Legacy4J.anyArmorSlotMatch(self(), item-> item.equals(actualItem))){
            ScreenUtil.updateAnimatedCharacterTime(1500);
        }
    }
}
