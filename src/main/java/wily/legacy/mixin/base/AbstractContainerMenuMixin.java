package wily.legacy.mixin.base;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.init.LegacyGameRules;

import static wily.legacy.Legacy4J.canRepair;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Shadow @Final public NonNullList<Slot> slots;

    @Shadow public abstract ItemStack getCarried();

    @Shadow public abstract void setCarried(ItemStack itemStack);

    @Unique
    private static boolean isLegacyOffhandSlot(Slot slot, Player player) {
        return slot.container instanceof Inventory && slot.getContainerSlot() == 40 && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_OFFHAND_LIMITS);
    }

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void doClick(int i, int j, ClickType clickType, Player player, CallbackInfo ci) {
        Slot slot;
        if (i >= 0 && i < slots.size()) {
            slot = slots.get(i);
            if (clickType == ClickType.PICKUP && isLegacyOffhandSlot(slot, player) && !getCarried().isEmpty() && !Legacy4J.canGoInLceOffhand(getCarried())) {
                ci.cancel();
                return;
            }
            if (clickType == ClickType.SWAP && ((j == 40 && !Legacy4J.canGoInLceOffhand(slot.getItem()) && LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_OFFHAND_LIMITS)) || (j >= 0 && j < 9 && isLegacyOffhandSlot(slot, player) && !Legacy4J.canGoInLceOffhand(player.getInventory().getItem(j))))) {
                ci.cancel();
                return;
            }
        }
        if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && j == 1 && i >= 0 && i < slots.size() && (slot = slots.get(i)).hasItem() && !getCarried().isEmpty()){
            if (canRepair(slot.getItem(),getCarried())) {
                ItemStack item = slot.getItem().getItem().getDefaultInstance();
                item.setDamageValue(slot.getItem().getDamageValue() - (item.getMaxDamage() - getCarried().getDamageValue()));
                slot.set(item);
                if (!/*? if <1.20.5 {*//*player.getAbilities().instabuild*//*?} else {*/player.hasInfiniteMaterials()/*?}*/)
                    setCarried(ItemStack.EMPTY);
                ci.cancel();
            } else {
                DyeColor color = Legacy4J.getDyeColorOrNull(getCarried().getItem());
                if (Legacy4J.isDyeableItem(slot.getItem().getItemHolder()) && color != null) {
                    Legacy4J.dyeItem(slot.getItem(), Legacy4J.getDyeColor(color));
                    slot.setChanged();
                    if (!/*? if <1.20.5 {*//*player.getAbilities().instabuild*//*?} else {*/player.hasInfiniteMaterials()/*?}*/)
                        getCarried().shrink(1);
                    ci.cancel();
                }
            }
        }
    }
}
