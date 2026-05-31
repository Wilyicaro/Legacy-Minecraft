package wily.legacy.mixin.base;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.entity.LegacyVillager;
import wily.legacy.inventory.LegacyMerchantOffer;
import wily.legacy.util.LegacyItemUtil;
import wily.legacy.mobcaps.ConsoleMobCaps;

import java.util.ArrayList;
import java.util.Optional;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager implements LegacyVillager {
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

    public VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract VillagerData getVillagerData();

    @Redirect(method = "increaseMerchantCareer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;updateTrades()V"))
    private void increaseMerchantCareer(Villager instance) {
        if (getLevel() < 5) {
            updateTrades(getLevel() + 1);
        }
    }

    @Unique
    private int getLevel() {
        return getVillagerData()./*? if <1.21.5 {*//*getLevel*//*?} else {*/level/*?}*/();
    }

    public Villager self() {
        return (Villager) (Object) this;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
            this.updateTrades();
            legacy$updateLockedTradePreviews();
        }
        return this.offers;
    }

    @Override
    public void legacy$updateLockedTradePreviews(ServerLevel level) {
        legacy$updateLockedTradePreviews();
    }

    @Unique
    private void legacy$updateLockedTradePreviews() {
        int nextLevel = getLevel() + 1;
        if (getLevel() < 5 && !legacy$hasOffersForLevel(self().getOffers(), nextLevel)) {
            updateTrades(nextLevel);
        }
    }

    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    public void updateTrades(CallbackInfo ci) {
        ci.cancel();
        updateTrades(getLevel());
    }

    @Unique
    protected void updateTrades(int level) {
        Int2ObjectMap<VillagerTrades.ItemListing[]> int2ObjectMap;
        VillagerData villagerData = this.getVillagerData();
        var profession = villagerData./*? if <1.21.5 {*//*getProfession()*//*?} else {*/profession().unwrapKey().orElse(null)/*?}*/;
        Int2ObjectMap<VillagerTrades.ItemListing[]> int2ObjectMap2 = /*? if >=1.20.2 {*/this.level().enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE) ? ((int2ObjectMap = VillagerTrades.EXPERIMENTAL_TRADES.get(profession))) : /*?}*/ VillagerTrades.TRADES.get(profession);
        VillagerTrades.ItemListing[] itemListings;
        if (int2ObjectMap2 == null || int2ObjectMap2.isEmpty() || (itemListings = int2ObjectMap2.get(level)) == null)
            return;

        ArrayList<VillagerTrades.ItemListing> arrayList = Lists.newArrayList(itemListings);
        if (profession == VillagerProfession.FLETCHER && level == 5) {
            arrayList.add(this::getDecayArrowTrade);
        }
        int j = 0;
        while (j < 2 && !arrayList.isEmpty()) {
            MerchantOffer merchantOffer = arrayList.remove(self().getRandom().nextInt(arrayList.size())).getOffer(self(), self().getRandom());
            if (merchantOffer == null) continue;
            ((LegacyMerchantOffer) merchantOffer).setRequiredLevel(level);
            self().getOffers().add(merchantOffer);
            ++j;
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

    @Unique
    protected MerchantOffer getDecayArrowTrade(Entity trader, net.minecraft.util.RandomSource randomSource) {
        return new MerchantOffer(new ItemCost(Items.ARROW, 5), Optional.of(new ItemCost(Items.EMERALD, 2)), LegacyItemUtil.createDecayTippedArrow().copyWithCount(5), 12, 30, 0.2f);
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void customServerAiStep(ServerLevel level, CallbackInfo ci) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyAudio)) {
            legacy$trackedPoiMemories = false;
            return;
        }
        Brain<?> brain = this.getBrain();
        GlobalPos home = brain.getMemory(MemoryModuleType.HOME).orElse(null);
        GlobalPos jobSite = brain.getMemory(MemoryModuleType.JOB_SITE).orElse(null);
        GlobalPos meetingPoint = brain.getMemory(MemoryModuleType.MEETING_POINT).orElse(null);
        if (!legacy$trackedPoiMemories) {
            legacy$home = home;
            legacy$jobSite = jobSite;
            legacy$meetingPoint = meetingPoint;
            legacy$wasSleeping = isSleeping();
            legacy$trackedPoiMemories = true;
            return;
        }

        if (legacy$home != null && home == null || legacy$jobSite != null && jobSite == null || legacy$meetingPoint != null && meetingPoint == null) {
            playSound(SoundEvents.VILLAGER_NO, 1.0f, getVoicePitch());
        } else if (legacy$home == null && home != null && legacy$hasNearbyBell(level, home)) {
            playSound(getNotifyTradeSound(), 1.0f, getVoicePitch());
        }

        if (legacy$wasSleeping && !isSleeping() && brain.isActive(Activity.REST)) {
            playSound(SoundEvents.VILLAGER_NO, 1.0f, getVoicePitch());
        }

        legacy$home = home;
        legacy$jobSite = jobSite;
        legacy$meetingPoint = meetingPoint;
        legacy$wasSleeping = isSleeping();
    }

    @Unique
    private boolean legacy$hasNearbyBell(ServerLevel level, GlobalPos home) {
        if (home.dimension() != level.dimension()) {
            return false;
        }
        return getBrain().hasMemoryValue(MemoryModuleType.MEETING_POINT) || level.getPoiManager().findClosest(poi -> poi.is(PoiTypes.MEETING), home.pos(), 48, PoiManager.Occupancy.ANY).isPresent();
    }
  
    @Inject(method = "getBreedOffspring*", at = @At("HEAD"), cancellable = true)
    public void getBreedOffspring(ServerLevel level, AgeableMob partner, CallbackInfoReturnable<Villager> cir) {
        if (!ConsoleMobCaps.canVillagerBreed(level)) {
            cir.setReturnValue(null);
        }
    }
}
