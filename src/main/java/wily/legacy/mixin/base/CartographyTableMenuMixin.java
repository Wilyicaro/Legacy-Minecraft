package wily.legacy.mixin.base;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.util.FactoryItemUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.inventory.RenameItemMenu;

@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenu implements RenameItemMenu {
    @Shadow public abstract void slotsChanged(Container container);

    @Shadow @Final private ResultContainer resultContainer;
    @Shadow @Final private ContainerLevelAccess access;
    private String itemName;
    private ItemStack lastInput = ItemStack.EMPTY;

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
        if (container != resultContainer) {
            resetNameIfInputChanged();
        }
        super.slotsChanged(container);
    }
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void quickMoveStack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior)) {
            return;
        }
        if (index < 3 || index >= slots.size()) {
            return;
        }
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.getItem().is(Items.PAPER) || slots.get(0).hasItem()) {
            return;
        }
        ItemStack stack = source.getItem();
        ItemStack copy = stack.copy();
        if (!moveItemStackTo(stack, 0, 1, false)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) {
            source.setByPlayer(ItemStack.EMPTY);
        }
        source.setChanged();
        if (stack.getCount() == copy.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        source.onTake(player, stack);
        broadcastChanges();
        cir.setReturnValue(copy);
    }
    @Redirect(method = "slotsChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 2))
    private boolean slotsChangedIsSecondSlotEmpty(ItemStack instance) {
        return !FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior) && instance.isEmpty();
    }
    @Redirect(method = "slotsChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 4))
    private boolean slotsChangedIsSecondSlotEmpty2(ItemStack instance) {
        return !FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior) && instance.isEmpty();
    }
    @Inject(method = "setupResultSlot", at = @At("RETURN"))
    private void setupResultSlot(ItemStack itemStack, ItemStack itemStack2, ItemStack itemStack3, CallbackInfo ci) {
        access.execute(((level, blockPos) -> {
            if (itemStack.is(Items.PAPER)) {
                if (!canUsePaperConversion(level.isClientSide())) {
                    return;
                }
                ItemStack result = itemStack2.isEmpty() ? Items.MAP.getDefaultInstance() : ItemStack.EMPTY;
                applyResultName(result);
                if (!ItemStack.matches(result, itemStack3)) {
                    resultContainer.setItem(0, result);
                    broadcastChanges();
                }
                return;
            }
            MapItemSavedData data = MapItem.getSavedData(itemStack,level);
            if (data == null || data.locked) return;
            ItemStack result = resultContainer.getItem(0);
            if (resultContainer.getItem(0).isEmpty() && itemStack.is(Items.FILLED_MAP) && itemStack2.isEmpty()) {
                result = itemStack.copyWithCount(1);
                applyResultName(result);
                resultContainer.setItem(0, result);
                return;
            }
            if (RenameItemMenu.validateName(itemName) != null) {
                if (!RenameItemMenu.getItemName(itemStack3).equals(itemName)) {
                    FactoryItemUtil.setCustomName(result,Component.literal(itemName));
                    broadcastChanges();
                }
            } else if (FactoryItemUtil.hasCustomName(result)){
                FactoryItemUtil.setCustomName(result,null);
                broadcastChanges();
            }
        }));


    }

    private void applyResultName(ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        String name = RenameItemMenu.validateName(itemName);
        if (name != null) {
            FactoryItemUtil.setCustomName(result, Component.literal(name));
        } else if (FactoryItemUtil.hasCustomName(result)) {
            FactoryItemUtil.setCustomName(result, null);
        }
    }

    private boolean canUsePaperConversion(boolean clientSide) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyMapBehavior) && (!clientSide || Legacy4JClient.hasModOnServer());
    }

    private void resetNameIfInputChanged() {
        if (slots.isEmpty()) {
            return;
        }
        ItemStack input = slots.get(0).getItem();
        input = input.isEmpty() ? ItemStack.EMPTY : input.copyWithCount(1);
        if (ItemStack.matches(input, lastInput)) {
            return;
        }
        lastInput = input;
        itemName = null;
    }
}
