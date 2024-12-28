package wily.legacy.mixin.base;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.network.ClientEffectActivationPayload;

@Mixin(CriteriaTriggers.class)
public class CriteriaTriggersMixin {
    @Redirect(method = "<clinit>", at = @At(value = "NEW", target = /*? if >1.20.1 {*/"()Lnet/minecraft/advancements/critereon/PlayerTrigger;"/*?} else {*//*"(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/advancements/critereon/PlayerTrigger;"*//*?}*/, ordinal = 3))
    private static PlayerTrigger heroOfTheVillagerTrigger(/*? if <=1.20.1 {*//*ResourceLocation location*//*?}*/){
        return effectTrigger(/*? if <=1.20.1 {*//*location, *//*?}*/MobEffects.HERO_OF_THE_VILLAGE);
    }
    @Redirect(method = "<clinit>", at = @At(value = "NEW", target = /*? if >1.20.1 {*/"()Lnet/minecraft/advancements/critereon/PlayerTrigger;"/*?} else {*//*"(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/advancements/critereon/PlayerTrigger;"*//*?}*/, ordinal = 4))
    private static PlayerTrigger badOmenTrigger(/*? if <=1.20.1 {*//*ResourceLocation location*//*?}*/){
        return effectTrigger(/*? if <=1.20.1 {*//*location, *//*?}*/MobEffects./*? if <1.20.5 {*//*BAD_OMEN*//*?} else {*/RAID_OMEN/*?}*/);
    }
    @Unique
    private static PlayerTrigger effectTrigger(/*? if <=1.20.1 {*//*ResourceLocation location, *//*?}*//*? if <1.20.5 {*//*MobEffect*//*?} else {*/Holder<MobEffect>/*?}*/ mobEffect){
        return new PlayerTrigger(/*? if <=1.20.1 {*//*location*//*?}*/){
            @Override
            public void trigger(ServerPlayer serverPlayer) {
                super.trigger(serverPlayer);
                CommonNetwork.sendToPlayer(serverPlayer, new ClientEffectActivationPayload(mobEffect));
            }
        };
    }

}
