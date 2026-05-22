package wily.legacy.mixin.base;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
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
    @Unique
    private boolean legacy$trackedPoiMemories;
    @Unique
    private boolean legacy$wasSleeping;
    @Unique
    private GlobalPos legacy$home;
    @Unique
    private GlobalPos legacy$jobSite;
    @Unique
    private GlobalPos legacy$meetingPoint;

    @Shadow
    public abstract VillagerData getVillagerData();

    @Shadow
    public abstract Brain<Villager> getBrain();

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

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void legacy$playPoiFeedbackSounds(ServerLevel level, CallbackInfo ci) {
        Brain<?> brain = getBrain();
        GlobalPos home = brain.getMemory(MemoryModuleType.HOME).orElse(null);
        GlobalPos jobSite = brain.getMemory(MemoryModuleType.JOB_SITE).orElse(null);
        GlobalPos meetingPoint = brain.getMemory(MemoryModuleType.MEETING_POINT).orElse(null);
        if (!legacy$trackedPoiMemories) {
            legacy$home = home;
            legacy$jobSite = jobSite;
            legacy$meetingPoint = meetingPoint;
            legacy$wasSleeping = legacy$isSleeping();
            legacy$trackedPoiMemories = true;
            return;
        }

        if (legacy$home != null && home == null || legacy$jobSite != null && jobSite == null || legacy$meetingPoint != null && meetingPoint == null) {
            legacy$playSound(SoundEvents.VILLAGER_NO, 1.0f, legacy$getVoicePitch());
        } else if (legacy$home == null && home != null && legacy$hasNearbyBell(level, home)) {
            legacy$playSound(legacy$getNotifyTradeSound(), 1.0f, legacy$getVoicePitch());
        }

        if (legacy$wasSleeping && !legacy$isSleeping() && brain.isActive(Activity.REST)) {
            legacy$playSound(SoundEvents.VILLAGER_NO, 1.0f, legacy$getVoicePitch());
        }

        legacy$home = home;
        legacy$jobSite = jobSite;
        legacy$meetingPoint = meetingPoint;
        legacy$wasSleeping = legacy$isSleeping();
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
    private boolean legacy$isSleeping() {
        return ((Villager) (Object) this).isSleeping();
    }

    @Unique
    private void legacy$playSound(SoundEvent sound, float volume, float pitch) {
        ((Villager) (Object) this).playSound(sound, volume, pitch);
    }

    @Unique
    private float legacy$getVoicePitch() {
        return ((LivingEntity) (Object) this).getVoicePitch();
    }

    @Unique
    private SoundEvent legacy$getNotifyTradeSound() {
        return ((Villager) (Object) this).getNotifyTradeSound();
    }

    @Unique
    private boolean legacy$hasNearbyBell(ServerLevel level, GlobalPos home) {
        if (home.dimension() != level.dimension()) {
            return false;
        }
        return getBrain().hasMemoryValue(MemoryModuleType.MEETING_POINT) || level.getPoiManager().findClosest(poi -> poi.is(PoiTypes.MEETING), home.pos(), 48, PoiManager.Occupancy.ANY).isPresent();
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
