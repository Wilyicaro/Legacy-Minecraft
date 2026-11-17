package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.world.MusicDiscHuntManager;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;
    @Unique
    private int musicDiscHuntBulkChanges;

    @Shadow
    public abstract int getContainerSize();

    @Shadow
    public abstract ItemStack getItem(int slot);

    @WrapMethod(method = "add(ILnet/minecraft/world/item/ItemStack;)Z")
    private boolean collectAddedDisc(int slot, ItemStack stack, Operation<Boolean> original) {
        if (!(player instanceof ServerPlayer) || !MusicDiscHuntManager.isTracked(stack)) return original.call(slot, stack);
        ItemStack disc = stack.copy();
        int count = countMatching(disc);
        boolean added = original.call(slot, stack);
        if (countMatching(disc) > count) MusicDiscHuntManager.collect(player, disc);
        return added;
    }

    @Inject(method = "setItem", at = @At("RETURN"))
    private void collectSetDisc(int slot, ItemStack stack, CallbackInfo ci) {
        if (musicDiscHuntBulkChanges == 0) MusicDiscHuntManager.collect(player, stack);
    }

    @Inject(method = {"load", "replaceWith"}, at = @At("HEAD"))
    private void beginBulkChange(CallbackInfo ci) {
        musicDiscHuntBulkChanges++;
    }

    @Inject(method = {"load", "replaceWith"}, at = @At("RETURN"))
    private void endBulkChange(CallbackInfo ci) {
        musicDiscHuntBulkChanges--;
    }

    @Unique
    private int countMatching(ItemStack target) {
        int count = 0;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, target)) count += stack.getCount();
        }
        return count;
    }
}
