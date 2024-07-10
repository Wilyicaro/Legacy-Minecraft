package wily.legacy.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public record ClientEntityDataSyncPacket(int entityID, CompoundTag tag) implements CommonNetwork.Packet {

    public static ClientEntityDataSyncPacket of(LivingEntity entity){
        CompoundTag tag = new CompoundTag();
        entity.addAdditionalSaveData(tag);
        return new ClientEntityDataSyncPacket(entity.getId(),tag);
    }
    public static void syncEntity(LivingEntity entity){
        if (entity.level() instanceof ServerLevel l) {
            ClientEntityDataSyncPacket packet = null;
            for (ServerPlayer player : l.getServer().getPlayerList().getPlayers()) {
                if (player.level() == entity.level() && player.distanceTo(entity) < l.getServer().getPlayerList().getViewDistance() * 16) CommonNetwork.sendToPlayer(player,packet == null ? (packet = ClientEntityDataSyncPacket.of(entity)) : packet);
            }
        }
    }
    public ClientEntityDataSyncPacket(FriendlyByteBuf buf){
        this(buf.readVarInt(),buf.readNbt());
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityID());
        buf.writeNbt(tag());
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        executor.execute(()-> {
            if (p.get().level().getEntity(entityID) instanceof LivingEntity e) e.readAdditionalSaveData(tag);
        });
    }
}
