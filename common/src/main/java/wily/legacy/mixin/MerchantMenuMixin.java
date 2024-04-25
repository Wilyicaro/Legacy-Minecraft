package wily.legacy.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.client.Offset;
import wily.legacy.inventory.LegacySlotWrapper;
import wily.legacy.inventory.RecipeMenu;

import java.util.List;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin extends AbstractContainerMenu implements RecipeMenu {
    @Shadow @Final private Merchant trader;

    @Shadow public abstract MerchantOffers getOffers();

    @Shadow public abstract int getTraderXp();

    @Shadow @Final private MerchantContainer tradeContainer;

    protected MerchantMenuMixin(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addFirstSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container, originalSlot.getContainerSlot(), 17, 114){
            public int getWidth() {
                return 27;
            }
            public void setChanged() {
                super.setChanged();
                MerchantMenuMixin.super.slotsChanged(container);
            }
            private final Offset MID_OFFSET = new Offset(0,16,0);

            @Override
            public Offset getOffset() {
                return slots.get(1).hasItem() || tradeContainer.getActiveOffer() != null && !tradeContainer.getActiveOffer().getCostB().isEmpty() ? Offset.ZERO : MID_OFFSET;
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSecondSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(),  17, 144){
            @Override
            public boolean isActive() {
                return hasItem() || tradeContainer.getActiveOffer() != null && !tradeContainer.getActiveOffer().getCostB().isEmpty();
            }

            public int getWidth() {
                return 27;
            }
        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addThirdSlot(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot,originalSlot.container, originalSlot.getContainerSlot(),  86, 130){

            public int getWidth() {
                return 27;
            }

        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 130 + (originalSlot.getContainerSlot() - 9) % 9 * 16,98 + (originalSlot.getContainerSlot() - 9) / 9 * 16){

            public int getWidth() {
                return 16;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                MerchantMenuMixin.super.slotsChanged(container);
            }

        };
    }
    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/MerchantMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 130 + originalSlot.getContainerSlot() * 16,154){

            public int getWidth() {
                return 16;
            }
            @Override
            public void setChanged() {
                super.setChanged();
                MerchantMenuMixin.super.slotsChanged(container);
            }

        };
    }

    @Override
    public void onCraft(Player player, int buttonInfo, List<Ingredient> ingredients, ItemStack result) {
        if (player instanceof ServerPlayer && buttonInfo >= 0 && buttonInfo < getOffers().size() && !getOffers().isEmpty()){
            MerchantOffer offer = getOffers().get(buttonInfo);
            offer.getResult().onCraftedBy(player.level(),player,offer.getResult().getCount());
            if (trader instanceof LivingEntity e) e.playSound(trader.getNotifyTradeSound(), 1.0f, e.getVoicePitch());
            this.trader.notifyTrade(offer);
            player.awardStat(Stats.TRADED_WITH_VILLAGER);
            this.trader.overrideXp(this.trader.getVillagerXp() + offer.getXp());
            player.sendMerchantOffers(containerId, getOffers(), trader instanceof Villager v ? v.getVillagerData().getLevel() : 0, getTraderXp(), trader.showProgressBar(), trader.canRestock());
        }
    }
}
