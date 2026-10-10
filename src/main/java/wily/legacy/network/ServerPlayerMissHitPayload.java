package wily.legacy.network;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

public class ServerPlayerMissHitPayload extends CommonNetwork.EmptyPayload {
    public ServerPlayerMissHitPayload() {
        super(ID);
    }

    public static final CommonNetwork.Identifier<ServerPlayerMissHitPayload> ID = CommonNetwork.Identifier.create(Legacy4J.identifier("server_player_miss_hit"), ServerPlayerMissHitPayload::new);

    @Override
    public void apply(Context context) {
        if (!context.player().isSpectator())
            context.player().level().playSound(null, context.player(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 1.0f, 1.0f);
    }


}
