package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.util.LegacyItemUtil;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow @Final public Container container;
    @Shadow public abstract int getContainerSlot();

    @ModifyReturnValue(method = "mayPlace", at = @At("RETURN"))
    private boolean mayPlace(boolean original, ItemStack stack) {
        return original && (!(container instanceof Inventory inventory) || getContainerSlot() != 40 || !LegacyGameRules.getSidedBooleanGamerule(inventory.player, LegacyGameRules.LCE_OFFHAND_LIMITS) || LegacyItemUtil.canGoInLceOffhand(stack));
    }
}
