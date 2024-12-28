package wily.legacy.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record ClientMerchantTradingPayload(int entityID, Optional<UUID> player, CommonNetwork.Identifier<ClientMerchantTradingPayload> identifier) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<ClientMerchantTradingPayload> ID_C2S = CommonNetwork.Identifier.create(Legacy4J.createModLocation("client_merchant_trading_c2s"), b-> new ClientMerchantTradingPayload(b,ClientMerchantTradingPayload.ID_C2S));
    public static final CommonNetwork.Identifier<ClientMerchantTradingPayload> ID_S2C = CommonNetwork.Identifier.create(Legacy4J.createModLocation("client_merchant_trading_s2c"), b-> new ClientMerchantTradingPayload(b,ClientMerchantTradingPayload.ID_S2C));

    public static ClientMerchantTradingPayload of(AbstractVillager villager){
        return new ClientMerchantTradingPayload(villager.getId(), Optional.ofNullable(villager.getTradingPlayer()).map(Player::getUUID),ID_S2C);
    }
    public static void sync(AbstractVillager entity){
        if (entity.level() instanceof ServerLevel l) {
            ClientMerchantTradingPayload packet = null;
            for (ServerPlayer player : l.getServer().getPlayerList().getPlayers()) {
                if (player.level() == entity.level() && player.distanceTo(entity) < l.getServer().getPlayerList().getViewDistance() * 16) CommonNetwork.sendToPlayer(player,packet == null ? (packet = ClientMerchantTradingPayload.of(entity)) : packet);
            }
        }
    }
    public ClientMerchantTradingPayload(CommonNetwork.PlayBuf buf, CommonNetwork.Identifier<ClientMerchantTradingPayload> identifier){
        this(buf.get().readVarInt(),buf.get().readOptional(b->b.readUUID()),identifier);
    }
    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeVarInt(entityID());
        buf.get().writeOptional(player,(b,u)-> b.writeUUID(u));
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (!(p.get().level().getEntity(entityID) instanceof AbstractVillager v)) return;
        if (p.get().level().isClientSide()) v.setTradingPlayer(player.map(u-> v.level().getPlayerByUUID(u)).orElse(null));
        else sync(v);
    }
}
