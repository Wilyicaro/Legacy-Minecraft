package wily.legacy.mixin;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.Offset;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.inventory.LegacySlot;

@Mixin(Slot.class)
public abstract class SlotMixin implements LegacySlotDisplay, LegacySlot {
    @Mutable
    @Shadow @Final public int x;
    @Mutable
    @Shadow @Final public int y;

    @Shadow public abstract ItemStack getItem();

    @Shadow public abstract void setChanged();

    private LegacySlotDisplay legacySlot;
    private ItemStack lastItemStack = ItemStack.EMPTY;
    private long lastItemStackChange;
    @Override
    public LegacySlotDisplay getDisplay() {
        return legacySlot;
    }
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    public void isActive(CallbackInfoReturnable<Boolean> cir) {
        if (!isVisible()) cir.setReturnValue(false);
    }
    @Inject(method = "getItem", at = @At("RETURN"))
    public void getItem(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack s = cir.getReturnValue();
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu.slots.contains(this) && !ItemStack.matches(s,lastItemStack) && lastItemStackChange != Util.getMillis()){
            lastItemStackChange = Util.getMillis();
            lastItemStack = s.copy();
            setChanged();
        }
    }
    @Override
    public void setLegacySlot(LegacySlotDisplay slot) {
        legacySlot = slot;
    }
    @Override
    public void setX(int x) {
        this.x = x;
    }
    @Override
    public void setY(int y) {
        this.y = y;
    }
    public int getWidth(){
        return getDisplay() == null ? 18 : getDisplay().getWidth();
    }
    public int getHeight(){
        return getDisplay() == null ? 18 : getDisplay().getHeight();
    }

    public Offset getOffset(){
        return getDisplay() == null ? Offset.ZERO : getDisplay().getOffset();
    }
    public ResourceLocation getIconSprite(){
        return getDisplay() == null ? null : getDisplay().getIconSprite();
    }
    public IconHolderOverride getIconHolderOverride(){
        return getDisplay() == null ? IconHolderOverride.EMPTY : getDisplay().getIconHolderOverride();
    }
    public boolean isVisible(){
        return getDisplay() == null || getDisplay().isVisible();
    }
}
