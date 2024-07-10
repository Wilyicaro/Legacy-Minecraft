package wily.legacy.mixin;

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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.inventory.LegacySlotDisplay;
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

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/CartographyTableMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 10, 62,new LegacySlotDisplay(){
            public int getWidth() {
                return 23;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/CartographyTableMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,10, 105,new LegacySlotDisplay(){

            public int getWidth() {
                return 23;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/CartographyTableMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addThirdSlot(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 166, 82,new LegacySlotDisplay(){

            public int getWidth() {
                return 27;
            }
        });
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/CartographyTableMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 10 + (originalSlot.getContainerSlot() - 9) % 9 * 21,156 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/CartographyTableMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 10 + originalSlot.getContainerSlot() * 21,225);
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
                if (RenameItemMenu.validateName(itemName) != null) result.setHoverName(Component.literal(itemName));
                resultContainer.setItem(0, result);
                return;
            }
            if (RenameItemMenu.validateName(itemName) != null) {
                if (!itemStack3.getHoverName().getString().equals(itemName)) {
                    result.setHoverName(Component.literal(itemName));
                    broadcastChanges();
                }
            } else if (result.hasCustomHoverName()){
                result.resetHoverName();
                broadcastChanges();
            }
        }));


    }
}
