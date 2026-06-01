package wily.legacy.mixin.base;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.inventory.LegacyMerchantOffer;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager {
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

    @Shadow
    public abstract Brain<Villager> getBrain();

    @Redirect(method = "increaseMerchantCareer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;updateTrades(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void legacy$increaseMerchantCareer(Villager villager, ServerLevel level) {
        legacy$addTrades(level, legacy$getLevel() + 1);
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            if (!(level() instanceof ServerLevel level)) {
                throw new IllegalStateException("Cannot load Villager offers on the client");
            }
            offers = new MerchantOffers();
            updateTrades(level);
            legacy$addTrades(level, legacy$getLevel() + 1);
        }
        return offers;
    }

    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void legacy$updateTrades(ServerLevel level, CallbackInfo ci) {
        ci.cancel();
        legacy$addTrades(level, legacy$getLevel());
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void legacy$playPoiFeedbackSounds(ServerLevel level, CallbackInfo ci) {
        if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyAudio)) {
            legacy$trackedPoiMemories = false;
            return;
        }
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

    @Unique
    private void legacy$addTrades(ServerLevel level, int tradeLevel) {
        if (tradeLevel < 1 || tradeLevel > VillagerData.MAX_VILLAGER_LEVEL || legacy$hasOffersForLevel(tradeLevel)) {
            return;
        }

        VillagerProfession profession = getVillagerData().profession().value();
        ResourceKey<TradeSet> trades = profession.getTrades(tradeLevel);
        if (trades == null) {
            return;
        }

        int start = offers.size();
        addOffersFromTradeSet(level, offers, trades);
        legacy$setOfferLevel(start, tradeLevel);
    }

    @Unique
    private int legacy$getLevel() {
        return getVillagerData().level();
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
    private void legacy$setOfferLevel(int start, int level) {
        for (int i = Math.max(0, start); i < offers.size(); i++) {
            ((LegacyMerchantOffer) offers.get(i)).setRequiredLevel(level);
        }
    }

    @Unique
    private boolean legacy$hasOffersForLevel(int level) {
        for (MerchantOffer offer : offers) {
            if (((LegacyMerchantOffer) offer).getRequiredLevel() == level) {
                return true;
            }
        }
        return false;
    }
}
