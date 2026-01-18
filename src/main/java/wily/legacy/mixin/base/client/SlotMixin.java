package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.inventory.LegacySlot;

@Mixin(Slot.class)
public abstract class SlotMixin implements LegacySlot {
    @Mutable
    @Shadow
    @Final
    public int x;
    @Mutable
    @Shadow
    @Final
    public int y;

    @Unique
    private int defaultX;

    @Unique
    private int defaultY;
    private LegacySlotDisplay display = LegacySlotDisplay.VANILLA;

    @Shadow
    public abstract ItemStack getItem();

    @Override
    public LegacySlotDisplay getDisplay() {
        return display;
    }

    @Override
    public void setDisplay(LegacySlotDisplay slot) {
        display = slot;
        if (slot == LegacySlotDisplay.VANILLA) {
            x = defaultX;
            y = defaultY;
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Container container, int i, int j, int k, CallbackInfo ci) {
        defaultX = x;
        defaultY = y;
    }

    @ModifyReturnValue(method = "getItem", at = @At("RETURN"))
    public ItemStack getItem(ItemStack original) {
        ItemStack override = display.getItemOverride();
        return override == null ? original : override;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }
}
