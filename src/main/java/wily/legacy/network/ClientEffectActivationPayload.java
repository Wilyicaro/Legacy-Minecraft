package wily.legacy.network;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.Legacy4JPlatform;

import java.util.function.Supplier;

public record ClientEffectActivationPayload(/*? if <1.20.5 {*//*MobEffect*//*?} else {*/Holder<MobEffect>/*?}*/ effect) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<ClientEffectActivationPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("client_effect_activation"), ClientEffectActivationPayload::new);
    public ClientEffectActivationPayload(CommonNetwork.PlayBuf buf){
        this(/*? if <1.20.5 {*//*BuiltInRegistries.MOB_EFFECT.getHolder(buf.get().readVarInt()).get().value()*//*?} else {*/MobEffect.STREAM_CODEC.decode(buf.get())/*?}*/);
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        //? if <1.20.5 {
        /*buf.get().writeVarInt(BuiltInRegistries.MOB_EFFECT.getId(effect));
        *///?} else {
        MobEffect.STREAM_CODEC.encode(buf.get(),effect);
        //?}
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
        if (FactoryAPIPlatform.isClient()) Legacy4JClient.displayEffectActivationAnimation(effect);
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }
}
