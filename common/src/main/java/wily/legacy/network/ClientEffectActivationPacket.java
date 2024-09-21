package wily.legacy.network;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import wily.legacy.Legacy4JClient;
import wily.legacy.Legacy4JPlatform;

import java.util.function.Supplier;

public record ClientEffectActivationPacket(MobEffect effect) implements CommonNetwork.Packet {
    public ClientEffectActivationPacket(FriendlyByteBuf buf){
        this(BuiltInRegistries.MOB_EFFECT.get(buf.readResourceKey(Registries.MOB_EFFECT)));
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceKey(BuiltInRegistries.MOB_EFFECT.getResourceKey(effect).get());
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
        if (Legacy4JPlatform.isClient()) Legacy4JClient.displayEffectActivationAnimation(effect);
    }
}
