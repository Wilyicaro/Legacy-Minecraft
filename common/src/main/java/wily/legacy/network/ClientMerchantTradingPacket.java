package wily.legacy.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.Merchant;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record ClientMerchantTradingPacket(int entityID, Optional<UUID> player) implements CommonNetwork.Packet {

    public static ClientMerchantTradingPacket of(AbstractVillager villager){
        return new ClientMerchantTradingPacket(villager.getId(), Optional.ofNullable(villager.getTradingPlayer()).map(Player::getUUID));
    }
    public static void sync(AbstractVillager entity){
        if (entity.level() instanceof ServerLevel l) {
            ClientMerchantTradingPacket packet = null;
            for (ServerPlayer player : l.getServer().getPlayerList().getPlayers()) {
                if (player.level() == entity.level() && player.distanceTo(entity) < l.getServer().getPlayerList().getViewDistance() * 16) CommonNetwork.sendToPlayer(player,packet == null ? (packet = ClientMerchantTradingPacket.of(entity)) : packet);
            }
        }
    }
    public ClientMerchantTradingPacket(RegistryFriendlyByteBuf buf){
        this(buf.readVarInt(),buf.readOptional(b->b.readUUID()));
    }
    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityID());
        buf.writeOptional(player,(b,u)-> b.writeUUID(u));
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (!(p.get().level().getEntity(entityID) instanceof AbstractVillager v)) return;
        if (p.get().level().isClientSide()) v.setTradingPlayer(player.map(u-> v.level().getPlayerByUUID(u)).orElse(null));
        else sync(v);
    }
}
