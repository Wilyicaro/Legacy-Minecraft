package wily.legacy.mixin.base;

import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.inventory.LegacyMerchantOffer;

@Mixin(MerchantContainer.class)
public class MerchantContainerMixin {
    @Shadow @Final private Merchant merchant;

    private int getActualMerchantLevel(){
        return merchant.isClientSide() ? (merchant.getTradingPlayer().containerMenu instanceof MerchantMenu menu ? menu.getTraderLevel() : 0) : merchant instanceof VillagerDataHolder data ? data.getVillagerData().getLevel() : 0;
    }
    @Redirect(method = "updateSellItem",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffers;getRecipeFor(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/trading/MerchantOffer;"))
    public MerchantOffer updateSellItem(MerchantOffers instance, ItemStack itemStack, ItemStack itemStack2, int i) {
        MerchantOffer offer = instance.getRecipeFor(itemStack,itemStack2,i);
        if (offer instanceof LegacyMerchantOffer o && getActualMerchantLevel() > 0 && o.getRequiredLevel() > getActualMerchantLevel())
            offer = null;
        return offer;
    }
}
