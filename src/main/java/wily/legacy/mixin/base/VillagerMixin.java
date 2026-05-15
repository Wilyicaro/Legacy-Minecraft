package wily.legacy.mixin.base;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.entity.LegacyVillager;
import wily.legacy.inventory.LegacyMerchantOffer;

@Mixin(Villager.class)
public abstract class VillagerMixin implements LegacyVillager {
    @Unique
    private int legacy$offersBeforeTradeUpdate;
    @Unique
    private int legacy$levelBeforeTradeUpdate;

    @Shadow
    public abstract VillagerData getVillagerData();

    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void legacy$reuseLockedTrades(ServerLevel level, CallbackInfo ci) {
        MerchantOffers offers = legacy$getOffers();
        legacy$offersBeforeTradeUpdate = offers.size();
        legacy$levelBeforeTradeUpdate = getVillagerData().level();
        if (legacy$hasOffersForLevel(offers, legacy$levelBeforeTradeUpdate)) {
            legacy$addLockedTrades(level, offers, legacy$levelBeforeTradeUpdate);
            ci.cancel();
        }
    }

    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void legacy$addLockedTrades(ServerLevel level, CallbackInfo ci) {
        MerchantOffers offers = legacy$getOffers();
        legacy$setOfferLevel(offers, legacy$offersBeforeTradeUpdate, legacy$levelBeforeTradeUpdate);
        legacy$addLockedTrades(level, offers, legacy$levelBeforeTradeUpdate);
    }

    @Override
    public void legacy$updateLockedTradePreviews(ServerLevel level) {
        MerchantOffers offers = legacy$getOffers();
        legacy$addLockedTrades(level, offers, getVillagerData().level());
    }

    @Unique
    private void legacy$addLockedTrades(ServerLevel level, MerchantOffers offers, int currentLevel) {
        if (!VillagerData.canLevelUp(currentLevel)) {
            return;
        }

        int nextLevel = currentLevel + 1;
        if (legacy$hasOffersForLevel(offers, nextLevel)) {
            return;
        }

        VillagerProfession profession = getVillagerData().profession().value();
        ResourceKey<TradeSet> trades = profession.getTrades(nextLevel);
        if (trades == null) {
            return;
        }

        int start = offers.size();
        ((AbstractVillagerAccessor) this).legacy$addOffersFromTradeSet(level, offers, trades);
        legacy$setOfferLevel(offers, start, nextLevel);
    }

    @Unique
    private MerchantOffers legacy$getOffers() {
        return ((Villager) (Object) this).getOffers();
    }

    @Unique
    private static void legacy$setOfferLevel(MerchantOffers offers, int start, int level) {
        for (int i = Math.max(0, start); i < offers.size(); i++) {
            ((LegacyMerchantOffer) offers.get(i)).setRequiredLevel(level);
        }
    }

    @Unique
    private static boolean legacy$hasOffersForLevel(MerchantOffers offers, int level) {
        for (MerchantOffer offer : offers) {
            if (((LegacyMerchantOffer) offer).getRequiredLevel() == level) {
                return true;
            }
        }
        return false;
    }
}
