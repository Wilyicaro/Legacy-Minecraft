package wily.legacy.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.LegacyMinecraft;
import wily.legacy.inventory.LegacySlotWrapper;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin extends AbstractContainerMenu {
    @Shadow @Final private Merchant trader;

    @Shadow public abstract MerchantOffers getOffers();

    @Shadow public abstract int getTraderXp();

    protected MerchantMenuMixin(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container, originalSlot.getContainerSlot(), 17, 114){
            public int getWidth() {
                return 27;
            }

            public int getHeight() {
                return 27;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(),  17, 144){

            public int getWidth() {
                return 27;
            }

            public int getHeight() {
                return 27;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addThirdSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(),  86, 130){

            public int getWidth() {
                return 27;
            }

            public int getHeight() {
                return 27;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 130 + (originalSlot.getContainerSlot() - 9) % 9 * 16,98 + (originalSlot.getContainerSlot() - 9) / 9 * 16){

            public int getWidth() {
                return 16;
            }

            public int getHeight() {
                return 16;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }

        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 130 + originalSlot.getContainerSlot() * 16,154){

            public int getWidth() {
                return 16;
            }

            public int getHeight() {
                return 16;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                slotsChanged(container);
            }

        };
    }

    @Override
    public boolean clickMenuButton(Player player, int i) {
        if (player instanceof ServerPlayer && i >= 0 && i < getOffers().size() && !getOffers().isEmpty()){
            MerchantOffer offer = getOffers().get(i);
            offer.getResult().onCraftedBy(player.level(),player,1);
            this.trader.notifyTrade(offer);
            player.awardStat(Stats.TRADED_WITH_VILLAGER);
            this.trader.overrideXp(this.trader.getVillagerXp() + offer.getXp());
            player.sendMerchantOffers(containerId, getOffers(), trader instanceof Villager v ? v.getVillagerData().getLevel() : 0, getTraderXp(), trader.showProgressBar(), trader.canRestock());
            return true;
        }
        return false;
    }

}
