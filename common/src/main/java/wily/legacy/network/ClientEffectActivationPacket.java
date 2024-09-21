package wily.legacy.network;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import wily.legacy.Legacy4JClient;
import wily.legacy.Legacy4JPlatform;

import java.util.function.Supplier;

public record ClientEffectActivationPacket(Holder<MobEffect> effect) implements CommonNetwork.Packet {
    public static final StreamCodec<RegistryFriendlyByteBuf,Holder<MobEffect>> MOB_EFFECT_CODEC = ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT);
    public ClientEffectActivationPacket(RegistryFriendlyByteBuf buf){
        this(MOB_EFFECT_CODEC.decode(buf));
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        MOB_EFFECT_CODEC.encode(buf,effect);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
        if (Legacy4JPlatform.isClient()) Legacy4JClient.displayEffectActivationAnimation(effect);
    }
}
