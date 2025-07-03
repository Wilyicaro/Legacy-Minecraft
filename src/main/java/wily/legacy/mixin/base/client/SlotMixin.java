package wily.legacy.mixin.base.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.inventory.LegacySlot;

@Mixin(Slot.class)
public abstract class SlotMixin implements LegacySlot {
    @Mutable
    @Shadow @Final public int x;
    @Mutable
    @Shadow @Final public int y;

    @Unique private int defaultX;

    @Unique private int defaultY;

    @Shadow public abstract ItemStack getItem();

    @Shadow public abstract void setChanged();

    private LegacySlotDisplay display = LegacySlotDisplay.VANILLA;
    private ItemStack lastItemStack = ItemStack.EMPTY;
    private long lastItemStackChange;

    @Override
    public LegacySlotDisplay getDisplay() {
        return display;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Container container, int i, int j, int k, CallbackInfo ci){
        defaultX = x;
        defaultY = y;
    }

    @Inject(method = "getItem", at = @At("RETURN"), cancellable = true)
    public void getItem(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack s = cir.getReturnValue();
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu.slots.contains(this) && !ItemStack.matches(s,lastItemStack) && lastItemStackChange != Util.getMillis()){
            lastItemStackChange = Util.getMillis();
            lastItemStack = s.copy();
            setChanged();
        }
        ItemStack override = LegacySlotDisplay.of((Slot) (Object) this).getItemOverride();
        if (override != null) cir.setReturnValue(override);
    }

    @Override
    public void setDisplay(LegacySlotDisplay slot) {
        display = slot;
        if (slot == LegacySlotDisplay.VANILLA){
            x = defaultX;
            y = defaultY;
        }
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
