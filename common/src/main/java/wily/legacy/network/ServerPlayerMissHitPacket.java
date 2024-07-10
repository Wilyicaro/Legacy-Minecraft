package wily.legacy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public class ServerPlayerMissHitPacket implements CommonNetwork.Packet {
    public ServerPlayerMissHitPacket(){
    }
    public ServerPlayerMissHitPacket(FriendlyByteBuf buf){
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
        if (!player.get().isSpectator()) player.get().level().playSound(null,player.get(),SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS,1.0f,1.0f);
    }
}
