package wily.legacy.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.network.CommonNetwork;
import wily.legacy.network.ClientEffectActivationPacket;

@Mixin(CriteriaTriggers.class)
public class CriteriaTriggersMixin {
    @Redirect(method = "<clinit>", at = @At(value = "NEW", target = "()Lnet/minecraft/advancements/critereon/PlayerTrigger;", ordinal = 3))
    private static PlayerTrigger heroOfTheVillagerTrigger(){
        return effectTrigger(MobEffects.HERO_OF_THE_VILLAGE);
    }
    @Redirect(method = "<clinit>", at = @At(value = "NEW", target = "()Lnet/minecraft/advancements/critereon/PlayerTrigger;", ordinal = 4))
    private static PlayerTrigger badOmenTrigger(){
        return effectTrigger(MobEffects.BAD_OMEN);
    }
    @Unique
    private static PlayerTrigger effectTrigger(MobEffect mobEffect){
        return new PlayerTrigger(){
            @Override
            public void trigger(ServerPlayer serverPlayer) {
                super.trigger(serverPlayer);
                CommonNetwork.sendToPlayer(serverPlayer, new ClientEffectActivationPacket(mobEffect));
            }
        };
    }

}
