package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.screen.CreativeModeScreen;
import wily.legacy.Legacy4JClient;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.entity.LegacyShieldPlayer;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.util.LegacyBlockProtection;
import wily.legacy.util.LegacyItemUtil;

@Mixin(MultiPlayerGameMode.class)
public class MultiplayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    //? if >=1.21.2 {
    @ModifyVariable(method = "handleCreativeModeItemDrop", at = @At("STORE"))
    public boolean handleCreativeModeItemDrop(boolean original) {
        return original && !(minecraft.screen instanceof CreativeModeScreen);
    }
    //?}

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void useItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerTrustPolicy trust = legacy$getTrust(player);
        if (trust != null && !trust.canUseItem(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS) && player.getItemInHand(hand).getItem() instanceof ShieldItem && (!((LegacyShieldPlayer) player).isAutoShielding() || ((LegacyShieldPlayer) player).isShieldPaused())) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @WrapOperation(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult useItemOnBlock(BlockState state, ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hitResult, Operation<InteractionResult> original) {
        PlayerTrustPolicy trust = legacy$getTrust(player);
        if (trust == null) return original.call(state, stack, level, player, hand, hitResult);
        if (!trust.canInteractWithBlock(state)) return InteractionResult.FAIL;
        return original.call(state, stack, level, player, hand, hitResult);
    }

    @WrapOperation(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;onItemUseFirst(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"), require = 0)
    private InteractionResult useItemFirst(ItemStack stack, UseOnContext context, Operation<InteractionResult> original) {
        Player player = context.getPlayer();
        PlayerTrustPolicy trust = player == null ? null : legacy$getTrust(player);
        if (trust == null) return original.call(stack, context);
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!trust.canInteractWithBlock(state)) return InteractionResult.FAIL;
        if (!trust.canBuildAndMine()) return InteractionResult.PASS;
        return original.call(stack, context);
    }

    @WrapOperation(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useWithoutItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult useBlock(BlockState state, Level level, Player player, BlockHitResult hitResult, Operation<InteractionResult> original) {
        PlayerTrustPolicy trust = legacy$getTrust(player);
        if (trust != null && !trust.canInteractWithBlock(state)) return InteractionResult.FAIL;
        return original.call(state, level, player, hitResult);
    }

    @WrapOperation(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult useItemOnBlock(ItemStack stack, UseOnContext context, Operation<InteractionResult> original) {
        Player player = context.getPlayer();
        PlayerTrustPolicy trust = player == null ? null : legacy$getTrust(player);
        if (trust != null && !trust.canBuildAndMine()) return InteractionResult.FAIL;
        return original.call(stack, context);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void interact(Player player, Entity target, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerTrustPolicy trust = legacy$getTrust(player);
        if (trust != null && !trust.canInteractWithEntity(target, player.isSecondaryUseActive(), player.getItemInHand(hand))) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void attack(Player player, Entity target, CallbackInfo ci) {
        PlayerTrustPolicy trust = legacy$getTrust(player);
        if (trust != null && !trust.canDamageByTrust(target)) ci.cancel();
    }

    @Inject(method = {"startDestroyBlock", "continueDestroyBlock"}, at = @At("HEAD"), cancellable = true)
    private void destroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        PlayerTrustPolicy trust = minecraft.player == null ? null : legacy$getTrust(minecraft.player);
        if (trust != null && !trust.canBuildAndMine()) cir.setReturnValue(false);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        PlayerTrustPolicy trust = minecraft.player == null ? null : legacy$getTrust(minecraft.player);
        if (trust != null && !trust.canBuildAndMine()) {
            cir.setReturnValue(false);
            return;
        }
        if (minecraft.player != null && minecraft.level != null && LegacyBlockProtection.blocksBreak(minecraft.level, pos, minecraft.level.getBlockState(pos), minecraft.player.getAbilities().instabuild)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private PlayerTrustPolicy legacy$getTrust(Player player) {
        if (!Legacy4JClient.hasModOnServer() || minecraft.getConnection() == null) return null;
        var info = minecraft.getConnection().getPlayerInfo(player.getUUID());
        return info == null ? null : PlayerTrustPolicy.of(player, LegacyPlayerInfo.of(info), Legacy4JClient.trustPlayers);
    }

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void handleContainerInput(int i, int j, int k, ContainerInput ContainerInput, Player player, CallbackInfo ci) {
        if (!LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_OFFHAND_LIMITS)) return;
        Slot slot = getSlot(player, j);
        if (slot == null) return;
        if (ContainerInput == ContainerInput.PICKUP && isOffhandSlot(slot) && !player.containerMenu.getCarried().isEmpty() && !LegacyItemUtil.canGoInLceOffhand(player.containerMenu.getCarried())) {
            ci.cancel();
            return;
        }
        if (ContainerInput == ContainerInput.SWAP && ((k == 40 && !LegacyItemUtil.canGoInLceOffhand(slot.getItem())) || (k >= 0 && k < 9 && isOffhandSlot(slot) && !LegacyItemUtil.canGoInLceOffhand(player.getInventory().getItem(k))))) {
            ci.cancel();
        }
    }

    private static Slot getSlot(Player player, int slotIndex) {
        return slotIndex < 0 || slotIndex >= player.containerMenu.slots.size() ? null : player.containerMenu.slots.get(slotIndex);
    }

    private static boolean isOffhandSlot(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() == 40;
    }
}
