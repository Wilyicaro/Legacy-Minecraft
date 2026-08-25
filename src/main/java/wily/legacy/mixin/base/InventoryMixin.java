package wily.legacy.mixin.base;

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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.world.MusicDiscHuntManager;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;
    @Unique
    private int musicDiscHuntBulkChanges;
    @Unique
    private ItemStack musicDiscHuntAddedDisc = ItemStack.EMPTY;
    @Unique
    private int musicDiscHuntAddedDiscCount;

    @Shadow
    public abstract int getContainerSize();

    @Shadow
    public abstract ItemStack getItem(int slot);

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void prepareAddedDisc(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        musicDiscHuntAddedDisc = player instanceof ServerPlayer && MusicDiscHuntManager.isTracked(stack) ? stack.copy() : ItemStack.EMPTY;
        musicDiscHuntAddedDiscCount = musicDiscHuntAddedDisc.isEmpty() ? 0 : countMatching(musicDiscHuntAddedDisc);
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private void collectAddedDisc(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ItemStack disc = musicDiscHuntAddedDisc;
        musicDiscHuntAddedDisc = ItemStack.EMPTY;
        if (!disc.isEmpty() && countMatching(disc) > musicDiscHuntAddedDiscCount) MusicDiscHuntManager.collect(player, disc);
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
            if (/*? if <1.20.5 {*//*ItemStack.isSameItemSameTags(stack, target)*//*?} else {*/ItemStack.isSameItemSameComponents(stack, target)/*?}*/) count += stack.getCount();
        }
        return count;
    }
}
