package wily.legacy.network;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import wily.legacy.Legacy4JClient;
import wily.legacy.Legacy4JPlatform;

import java.util.function.Supplier;

public record ClientEffectActivationPacket(Holder<MobEffect> effect) implements CommonNetwork.Packet {
    public ClientEffectActivationPacket(RegistryFriendlyByteBuf buf){
        this(MobEffect.STREAM_CODEC.decode(buf));
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        MobEffect.STREAM_CODEC.encode(buf,effect);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
        if (Legacy4JPlatform.isClient()) Legacy4JClient.displayEffectActivationAnimation(effect);
    }
}
