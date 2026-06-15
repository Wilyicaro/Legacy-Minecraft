package wily.legacy.network;

import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.entity.LegacyShieldPlayer;

public class ServerPlayerShieldPausePayload extends CommonNetwork.EmptyPayload {
    public static final CommonNetwork.Identifier<ServerPlayerShieldPausePayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("server_player_shield_pause"), ServerPlayerShieldPausePayload::new);

    public ServerPlayerShieldPausePayload() {
        super(ID);
    }

    @Override
    public void apply(Context context) {
        ((LegacyShieldPlayer) context.player()).pauseShield(LegacyShieldPlayer.SHIELD_PAUSE_TICKS);
    }
}
