package wily.legacy.mixin.base;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.inventory.RenameItemMenu;

@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenu implements RenameItemMenu {
    @Shadow public abstract void slotsChanged(Container container);

    @Shadow @Final private ResultContainer resultContainer;
    @Shadow @Final private ContainerLevelAccess access;
    private String itemName;

    protected CartographyTableMenuMixin(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    @Override
    public void setResultItemName(String name) {
        itemName = name;
        slotsChanged(resultContainer);
    }

    @Override
    public String getResultItemName() {
        return itemName;
    }
    @Inject(method = "slotsChanged", at = @At("HEAD"))
    private void slotsChanged(Container container, CallbackInfo ci) {
        super.slotsChanged(container);
    }
    @Redirect(method = "slotsChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 2))
    private boolean slotsChangedIsSecondSlotEmpty(ItemStack instance) {
        return false;
    }
    @Redirect(method = "slotsChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 4))
    private boolean slotsChangedIsSecondSlotEmpty2(ItemStack instance) {
        return false;
    }
    @Inject(method = "setupResultSlot", at = @At("RETURN"))
    private void setupResultSlot(ItemStack itemStack, ItemStack itemStack2, ItemStack itemStack3, CallbackInfo ci) {
        access.execute(((level, blockPos) -> {
            MapItemSavedData data = MapItem.getSavedData(itemStack,level);
            if (data == null || data.locked) return;
            ItemStack result = resultContainer.getItem(0);
            if (resultContainer.getItem(0).isEmpty() && itemStack.is(Items.FILLED_MAP) && itemStack2.isEmpty()) {
                result = itemStack.copyWithCount(1);
                if (RenameItemMenu.validateName(itemName) != null) RenameItemMenu.setCustomName(result,Component.literal(itemName));
                resultContainer.setItem(0, result);
                return;
            }
            if (RenameItemMenu.validateName(itemName) != null) {
                if (!itemStack3.getHoverName().getString().equals(itemName)) {
                    RenameItemMenu.setCustomName(result,Component.literal(itemName));
                    broadcastChanges();
                }
            } else if (RenameItemMenu.hasCustomName(result)){
                RenameItemMenu.setCustomName(result,null);
                broadcastChanges();
            }
        }));


    }
}
